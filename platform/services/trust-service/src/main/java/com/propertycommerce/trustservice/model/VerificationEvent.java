package com.propertycommerce.trustservice.model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity @Table(name = "verification_events")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class VerificationEvent {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private String id;
    @Column(nullable = false) private String userId;
    @Column(nullable = false) private String eventType;
    @Column(nullable = false) private String status;
    private String provider, reference;
    @Column(columnDefinition = "TEXT") private String metadata;
    @CreationTimestamp private LocalDateTime createdAt;
}
