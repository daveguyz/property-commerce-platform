package com.staysphere.auctionservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_records", indexes = {
    @Index(name = "idx_kyc_user",    columnList = "user_id"),
    @Index(name = "idx_kyc_status",  columnList = "status"),
    @Index(name = "idx_kyc_session", columnList = "stripe_session_id")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class KycRecord {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String userEmail;

    // Stripe Identity verification session
    @Column(nullable = false)
    private String stripeSessionId;

    // Stripe Identity verification report (created after completion)
    private String stripeVerificationReportId;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    @Builder.Default
    private KycStatus status = KycStatus.NOT_STARTED;

    // URL the user visits to complete verification (expires)
    @Column(length = 1000)
    private String verificationUrl;

    // Document type submitted (passport, id_card, driving_license)
    private String documentType;

    // Stripe risk score (LOW, MEDIUM, HIGH)
    private String riskScore;

    // Failure reason from Stripe (if status = FAILED)
    @Column(columnDefinition = "TEXT")
    private String failureReason;

    // The lot this KYC was triggered for (optional — may be global for the user)
    private String triggeringLotId;

    // AI fraud assessment from AiFraudService
    @Column(precision = 5, scale = 4)
    private java.math.BigDecimal aiFraudScore;

    @Column(columnDefinition = "TEXT")
    private String aiFraudNotes;

    private LocalDateTime verifiedAt;
    private LocalDateTime expiresAt;

    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp  private LocalDateTime updatedAt;
}
