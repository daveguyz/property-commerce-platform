package com.propertycommerce.webhookrouter.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * A URL registered by a tenant to receive outbound webhook events.
 * HMAC-SHA256 signed on every delivery using the stored signingSecret.
 * eventFilter is a comma-separated list; empty = receive all events.
 * Wildcard supported: "auction.*" matches "auction.lot.closed", etc.
 */
@Entity
@Table(name = "webhook_endpoints", indexes = {
    @Index(name = "idx_we_tenant", columnList = "tenant_id"),
    @Index(name = "idx_we_active", columnList = "active")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WebhookEndpoint {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String url;

    @Column(name = "signing_secret", nullable = false, length = 64)
    private String signingSecret;

    @Column(name = "event_filter", columnDefinition = "TEXT")
    private String eventFilter;

    @Builder.Default
    private boolean active = true;

    @Column(name = "failure_count")
    @Builder.Default
    private int failureCount = 0;

    @Column(name = "last_delivery_at")
    private LocalDateTime lastDeliveryAt;

    @Column(name = "disabled_at")
    private LocalDateTime disabledAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public boolean matchesEvent(String eventType) {
        if (eventFilter == null || eventFilter.isBlank()) return true;
        for (String f : eventFilter.split(",")) {
            String t = f.trim();
            if (t.equals(eventType)
                    || (t.endsWith(".*") && eventType.startsWith(t.replace(".*", ".")))) {
                return true;
            }
        }
        return false;
    }
}
