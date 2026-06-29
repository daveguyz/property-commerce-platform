package com.staysphere.trustservice.listener;

import com.staysphere.shared.events.AgreementFullyExecutedEvent;
import com.staysphere.shared.events.PaymentDefaultedEvent;
import com.staysphere.trustservice.repository.UserProfileRepository;
import com.staysphere.trustservice.service.TrustScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adjusts trust scores on auction agreement lifecycle events.
 *
 * AgreementFullyExecutedEvent → +rep for buyer + seller (completed deal)
 * PaymentDefaultedEvent       → −rep for defaulting buyer (Rule 14.2)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AuctionEventListener {

    private final TrustScoreService trustScoreService;
    private final UserProfileRepository profileRepository;

    @KafkaListener(topics = AgreementFullyExecutedEvent.TOPIC,
                   groupId = "trust-service-group")
    @Transactional
    public void onAgreementExecuted(AgreementFullyExecutedEvent event) {
        try {
            applyCompletedTransaction(event.getWinnerId(), "buyer");
            applyCompletedTransaction(event.getSellerId(), "seller");
            log.info("[Trust] +rep for executed agreement {} lot {}",
                    event.getAgreementId(), event.getLotId());
        } catch (Exception e) {
            log.error("[Trust] Failed to update trust for agreement {}: {}",
                    event.getAgreementId(), e.getMessage());
        }
    }

    @KafkaListener(topics = PaymentDefaultedEvent.TOPIC,
                   groupId = "trust-service-group")
    @Transactional
    public void onPaymentDefaulted(PaymentDefaultedEvent event) {
        try {
            applyDefault(event.getDefaultingBidderId());
            log.warn("[Trust] −rep for payment default by {} on lot {}",
                    event.getDefaultingBidderId(), event.getLotId());
        } catch (Exception e) {
            log.error("[Trust] Failed to apply default penalty for {}: {}",
                    event.getDefaultingBidderId(), e.getMessage());
        }
    }

    private void applyCompletedTransaction(String userId, String role) {
        if (userId == null || userId.isBlank()) return;
        profileRepository.findByUserId(userId).ifPresent(profile -> {
            profile.setTotalBookings(
                (profile.getTotalBookings() != null ? profile.getTotalBookings() : 0) + 1);
            profileRepository.save(profile);
            trustScoreService.recalculateTrustScore(userId);
        });
    }

    private void applyDefault(String userId) {
        if (userId == null || userId.isBlank()) return;
        profileRepository.findByUserId(userId).ifPresent(profile -> {
            profile.setCancelledBookings(
                (profile.getCancelledBookings() != null ? profile.getCancelledBookings() : 0) + 1);
            profileRepository.save(profile);
            trustScoreService.recalculateTrustScore(userId);
        });
    }
}
