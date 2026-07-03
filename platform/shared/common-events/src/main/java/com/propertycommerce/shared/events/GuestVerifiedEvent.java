package com.propertycommerce.shared.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GuestVerifiedEvent {
    public static final String TOPIC = "guest.verified";
    private String eventId;
    private String tenantId;   // Phase D: multi-tenancy scope
    private String guestId;
    private String verificationType;
    private LocalDateTime occurredAt;
}
