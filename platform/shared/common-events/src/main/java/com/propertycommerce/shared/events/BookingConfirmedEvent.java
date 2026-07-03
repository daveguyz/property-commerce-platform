package com.propertycommerce.shared.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BookingConfirmedEvent {
    public static final String TOPIC = "booking.confirmed";
    private String eventId;
    private String tenantId;   // Phase D: multi-tenancy scope
    private String bookingId;
    private String propertyId;
    private String guestId;
    private String hostId;
    private String accessCode;
    private String paymentIntentId;
    private LocalDateTime occurredAt;
}
