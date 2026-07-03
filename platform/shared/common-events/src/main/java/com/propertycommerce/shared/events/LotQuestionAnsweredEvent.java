package com.propertycommerce.shared.events;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LotQuestionAnsweredEvent {
    public static final String TOPIC = "auction.question-answered";

    private String eventId;
    private String tenantId;   // Phase D: multi-tenancy scope
    private String questionId;
    private String lotId;
    private String bidderId;
    private String bidderEmail;
    private String responsePreview;   // first 120 chars
    private boolean answeredPublicly; // if true, no email sent (visible in room)
    private LocalDateTime respondedAt;
}
