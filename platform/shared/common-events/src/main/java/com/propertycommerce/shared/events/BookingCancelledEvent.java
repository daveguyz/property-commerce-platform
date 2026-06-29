package com.propertycommerce.shared.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BookingCancelledEvent {
    public static final String TOPIC = "booking.cancelled";
    private String eventId;
    private String bookingId;
    private String propertyId;
    private String guestId;
    private String hostId;
    private String cancellationReason;
    private BigDecimal refundAmount;
    private String cancelledBy;
    private LocalDateTime occurredAt;
}
