package com.propertycommerce.notificationservice.model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity @Table(name = "notification_logs",
    indexes = @Index(name = "idx_notif_user", columnList = "user_id"))
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationLog {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private String id;
    @Column(nullable = false) private String userId;
    private String bookingId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private NotificationType type;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private NotificationChannel channel;
    private String recipient;
    @Column(columnDefinition = "TEXT") private String subject;
    @Column(columnDefinition = "TEXT") private String body;
    @Enumerated(EnumType.STRING) @Builder.Default private NotificationStatus status = NotificationStatus.PENDING;
    private String errorMessage;
    private Integer retryCount;
    @CreationTimestamp private LocalDateTime createdAt;
    private LocalDateTime sentAt;

    public enum NotificationType {
        BOOKING_CREATED, BOOKING_CONFIRMED, BOOKING_CANCELLED,
        CHECK_IN_REMINDER, CHECK_OUT_REMINDER, REVIEW_REQUEST,
        PAYMENT_CONFIRMED, PAYMENT_FAILED, NEGOTIATION_RECEIVED,
        NEGOTIATION_RESPONDED, ACCOUNT_VERIFIED, WELCOME,
        AUCTION_WON, AUCTION_OUTBID, AUCTION_LOT_OPENED, KYC_VERIFIED
    }
    public enum NotificationChannel { EMAIL, SMS, WHATSAPP, PUSH }
    public enum NotificationStatus { PENDING, SENT, FAILED, SKIPPED }
}
