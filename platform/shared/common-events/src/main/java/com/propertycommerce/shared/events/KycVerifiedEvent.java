package com.propertycommerce.shared.events;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class KycVerifiedEvent {
    public static final String TOPIC = "kyc.verified";
    private String eventId;
    private String tenantId;   // Phase D: multi-tenancy scope
    private String userId;
    private String userEmail;
    private String stripeSessionId;
    private LocalDateTime occurredAt;
}
