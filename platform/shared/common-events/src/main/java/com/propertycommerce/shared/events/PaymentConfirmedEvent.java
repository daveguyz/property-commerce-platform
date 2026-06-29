package com.propertycommerce.shared.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentConfirmedEvent {
    public static final String TOPIC = "payment.confirmed";
    private String eventId;
    private String bookingId;
    private String paymentIntentId;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime occurredAt;
}
