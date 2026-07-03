package com.propertycommerce.shared.events;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentDefaultedEvent {
    public static final String TOPIC = "auction.payment-defaulted";

    private String eventId;
    private String tenantId;   // Phase D: multi-tenancy scope
    private String agreementId;
    private String lotId;
    private String propertyId;
    private String defaultingBidderId;
    private String defaultingBidderEmail;
    private String sellerId;
    private BigDecimal winningAmount;
    private BigDecimal forfeitedDeposit;
    private String currency;
    private LocalDateTime paymentDeadline;
    private LocalDateTime defaultedAt;
    private boolean nextBidderOffered;
}
