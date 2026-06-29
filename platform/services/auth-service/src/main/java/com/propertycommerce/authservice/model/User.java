package com.propertycommerce.authservice.model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.*;
import java.time.*;
import java.util.*;
@Entity @Table(name = "users",
    indexes = { @Index(name = "idx_user_email", columnList = "email") })
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private String id;
    @Column(nullable = false, unique = true) private String email;
    @Column(nullable = false) private String passwordHash;
    private String firstName, lastName, phone, profileImageUrl, shopifyCustomerId;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    @Builder.Default private Set<String> roles = new HashSet<>(Set.of("GUEST"));
    @Enumerated(EnumType.STRING) @Builder.Default private AccountStatus status = AccountStatus.ACTIVE;
    @Builder.Default private Boolean emailVerified = false;
    @Builder.Default private Boolean phoneVerified = false;
    @Builder.Default private Boolean twoFactorEnabled = false;
    private String twoFactorSecret, refreshTokenHash, passwordResetToken, emailVerificationToken;
    private LocalDateTime passwordResetExpiry, lastLoginAt, lockoutUntil;
    private String lastLoginIp;
    @Builder.Default private Integer failedLoginAttempts = 0;
    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp private LocalDateTime updatedAt;
    public enum AccountStatus { ACTIVE, SUSPENDED, PENDING_VERIFICATION, DELETED }
}
