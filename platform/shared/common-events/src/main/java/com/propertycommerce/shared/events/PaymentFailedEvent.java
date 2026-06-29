package com.propertycommerce.shared.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentFailedEvent {
    public static final String TOPIC = "payment.failed";
    private String eventId;
    private String bookingId;
    private String paymentIntentId;
    private String failureReason;
    private LocalDateTime occurredAt;
}
