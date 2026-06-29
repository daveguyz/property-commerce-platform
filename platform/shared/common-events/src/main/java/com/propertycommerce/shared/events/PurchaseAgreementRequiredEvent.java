package com.propertycommerce.shared.events;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PurchaseAgreementRequiredEvent {
    public static final String TOPIC = "auction.purchase-agreement-required";

    private String eventId;
    private String lotId;
    private String propertyId;
    private String lotTitle;
    private String winnerId;
    private String winnerEmail;
    private String sellerId;
    private String sellerEmail;
    private String auctioneerId;
    private BigDecimal winningAmount;
    private BigDecimal depositAmount;  // already captured
    private String currency;
    private LocalDateTime auctionClosedAt;
    private int paymentDeadlineDays;   // from settings (default 10)
}
