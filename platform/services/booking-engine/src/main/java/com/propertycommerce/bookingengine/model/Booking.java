package com.propertycommerce.bookingengine.model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "bookings", indexes = {
    @Index(name = "idx_booking_guest", columnList = "guest_id"),
    @Index(name = "idx_booking_property", columnList = "property_id"),
    @Index(name = "idx_booking_status", columnList = "status")})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Booking {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private String id;
    @Column(nullable = false) private String propertyId;
    @Column(nullable = false) private String guestId;
    @Column(nullable = false) private String hostId;
    private String shopifyOrderId;
    @Column(nullable = false) private LocalDate checkIn;
    @Column(nullable = false) private LocalDate checkOut;
    @Column(nullable = false) private Integer guestCount;
    private Integer childrenCount, infantCount, petCount;
    private BigDecimal baseAmount, cleaningFee, serviceFee, taxes, totalAmount, hostPayout, platformFee;
    @Column(length = 10, nullable = false) @Builder.Default private String currency = "NAD";
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    @Builder.Default private BookingStatus status = BookingStatus.PENDING_PAYMENT;
    private String paymentIntentId, accessCode;
    @Column(columnDefinition = "TEXT") private String specialRequests;
    @Column(columnDefinition = "TEXT") private String cancellationReason;
    private String cancelledBy;
    @CreationTimestamp private LocalDateTime createdAt;
    private LocalDateTime confirmedAt, cancelledAt, checkedInAt, checkedOutAt;
}
