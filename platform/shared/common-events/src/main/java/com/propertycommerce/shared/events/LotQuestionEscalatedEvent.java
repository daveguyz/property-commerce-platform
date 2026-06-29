package com.propertycommerce.shared.events;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LotQuestionEscalatedEvent {
    public static final String TOPIC = "auction.question-escalated";

    private String eventId;
    private String questionId;
    private String lotId;
    private String bidderId;
    private String bidderEmail;
    private String reason;
    private String category;
    private LocalDateTime escalatedAt;
}
