package com.staysphere.bookingengine.service;

import com.staysphere.bookingengine.model.AgreementStatus;
import com.staysphere.bookingengine.model.PurchaseAgreement;
import com.staysphere.bookingengine.repository.PurchaseAgreementRepository;
import com.staysphere.shared.events.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Manages the full purchase agreement lifecycle after an auction closes.
 *
 * Flow:
 *   1. AuctionSettlementService fires PurchaseAgreementRequiredEvent
 *   2. This service generates the agreement and emails tokenised signing links
 *   3. Buyer signs → BUYER_SIGNED
 *   4. Seller signs → FULLY_EXECUTED → AgreementFullyExecutedEvent
 *   5. @Scheduled daily check: if payment_deadline passed → DEFAULTED → PaymentDefaultedEvent
 *
 * E-signature tokens:
 *   UUID4 generated per party, SHA-256 stored, plaintext emailed.
 *   Expire 72 hours after sending.
 *   The "signature" is recorded as SHA-256(signatureData + ISO-timestamp + userId).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PurchaseAgreementService {

    private final PurchaseAgreementRepository agreementRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${staysphere.frontend.url:https://staysphere-aos.myshopify.com}")
    private String frontendUrl;

    @Value("${staysphere.agreement.payment-deadline-days:10}")
    private int defaultPaymentDeadlineDays;

    // ═══════════════════════════════════════════════════════════
    // STEP 1 — Generate agreement from AuctionLotClosedEvent
    // ═══════════════════════════════════════════════════════════

    /**
     * Triggered by AuctionSettlementService publishing
     * PurchaseAgreementRequiredEvent after the lot settles with a winner.
     */
    @KafkaListener(topics = PurchaseAgreementRequiredEvent.TOPIC,
                   groupId = "booking-engine-group")
    @Transactional
    public void onPurchaseAgreementRequired(PurchaseAgreementRequiredEvent event) {
        log.info("[Agreement] Generating for lot {} winner={} amount={}",
                event.getLotId(), event.getWinnerId(), event.getWinningAmount());

        // Idempotency — if agreement already exists for this lot, skip
        if (agreementRepository.findByLotId(event.getLotId()).isPresent()) {
            log.warn("[Agreement] Agreement already exists for lot {} — skipping", event.getLotId());
            return;
        }

        BigDecimal balance = event.getWinningAmount()
                .subtract(event.getDepositAmount() != null
                        ? event.getDepositAmount() : BigDecimal.ZERO);

        int deadlineDays = event.getPaymentDeadlineDays() > 0
                ? event.getPaymentDeadlineDays() : defaultPaymentDeadlineDays;

        LocalDateTime paymentDeadline = event.getAuctionClosedAt()
                .plusDays(deadlineDays);

        // Generate signing tokens (plaintext returned, hash stored)
        String buyerToken  = UUID.randomUUID().toString();
        String sellerToken = UUID.randomUUID().toString();
        LocalDateTime tokenExpiry = LocalDateTime.now().plusHours(72);

        PurchaseAgreement agreement = PurchaseAgreement.builder()
                .lotId(event.getLotId())
                .propertyId(event.getPropertyId())
                .lotTitle(event.getLotTitle())
                .winnerId(event.getWinnerId())
                .winnerEmail(event.getWinnerEmail())
                .sellerId(event.getSellerId())
                .sellerEmail(event.getSellerEmail())
                .winningAmount(event.getWinningAmount())
                .depositAmount(event.getDepositAmount())
                .balanceDue(balance.max(BigDecimal.ZERO))
                .currency(event.getCurrency())
                .status(AgreementStatus.SENT)
                .buyerSigningTokenHash(sha256(buyerToken))
                .sellerSigningTokenHash(sha256(sellerToken))
                .signingTokensExpireAt(tokenExpiry)
                .paymentDeadline(paymentDeadline)
                .paymentDeadlineDays(deadlineDays)
                .build();

        PurchaseAgreement saved = agreementRepository.save(agreement);
        log.info("[Agreement] Created agreement {} for lot {} — deadline {}",
                saved.getId(), event.getLotId(), paymentDeadline);

        // Publish signing URLs via Kafka → notification-service sends emails
        publishSigningEmails(saved, buyerToken, sellerToken);
    }

    // ═══════════════════════════════════════════════════════════
    // STEP 3 — Record buyer signature
    // ═══════════════════════════════════════════════════════════

    /**
     * Called via POST /api/v1/agreements/sign/buyer?token={plaintextToken}
     *
     * @param plaintextToken  from the email link
     * @param signatureData   canvas PNG base64 or typed name string
     * @param signerId        the authenticated user ID (must match winnerId)
     */
    @Transactional
    public PurchaseAgreement recordBuyerSignature(String plaintextToken,
                                                   String signatureData,
                                                   String signerId) {
        PurchaseAgreement agreement = findByBuyerToken(plaintextToken);
        assertSignerMatch(agreement.getWinnerId(), signerId, "buyer");
        assertTokenNotExpired(agreement);
        assertStatus(agreement, AgreementStatus.SENT,
                "Agreement is not awaiting buyer signature (status=" + agreement.getStatus() + ")");

        agreement.setBuyerSignatureHash(sha256(signatureData + LocalDateTime.now() + signerId));
        agreement.setBuyerSignedAt(LocalDateTime.now());
        agreement.setStatus(AgreementStatus.BUYER_SIGNED);
        agreement.setBuyerSigningTokenHash(null); // invalidate token after use
        PurchaseAgreement saved = agreementRepository.save(agreement);

        log.info("[Agreement] Buyer signed agreement {} (lot {})",
                saved.getId(), saved.getLotId());
        return saved;
    }

    // ═══════════════════════════════════════════════════════════
    // STEP 4 — Record seller signature → FULLY_EXECUTED
    // ═══════════════════════════════════════════════════════════

    /**
     * Called via POST /api/v1/agreements/sign/seller?token={plaintextToken}
     */
    @Transactional
    public PurchaseAgreement recordSellerSignature(String plaintextToken,
                                                    String signatureData,
                                                    String signerId) {
        PurchaseAgreement agreement = findBySellerToken(plaintextToken);
        assertSignerMatch(agreement.getSellerId(), signerId, "seller");
        assertTokenNotExpired(agreement);

        if (agreement.getStatus() != AgreementStatus.BUYER_SIGNED
                && agreement.getStatus() != AgreementStatus.SENT) {
            throw new IllegalStateException(
                    "Seller cannot sign at this stage (status=" + agreement.getStatus() + ")");
        }

        agreement.setSellerSignatureHash(sha256(signatureData + LocalDateTime.now() + signerId));
        agreement.setSellerSignedAt(LocalDateTime.now());
        agreement.setStatus(AgreementStatus.FULLY_EXECUTED);
        agreement.setFullyExecutedAt(LocalDateTime.now());
        agreement.setSellerSigningTokenHash(null);
        PurchaseAgreement saved = agreementRepository.save(agreement);

        log.info("[Agreement] Fully executed — agreement {} lot {} winner={}",
                saved.getId(), saved.getLotId(), saved.getWinnerId());

        // Fire AgreementFullyExecutedEvent → notifications, trust-service, analytics
        kafkaTemplate.send(AgreementFullyExecutedEvent.TOPIC,
                AgreementFullyExecutedEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .agreementId(saved.getId())
                        .lotId(saved.getLotId())
                        .propertyId(saved.getPropertyId())
                        .winnerId(saved.getWinnerId())
                        .winnerEmail(saved.getWinnerEmail())
                        .sellerId(saved.getSellerId())
                        .sellerEmail(saved.getSellerEmail())
                        .winningAmount(saved.getWinningAmount())
                        .currency(saved.getCurrency())
                        .buyerSignedAt(saved.getBuyerSignedAt())
                        .sellerSignedAt(saved.getSellerSignedAt())
                        .fullyExecutedAt(saved.getFullyExecutedAt())
                        .build());

        // Initiate conveyancing (stub — wire to external conveyancer in production)
        initiateConveyancing(saved);
        return saved;
    }

    // ═══════════════════════════════════════════════════════════
    // STEP 5 — Payment confirmation (called by payment-service)
    // ═══════════════════════════════════════════════════════════

    /**
     * Called when payment-service confirms the balance was received.
     * Triggered via Kafka PaymentConfirmedEvent or direct REST call.
     */
    @Transactional
    public PurchaseAgreement confirmPayment(String lotId) {
        PurchaseAgreement agreement = findByLotId(lotId);
        agreement.setPaymentConfirmedAt(LocalDateTime.now());
        PurchaseAgreement saved = agreementRepository.save(agreement);
        log.info("[Agreement] Payment confirmed for agreement {} lot {}", saved.getId(), lotId);
        return saved;
    }

    // ═══════════════════════════════════════════════════════════
    // PAYMENT DEFAULT HANDLING
    // ═══════════════════════════════════════════════════════════

    /**
     * Daily scheduler — check for agreements past payment deadline.
     * FULLY_EXECUTED + deadline passed + no payment = DEFAULTED.
     */
    @Scheduled(cron = "0 0 6 * * *") // 06:00 daily
    @Transactional
    public void checkPaymentDeadlines() {
        List<PurchaseAgreement> overdue = agreementRepository
                .findOverdueAgreements(LocalDateTime.now());

        if (overdue.isEmpty()) return;
        log.info("[Agreement] Default check: {} overdue agreements found", overdue.size());

        overdue.forEach(this::processDefault);
    }

    @Transactional
    public PurchaseAgreement processDefault(PurchaseAgreement agreement) {
        agreement.setStatus(AgreementStatus.DEFAULTED);
        agreement.setDefaultedAt(LocalDateTime.now());
        PurchaseAgreement saved = agreementRepository.save(agreement);

        log.warn("[Agreement] DEFAULTED — agreement {} lot {} buyer={}",
                saved.getId(), saved.getLotId(), saved.getWinnerId());

        kafkaTemplate.send(PaymentDefaultedEvent.TOPIC,
                PaymentDefaultedEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .agreementId(saved.getId())
                        .lotId(saved.getLotId())
                        .propertyId(saved.getPropertyId())
                        .defaultingBidderId(saved.getWinnerId())
                        .defaultingBidderEmail(saved.getWinnerEmail())
                        .sellerId(saved.getSellerId())
                        .winningAmount(saved.getWinningAmount())
                        .forfeitedDeposit(saved.getDepositAmount())
                        .currency(saved.getCurrency())
                        .paymentDeadline(saved.getPaymentDeadline())
                        .defaultedAt(saved.getDefaultedAt())
                        .nextBidderOffered(false)
                        .build());

        return saved;
    }

    /**
     * Offer the lot to the next highest bidder after a default.
     * Called by the auctioneer from the dashboard Agreement tab.
     */
    @Transactional
    public PurchaseAgreement offerToNextBidder(String agreementId, String nextBidderId) {
        PurchaseAgreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new IllegalArgumentException("Agreement not found: " + agreementId));

        if (agreement.getStatus() != AgreementStatus.DEFAULTED) {
            throw new IllegalStateException(
                    "Can only offer to next bidder when status is DEFAULTED");
        }

        agreement.setNextBidderOffered(true);
        agreement.setNextBidderId(nextBidderId);
        PurchaseAgreement saved = agreementRepository.save(agreement);

        log.info("[Agreement] Lot {} offered to next bidder {} after default",
                saved.getLotId(), nextBidderId);

        // Re-emit PurchaseAgreementRequired for the next bidder
        // (auction-service should call back with their details)
        kafkaTemplate.send(PaymentDefaultedEvent.TOPIC,
                PaymentDefaultedEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .agreementId(saved.getId())
                        .lotId(saved.getLotId())
                        .propertyId(saved.getPropertyId())
                        .defaultingBidderId(saved.getWinnerId())
                        .defaultingBidderEmail(saved.getWinnerEmail())
                        .sellerId(saved.getSellerId())
                        .winningAmount(saved.getWinningAmount())
                        .forfeitedDeposit(saved.getDepositAmount())
                        .currency(saved.getCurrency())
                        .paymentDeadline(saved.getPaymentDeadline())
                        .defaultedAt(saved.getDefaultedAt())
                        .nextBidderOffered(true)
                        .build());

        return saved;
    }

    // ═══════════════════════════════════════════════════════════
    // READ / QUERY
    // ═══════════════════════════════════════════════════════════

    public PurchaseAgreement getByLotId(String lotId) {
        return findByLotId(lotId);
    }

    public List<PurchaseAgreement> getForBuyer(String winnerId) {
        return agreementRepository.findByWinnerIdOrderByCreatedAtDesc(winnerId);
    }

    public List<PurchaseAgreement> getForSeller(String sellerId) {
        return agreementRepository.findBySellerIdOrderByCreatedAtDesc(sellerId);
    }

    // ═══════════════════════════════════════════════════════════
    // CONVEYANCING STUB
    // ═══════════════════════════════════════════════════════════

    private void initiateConveyancing(PurchaseAgreement agreement) {
        // Stub for Phase 7. In production: call external conveyancer API
        // (e.g. WeConvey, Searchflow, or agent's own conveyancer).
        agreement.setConveyancingInitiatedAt(LocalDateTime.now());
        agreement.setConveyancerRef("CONV-" + agreement.getId().substring(0, 8).toUpperCase());
        agreementRepository.save(agreement);
        log.info("[Agreement] Conveyancing initiated for agreement {} lot {} ref={}",
                agreement.getId(), agreement.getLotId(), agreement.getConveyancerRef());
    }

    // ═══════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════

    private void publishSigningEmails(PurchaseAgreement agreement,
                                       String buyerToken, String sellerToken) {
        // Build signing URLs — /pages/purchase-agreement?token={plaintext}
        String buyerUrl  = frontendUrl + "/pages/purchase-agreement?role=buyer&token="  + buyerToken;
        String sellerUrl = frontendUrl + "/pages/purchase-agreement?role=seller&token=" + sellerToken;

        // Publish as an AgreementFullyExecutedEvent with status=SENT so
        // notification-service knows to send signing-request emails
        // (using a dedicated event topic avoids creating a new event type)
        kafkaTemplate.send("auction.agreement-signing-required",
                java.util.Map.of(
                        "agreementId",   agreement.getId(),
                        "lotId",         agreement.getLotId(),
                        "lotTitle",      agreement.getLotTitle() != null ? agreement.getLotTitle() : "",
                        "winnerId",      agreement.getWinnerId(),
                        "winnerEmail",   agreement.getWinnerEmail() != null ? agreement.getWinnerEmail() : "",
                        "sellerId",      agreement.getSellerId(),
                        "sellerEmail",   agreement.getSellerEmail() != null ? agreement.getSellerEmail() : "",
                        "winningAmount", agreement.getWinningAmount().toPlainString(),
                        "balanceDue",    agreement.getBalanceDue().toPlainString(),
                        "currency",      agreement.getCurrency(),
                        "buyerSignUrl",  buyerUrl,
                        "sellerSignUrl", sellerUrl,
                        "tokenExpiry",   agreement.getSigningTokensExpireAt().toString(),
                        "paymentDeadline", agreement.getPaymentDeadline().toString()
                ));
        log.info("[Agreement] Signing emails queued for agreement {}", agreement.getId());
    }

    private PurchaseAgreement findByLotId(String lotId) {
        return agreementRepository.findByLotId(lotId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No purchase agreement found for lot: " + lotId));
    }

    private PurchaseAgreement findByBuyerToken(String plaintextToken) {
        return agreementRepository.findByBuyerSigningTokenHash(sha256(plaintextToken))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid or expired buyer signing token"));
    }

    private PurchaseAgreement findBySellerToken(String plaintextToken) {
        return agreementRepository.findBySellerSigningTokenHash(sha256(plaintextToken))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid or expired seller signing token"));
    }

    private void assertSignerMatch(String expectedId, String actualId, String role) {
        if (!expectedId.equals(actualId)) {
            throw new SecurityException(
                    "Authenticated user " + actualId + " is not the " + role
                    + " for this agreement");
        }
    }

    private void assertTokenNotExpired(PurchaseAgreement agreement) {
        if (agreement.getSigningTokensExpireAt() != null
                && LocalDateTime.now().isAfter(agreement.getSigningTokensExpireAt())) {
            throw new IllegalStateException(
                    "Signing token has expired. Please request a new signing link.");
        }
    }

    private void assertStatus(PurchaseAgreement agreement,
                               AgreementStatus expected, String message) {
        if (agreement.getStatus() != expected) {
            throw new IllegalStateException(message);
        }
    }

    static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
