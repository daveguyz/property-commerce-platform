package com.propertycommerce.authservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * A long-lived API key issued to a tenant for server-to-server integrations.
 *
 * Unlike a JWT, an API key does not expire automatically — it lives until
 * explicitly revoked. The gateway accepts X-Api-Key as an alternative to
 * Authorization: Bearer for contexts where no end-user login exists
 * (e.g. a WordPress plugin making server-side API calls).
 *
 * Security: only the SHA-256 hash is stored (same pattern as BiddingCredential).
 * The plaintext key is returned once at issuance and never retrievable again.
 *
 * Key format: pcp_live_{32-char-hex} or pcp_test_{32-char-hex}
 * so consumers can easily distinguish live from test keys.
 */
@Entity
@Table(name = "tenant_api_keys", indexes = {
    @Index(name = "idx_tak_tenant", columnList = "tenant_id"),
    @Index(name = "idx_tak_hash",   columnList = "key_hash", unique = true)
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TenantApiKey {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /** Human-readable label set by the tenant at issuance */
    @Column(nullable = false, length = 100)
    private String label;

    /** SHA-256 hex of the plaintext key — never the key itself */
    @Column(name = "key_hash", nullable = false, length = 64, unique = true)
    private String keyHash;

    /** Prefix of the plaintext key stored for display ("pcp_live_ab12...") */
    @Column(name = "key_prefix", nullable = false, length = 20)
    private String keyPrefix;

    @Builder.Default
    private boolean active = true;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
