package com.propertycommerce.shared.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ReviewCreatedEvent {
    public static final String TOPIC = "review.created";
    private String eventId;
    private String reviewId;
    private String bookingId;
    private String propertyId;
    private String guestId;
    private String hostId;
    private Integer overallRating;
    private LocalDateTime occurredAt;
}
