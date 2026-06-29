package com.propertycommerce.bookingengine.model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity @Table(name = "trip_bookings")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TripBooking {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private String id;
    @Column(nullable = false) private String guestId;
    private String tripName;
    @Enumerated(EnumType.STRING) @Builder.Default private TripStatus status = TripStatus.DRAFT;
    private BigDecimal totalAmount;
    @Column(length = 10) @Builder.Default private String currency = "NAD";
    @ElementCollection
    @CollectionTable(name = "trip_booking_ids", joinColumns = @JoinColumn(name = "trip_id"))
    @Column(name = "booking_id") @Builder.Default private List<String> bookingIds = new ArrayList<>();
    @CreationTimestamp private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    public enum TripStatus { DRAFT, CONFIRMED, CANCELLED, PARTIAL }
}
