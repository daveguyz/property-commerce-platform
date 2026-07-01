package com.propertycommerce.webhookrouter.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "webhook_deliveries", indexes = {
    @Index(name = "idx_wd_endpoint", columnList = "endpoint_id"),
    @Index(name = "idx_wd_event",    columnList = "event_type"),
    @Index(name = "idx_wd_status",   columnList = "status")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WebhookDelivery {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "endpoint_id", nullable = false)
    private String endpointId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "event_id")
    private String eventId;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DeliveryStatus status = DeliveryStatus.PENDING;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "attempt_count")
    @Builder.Default
    private int attemptCount = 0;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum DeliveryStatus { PENDING, DELIVERED, FAILED, ABANDONED }
}
