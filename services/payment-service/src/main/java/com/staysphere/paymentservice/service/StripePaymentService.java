package com.staysphere.paymentservice.service;
import com.stripe.*;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.net.Webhook;
import com.stripe.param.*;
import com.staysphere.paymentservice.model.*;
import com.staysphere.paymentservice.repository.*;
import com.staysphere.shared.events.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service @Slf4j @RequiredArgsConstructor
public class StripePaymentService {

    private final PaymentTransactionRepository transactionRepository;
    private final HostPayoutAccountRepository payoutAccountRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${stripe.secret-key}") private String stripeSecretKey;
    @Value("${stripe.webhook-secret}") private String webhookSecret;

    @Transactional
    public Map<String, String> createPaymentIntent(String bookingId, String guestId,
            String hostId, BigDecimal amount, BigDecimal hostPayout,
            BigDecimal platformFee, String currency) throws StripeException {

        Stripe.apiKey = stripeSecretKey;

        HostPayoutAccount hostAccount = payoutAccountRepository.findByHostId(hostId)
                .orElseThrow(() -> new RuntimeException("Host has no payout account"));

        long amountCents = amount.multiply(BigDecimal.valueOf(100)).longValueExact();
        long feeCents = platformFee.multiply(BigDecimal.valueOf(100)).longValueExact();

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountCents)
                .setCurrency(currency.toLowerCase())
                .setApplicationFeeAmount(feeCents)
                .setTransferData(PaymentIntentCreateParams.TransferData.builder()
                        .setDestination(hostAccount.getStripeAccountId()).build())
                .putMetadata("booking_id", bookingId)
                .putMetadata("guest_id", guestId)
                .putMetadata("host_id", hostId)
                .build();

        PaymentIntent intent = PaymentIntent.create(params);

        // Save transaction record
        PaymentTransaction tx = PaymentTransaction.builder()
                .bookingId(bookingId).guestId(guestId).hostId(hostId)
                .paymentIntentId(intent.getId()).hostStripeAccountId(hostAccount.getStripeAccountId())
                .amount(amount).hostPayout(hostPayout).platformFee(platformFee)
                .currency(currency).status(PaymentTransaction.TransactionStatus.PENDING).build();
        transactionRepository.save(tx);

        return Map.of("clientSecret", intent.getClientSecret(), "paymentIntentId", intent.getId());
    }

    @Transactional
    public void handleWebhookEvent(String payload, String sigHeader) {
        Stripe.apiKey = stripeSecretKey;
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new IllegalArgumentException("Invalid Stripe webhook signature");
        }

        switch (event.getType()) {
            case "payment_intent.succeeded" -> handlePaymentSucceeded(event);
            case "payment_intent.payment_failed" -> handlePaymentFailed(event);
            case "transfer.created" -> log.info("Transfer created: {}", event.getId());
            default -> log.debug("Unhandled Stripe event: {}", event.getType());
        }
    }

    private void handlePaymentSucceeded(Event event) {
        PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElseThrow();
        String bookingId = intent.getMetadata().get("booking_id");

        transactionRepository.findByPaymentIntentId(intent.getId()).ifPresent(tx -> {
            tx.setStatus(PaymentTransaction.TransactionStatus.SUCCEEDED);
            tx.setProcessedAt(LocalDateTime.now());
            transactionRepository.save(tx);
        });

        kafkaTemplate.send(PaymentConfirmedEvent.TOPIC, PaymentConfirmedEvent.builder()
                .eventId(UUID.randomUUID().toString()).bookingId(bookingId)
                .paymentIntentId(intent.getId())
                .amount(BigDecimal.valueOf(intent.getAmount()).divide(BigDecimal.valueOf(100)))
                .currency(intent.getCurrency().toUpperCase()).occurredAt(LocalDateTime.now()).build());

        log.info("Payment succeeded for booking {}", bookingId);
    }

    private void handlePaymentFailed(Event event) {
        PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElseThrow();
        String bookingId = intent.getMetadata().get("booking_id");

        transactionRepository.findByPaymentIntentId(intent.getId()).ifPresent(tx -> {
            tx.setStatus(PaymentTransaction.TransactionStatus.FAILED);
            tx.setFailureReason(intent.getLastPaymentError() != null
                    ? intent.getLastPaymentError().getMessage() : "Unknown");
            tx.setProcessedAt(LocalDateTime.now());
            transactionRepository.save(tx);
        });

        kafkaTemplate.send(PaymentFailedEvent.TOPIC, PaymentFailedEvent.builder()
                .eventId(UUID.randomUUID().toString()).bookingId(bookingId)
                .paymentIntentId(intent.getId())
                .failureReason(intent.getLastPaymentError() != null
                        ? intent.getLastPaymentError().getMessage() : "Unknown")
                .occurredAt(LocalDateTime.now()).build());

        log.warn("Payment failed for booking {}", bookingId);
    }

    @Transactional
    public Map<String, String> createHostConnectAccount(String hostId, String email, String country) throws StripeException {
        Stripe.apiKey = stripeSecretKey;

        AccountCreateParams params = AccountCreateParams.builder()
                .setType(AccountCreateParams.Type.EXPRESS)
                .setCountry(country).setEmail(email)
                .setCapabilities(AccountCreateParams.Capabilities.builder()
                        .setCardPayments(AccountCreateParams.Capabilities.CardPayments.builder().setRequested(true).build())
                        .setTransfers(AccountCreateParams.Capabilities.Transfers.builder().setRequested(true).build())
                        .build())
                .build();

        Account account = Account.create(params);

        HostPayoutAccount payoutAccount = HostPayoutAccount.builder()
                .hostId(hostId).stripeAccountId(account.getId())
                .stripeAccountType("express").country(country).currency("NAD").email(email)
                .detailsSubmitted(false).chargesEnabled(false).payoutsEnabled(false).build();
        payoutAccountRepository.save(payoutAccount);

        // Generate onboarding link
        AccountLinkCreateParams linkParams = AccountLinkCreateParams.builder()
                .setAccount(account.getId())
                .setRefreshUrl("https://staysphere.com/host/stripe/refresh")
                .setReturnUrl("https://staysphere.com/host/stripe/complete")
                .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                .build();
        AccountLink link = AccountLink.create(linkParams);

        return Map.of("accountId", account.getId(), "onboardingUrl", link.getUrl());
    }

    @Transactional
    public Map<String, Object> processRefund(String bookingId, BigDecimal refundAmount) throws StripeException {
        Stripe.apiKey = stripeSecretKey;
        PaymentTransaction tx = transactionRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("No payment found for booking " + bookingId));

        long refundCents = refundAmount.multiply(BigDecimal.valueOf(100)).longValueExact();
        RefundCreateParams params = RefundCreateParams.builder()
                .setPaymentIntent(tx.getPaymentIntentId())
                .setAmount(refundCents).build();
        Refund refund = Refund.create(params);

        tx.setStatus(PaymentTransaction.TransactionStatus.REFUNDED);
        transactionRepository.save(tx);

        return Map.of("refundId", refund.getId(), "status", refund.getStatus(), "amount", refundAmount);
    }
}
