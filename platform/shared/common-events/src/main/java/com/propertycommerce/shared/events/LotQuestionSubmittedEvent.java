package com.propertycommerce.shared.events;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LotQuestionSubmittedEvent {
    public static final String TOPIC = "auction.question-submitted";

    private String eventId;
    private String tenantId;   // Phase D: multi-tenancy scope
    private String questionId;
    private String lotId;
    private String bidderId;
    private String bidderDisplayName;  // "Bidder #4"
    private String auctioneerId;       // null if seller manages own lot
    private String sellerId;
    private String category;           // PROPERTY_INFO | BIDDING_RULES | TECHNICAL | GENERAL | COMPLAINT
    private String contentPreview;     // first 80 chars
    private LocalDateTime submittedAt;
}
