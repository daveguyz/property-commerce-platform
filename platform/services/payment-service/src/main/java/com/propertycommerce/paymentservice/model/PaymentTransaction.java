package com.propertycommerce.paymentservice.model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "payment_transactions",
    indexes = { @Index(name = "idx_payment_booking", columnList = "booking_id"),
                @Index(name = "idx_payment_intent", columnList = "payment_intent_id"),
                @Index(name = "idx_payment_host", columnList = "host_id") })
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentTransaction {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private String id;
    @Column(nullable = false) private String bookingId;
    @Column(nullable = false) private String guestId;
    @Column(nullable = false) private String hostId;
    private String paymentIntentId, chargeId, transferId;
    private String hostStripeAccountId;
    @Column(precision = 12, scale = 2) private BigDecimal amount;
    @Column(precision = 12, scale = 2) private BigDecimal hostPayout;
    @Column(precision = 12, scale = 2) private BigDecimal platformFee;
    @Column(length = 10) private String currency;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    @Builder.Default private TransactionStatus status = TransactionStatus.PENDING;
    private String failureReason;
    @Column(columnDefinition = "TEXT") private String stripeMetadata;
    @CreationTimestamp private LocalDateTime createdAt;
    private LocalDateTime processedAt;

    public enum TransactionStatus { PENDING, PROCESSING, SUCCEEDED, FAILED, REFUNDED, PARTIALLY_REFUNDED }
}
