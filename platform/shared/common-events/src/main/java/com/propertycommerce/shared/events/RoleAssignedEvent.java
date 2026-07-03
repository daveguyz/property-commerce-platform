package com.propertycommerce.shared.events;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RoleAssignedEvent {
    public static final String TOPIC = "auth.role.assigned";

    private String eventId;
    private String tenantId;   // Phase D: multi-tenancy scope
    private String userId;
    private String email;
    private String role;          // e.g. "auctioneer", "host", "admin"
    private String action;        // "GRANTED" or "REVOKED"
    private String assignedBy;    // admin userId who made the change
    private String shopifyCustomerId; // present if user has linked Shopify account
    private LocalDateTime occurredAt;
}
