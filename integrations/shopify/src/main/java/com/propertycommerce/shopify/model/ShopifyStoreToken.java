package com.propertycommerce.shopify.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * Persists the Shopify OAuth access token for each installed store.
 * One record per shop domain.
 */
@Entity
@Table(name = "shopify_store_tokens",
       indexes = @Index(name = "idx_store_domain", columnList = "shop_domain", unique = true))
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ShopifyStoreToken {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /** e.g. propertycommerce.io */
    @Column(name = "shop_domain", nullable = false, unique = true)
    private String shopDomain;

    /** Admin API access token (shpat_...) */
    @Column(name = "access_token", nullable = false)
    private String accessToken;

    /** Shopify plan name (development / partner_test / shopify_plus ...) */
    private String planName;

    /** Store owner email — for sending install confirmation */
    private String ownerEmail;

    /** The theme ID we provisioned (so we can update it on re-install) */
    private Long provisionedThemeId;

    @Builder.Default
    private Boolean themeProvisioned = false;

    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp private LocalDateTime installedAt;
    @UpdateTimestamp  private LocalDateTime updatedAt;
    private LocalDateTime uninstalledAt;
}
