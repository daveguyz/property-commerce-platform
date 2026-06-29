package com.propertycommerce.auctionservice.service;

import com.propertycommerce.auctionservice.model.*;
import com.propertycommerce.auctionservice.repository.KycRecordRepository;
import com.propertycommerce.shared.events.KycVerifiedEvent;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.identity.VerificationSession;
import com.stripe.net.Webhook;
import com.stripe.param.identity.VerificationSessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service @Slf4j @RequiredArgsConstructor
public class KycService {

    private final KycRecordRepository kycRecordRepository;
    private final AiFraudService aiFraudService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${stripe.secret-key}")         private String stripeSecretKey;
    @Value("${stripe.kyc-webhook-secret:}") private String kycWebhookSecret;
    @Value("${pcp.frontend.url:https://propertycommerce.io}") private String frontendUrl;

    /**
     * Create a Stripe Identity verification session for a user.
     * Returns the URL the user must visit to submit their documents.
     */
    @Transactional
    public KycRecord createVerificationSession(String userId, String userEmail, String triggeringLotId) {
        // Check if user already has a valid verified record
        if (kycRecordRepository.existsByUserIdAndStatus(userId, KycStatus.VERIFIED)) {
            return kycRecordRepository.findByUserIdAndStatus(userId, KycStatus.VERIFIED).orElseThrow();
        }

        Stripe.apiKey = stripeSecretKey;

        try {
            VerificationSession session = VerificationSession.create(
                    VerificationSessionCreateParams.builder()
                            .setType(VerificationSessionCreateParams.Type.DOCUMENT)
                            .putMetadata("user_id", userId)
                            .putMetadata("user_email", userEmail)
                            .putMetadata("lot_id", triggeringLotId != null ? triggeringLotId : "")
                            .setReturnUrl(frontendUrl + "/pages/kyc?status=complete")
                            .setOptions(VerificationSessionCreateParams.Options.builder()
                                    .setDocument(VerificationSessionCreateParams.Options.Document.builder()
                                            .addAllowedType(VerificationSessionCreateParams.Options.Document.AllowedType.PASSPORT)
                                            .addAllowedType(VerificationSessionCreateParams.Options.Document.AllowedType.ID_CARD)
                                            .addAllowedType(VerificationSessionCreateParams.Options.Document.AllowedType.DRIVING_LICENSE)
                                            .setRequireMatchingSelfie(true)
                                            .build())
                                    .build())
                            .build()
            );

            KycRecord record = KycRecord.builder()
                    .userId(userId)
                    .userEmail(userEmail)
                    .stripeSessionId(session.getId())
                    .status(KycStatus.SESSION_CREATED)
                    .verificationUrl(session.getUrl())
                    .triggeringLotId(triggeringLotId)
                    .expiresAt(LocalDateTime.now().plusHours(48))
                    .build();

            KycRecord saved = kycRecordRepository.save(record);
            log.info("[KYC] Session created for user {} session={}", userId, session.getId());
            return saved;

        } catch (Exception e) {
            log.error("[KYC] Failed to create Stripe Identity session for {}: {}", userId, e.getMessage());
            throw new IllegalStateException("KYC session creation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Handle Stripe Identity webhook events.
     * Called by the webhook controller when Stripe POSTs to /api/v1/kyc/webhook.
     */
    @Transactional
    public void handleStripeWebhook(String payload, String sigHeader) {
        Stripe.apiKey = stripeSecretKey;

        com.stripe.model.Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, kycWebhookSecret);
        } catch (SignatureVerificationException e) {
            throw new IllegalArgumentException("Invalid Stripe Identity webhook signature");
        }

        log.info("[KYC] Webhook received: {}", event.getType());

        switch (event.getType()) {
            case "identity.verification_session.verified"      -> handleVerified(event);
            case "identity.verification_session.requires_input" -> handleRequiresInput(event);
            case "identity.verification_session.canceled"      -> handleCancelled(event);
            case "identity.verification_session.processing"    -> handleProcessing(event);
            default -> log.debug("[KYC] Unhandled event type: {}", event.getType());
        }
    }

    private void handleVerified(com.stripe.model.Event event) {
        VerificationSession session = (VerificationSession)
                event.getDataObjectDeserializer().getObject().orElseThrow();

        kycRecordRepository.findByStripeSessionId(session.getId()).ifPresent(record -> {
            record.setStatus(KycStatus.VERIFIED);
            record.setVerifiedAt(LocalDateTime.now());
            record.setStripeVerificationReportId(session.getLastVerificationReport());

            // Run AI fraud assessment on newly verified user
            try {
                AiFraudService.FraudAssessment assessment =
                        aiFraudService.assessKycVerification(record.getUserId(), session.getId());
                record.setAiFraudScore(java.math.BigDecimal.valueOf(assessment.score()));
                record.setAiFraudNotes(assessment.reasoning());
                if (assessment.score() > 0.7) {
                    record.setStatus(KycStatus.FAILED);
                    record.setFailureReason("AI fraud detection flagged this verification");
                    log.warn("[KYC] High fraud score {:.3f} for user {} — verification rejected",
                            assessment.score(), record.getUserId());
                }
            } catch (Exception e) {
                log.warn("[KYC] AI fraud assessment failed (non-blocking): {}", e.getMessage());
            }

            kycRecordRepository.save(record);

            if (record.getStatus() == KycStatus.VERIFIED) {
                kafkaTemplate.send(KycVerifiedEvent.TOPIC, KycVerifiedEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .userId(record.getUserId())
                        .userEmail(record.getUserEmail())
                        .stripeSessionId(session.getId())
                        .occurredAt(LocalDateTime.now())
                        .build());
                log.info("[KYC] User {} VERIFIED", record.getUserId());
            }
        });
    }

    private void handleRequiresInput(com.stripe.model.Event event) {
        VerificationSession session = (VerificationSession)
                event.getDataObjectDeserializer().getObject().orElseThrow();
        kycRecordRepository.findByStripeSessionId(session.getId()).ifPresent(record -> {
            record.setStatus(KycStatus.REQUIRES_INPUT);
            if (session.getLastError() != null)
                record.setFailureReason(session.getLastError().getReason());
            kycRecordRepository.save(record);
            log.info("[KYC] Session {} requires additional input", session.getId());
        });
    }

    private void handleCancelled(com.stripe.model.Event event) {
        VerificationSession session = (VerificationSession)
                event.getDataObjectDeserializer().getObject().orElseThrow();
        kycRecordRepository.findByStripeSessionId(session.getId()).ifPresent(record -> {
            record.setStatus(KycStatus.CANCELLED);
            kycRecordRepository.save(record);
        });
    }

    private void handleProcessing(com.stripe.model.Event event) {
        VerificationSession session = (VerificationSession)
                event.getDataObjectDeserializer().getObject().orElseThrow();
        kycRecordRepository.findByStripeSessionId(session.getId()).ifPresent(record -> {
            record.setStatus(KycStatus.PROCESSING);
            kycRecordRepository.save(record);
        });
    }

    /** Check if a user has passed KYC. */
    public boolean isUserVerified(String userId) {
        return kycRecordRepository.existsByUserIdAndStatus(userId, KycStatus.VERIFIED);
    }

    /** Get the most recent KYC record for a user. */
    public KycRecord getKycStatus(String userId) {
        return kycRecordRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
                .orElse(KycRecord.builder().userId(userId).status(KycStatus.NOT_STARTED).build());
    }

    /**
     * Auto-trigger KYC requirement if a bid exceeds the lot's threshold.
     * Called by BidEngineService before accepting a bid.
     */
    public void assertKycIfRequired(String lotId, String userId, java.math.BigDecimal bidAmount) {
        // Lot-level KYC
        if (kycRecordRepository.existsByUserIdAndStatus(userId, KycStatus.VERIFIED)) return;

        // We throw — the controller returns the KYC URL so the frontend can redirect
        throw new KycRequiredException("Identity verification required to bid on this lot. " +
                "Please complete KYC before placing your bid.", userId, lotId);
    }

    /** Typed exception so the controller can return 403 + KYC URL. */
    public static class KycRequiredException extends RuntimeException {
        private final String userId;
        private final String lotId;
        public KycRequiredException(String msg, String userId, String lotId) {
            super(msg);
            this.userId = userId;
            this.lotId = lotId;
        }
        public String getUserId() { return userId; }
        public String getLotId()  { return lotId; }
    }
}
