package com.propertycommerce.tenantservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * A tenant is one customer of the platform — a real estate agency,
 * a property portal, an auction house. All domain data across every
 * microservice is scoped by tenantId (row-level tenancy).
 */
@Entity
@Table(name = "tenants", indexes = {
    @Index(name = "idx_tenant_slug", columnList = "slug", unique = true)
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Tenant {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true, length = 60)
    private String slug;

    @Column(nullable = false)
    private String name;

    @Column(name = "contact_email", nullable = false)
    private String contactEmail;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TenantStatus status = TenantStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "quota_tier")
    @Builder.Default
    private QuotaTier quotaTier = QuotaTier.FREE;

    // ── White-label branding (served to the SDK at init) ────────────────
    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "primary_color", length = 9)
    @Builder.Default
    private String primaryColor = "#6366f1";

    @Column(name = "primary_text_color", length = 9)
    @Builder.Default
    private String primaryTextColor = "#ffffff";

    @Column(name = "border_radius", length = 10)
    @Builder.Default
    private String borderRadius = "8px";

    @Column(name = "default_currency", length = 3)
    @Builder.Default
    private String defaultCurrency = "USD";

    @Column(name = "default_locale", length = 5)
    @Builder.Default
    private String defaultLocale = "en";

    @Column(name = "allowed_currencies")
    @Builder.Default
    private String allowedCurrencies = "USD";

    // ── Feature flags ────────────────────────────────────────────────────
    @Builder.Default
    @Column(name = "feature_auctions")   private boolean featureAuctions = true;
    @Builder.Default
    @Column(name = "feature_bookings")   private boolean featureBookings = true;
    @Builder.Default
    @Column(name = "feature_messaging")  private boolean featureMessaging = true;
    @Builder.Default
    @Column(name = "feature_ai")         private boolean featureAi = false;
    @Builder.Default
    @Column(name = "feature_livestream") private boolean featureLivestream = false;

    // ── Quotas ───────────────────────────────────────────────────────────
    @Column(name = "api_calls_per_day")
    @Builder.Default
    private int apiCallsPerDay = 10_000;

    @Column(name = "bids_per_minute")
    @Builder.Default
    private int bidsPerMinute = 60;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum TenantStatus { ACTIVE, SUSPENDED, CANCELLED }
    public enum QuotaTier { FREE, PROFESSIONAL, ENTERPRISE }
}
