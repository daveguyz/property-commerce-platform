package com.propertycommerce.shared.events;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AgreementFullyExecutedEvent {
    public static final String TOPIC = "auction.agreement-executed";

    private String eventId;
    private String tenantId;   // Phase D: multi-tenancy scope
    private String agreementId;
    private String lotId;
    private String propertyId;
    private String winnerId;
    private String winnerEmail;
    private String sellerId;
    private String sellerEmail;
    private BigDecimal winningAmount;
    private String currency;
    private LocalDateTime buyerSignedAt;
    private LocalDateTime sellerSignedAt;
    private LocalDateTime fullyExecutedAt;
}
