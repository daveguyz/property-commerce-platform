package com.staysphere.bookingengine.model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.*;

@Entity @Table(name = "booking_negotiations")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BookingNegotiation {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private String id;
    private String propertyId, guestId, hostId;
    private LocalDate checkIn, checkOut;
    private Integer guestCount;
    private BigDecimal originalPrice, offeredPrice, counterPrice;
    @Enumerated(EnumType.STRING) private NegotiationStatus status;
    @Column(columnDefinition = "TEXT") private String guestMessage, hostResponse, aiSuggestion;
    @CreationTimestamp private LocalDateTime createdAt;
    private LocalDateTime respondedAt, expiresAt;
    public enum NegotiationStatus { PENDING, ACCEPTED, REJECTED, COUNTERED, EXPIRED }
}
