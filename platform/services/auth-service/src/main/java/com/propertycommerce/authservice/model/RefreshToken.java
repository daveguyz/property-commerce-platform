package com.propertycommerce.authservice.model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
@Entity @Table(name = "refresh_tokens")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RefreshToken {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private String id;
    @Column(nullable = false) private String userId;
    @Column(nullable = false, unique = true) private String tokenHash;
    @Column(nullable = false) private LocalDateTime expiresAt;
    private String deviceInfo, ipAddress;
    @Builder.Default private Boolean revoked = false;
    @CreationTimestamp private LocalDateTime createdAt;
}
