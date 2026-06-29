package com.propertycommerce.trustservice.service;
import com.propertycommerce.trustservice.model.UserProfile;
import com.propertycommerce.trustservice.repository.ReviewRepository;
import com.propertycommerce.trustservice.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service @Slf4j @RequiredArgsConstructor
public class TrustScoreService {
    private final UserProfileRepository profileRepository;
    private final ReviewRepository reviewRepository;

    @Transactional
    public double recalculateTrustScore(String userId) {
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        double score = calculateScore(profile);
        profile.setTrustScore(score);
        profileRepository.save(profile);
        log.info("Trust score for user {} recalculated to {}", userId, score);
        return score;
    }

    private double calculateScore(UserProfile profile) {
        double score = 0.0;

        // ===== ID & VERIFICATION (30 pts) =====
        if (Boolean.TRUE.equals(profile.getIdVerified())) score += 20.0;
        if (Boolean.TRUE.equals(profile.getPhoneVerified())) score += 5.0;
        if (Boolean.TRUE.equals(profile.getEmailVerified())) score += 5.0;

        // ===== BOOKING HISTORY (25 pts) =====
        int completedBookings = profile.getTotalBookings() != null ? profile.getTotalBookings() : 0;
        int cancelledBookings = profile.getCancelledBookings() != null ? profile.getCancelledBookings() : 0;
        // Up to 20 pts for completed bookings (2pts each, cap at 10)
        score += Math.min(completedBookings * 2.0, 20.0);
        // Penalty for cancellations
        score -= Math.min(cancelledBookings * 2.5, 15.0);

        // ===== REVIEW SCORE (25 pts) =====
        Double avgRating = reviewRepository.getAverageRatingForGuest(profile.getUserId());
        if (avgRating != null) {
            // 5-star = 25pts, 4-star = 20pts, 3-star = 12pts
            score += avgRating * 5.0;
        }

        // ===== RESPONSE RATE (10 pts) =====
        if (profile.getResponseRate() != null) {
            score += profile.getResponseRate() * 10.0;
        }

        // ===== ACCOUNT AGE (10 pts) =====
        if (profile.getCreatedAt() != null) {
            long daysOld = ChronoUnit.DAYS.between(profile.getCreatedAt().toLocalDate(), LocalDate.now());
            // Max 10pts at 1 year (365 days)
            score += Math.min(daysOld / 36.5, 10.0);
        }

        // Clamp 0-100
        return Math.max(0.0, Math.min(100.0, score));
    }

    public TrustScoreBreakdown getScoreBreakdown(String userId) {
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        return new TrustScoreBreakdown(
                profile.getTrustScore(),
                Boolean.TRUE.equals(profile.getIdVerified()),
                Boolean.TRUE.equals(profile.getPhoneVerified()),
                Boolean.TRUE.equals(profile.getEmailVerified()),
                profile.getTotalBookings(),
                profile.getCancelledBookings(),
                reviewRepository.getAverageRatingForGuest(userId),
                profile.getResponseRate(),
                getTrustLabel(profile.getTrustScore())
        );
    }

    private String getTrustLabel(Double score) {
        if (score == null) return "UNVERIFIED";
        if (score >= 85) return "HIGHLY_TRUSTED";
        if (score >= 70) return "TRUSTED";
        if (score >= 50) return "VERIFIED";
        if (score >= 30) return "BASIC";
        return "UNVERIFIED";
    }

    public record TrustScoreBreakdown(Double totalScore, Boolean idVerified, Boolean phoneVerified,
            Boolean emailVerified, Integer totalBookings, Integer cancelledBookings,
            Double averageRating, Double responseRate, String trustLabel) {}
}
