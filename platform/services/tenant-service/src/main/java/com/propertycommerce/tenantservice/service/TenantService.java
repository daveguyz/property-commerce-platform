package com.propertycommerce.tenantservice.service;

import com.propertycommerce.tenantservice.model.Tenant;
import com.propertycommerce.tenantservice.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Tenant lifecycle + white-label config resolution.
 *
 * getPublicConfig() is called by the SDK on every PCP.init() from every
 * visitor's browser, so it is cached (Redis via Spring Cache) and returns
 * only the branding subset — never quotas, status, or contact details.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;

    @Transactional
    public Tenant register(String slug, String name, String contactEmail) {
        if (tenantRepository.existsBySlug(slug)) {
            throw new IllegalArgumentException("Slug already taken: " + slug);
        }
        Tenant tenant = Tenant.builder()
                .slug(slug)
                .name(name)
                .contactEmail(contactEmail)
                .build();
        Tenant saved = tenantRepository.save(tenant);
        log.info("[Tenant] Registered tenant {} ({})", saved.getId(), slug);
        return saved;
    }

    public Tenant get(String id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + id));
    }

    /**
     * Public branding config — the exact shape PCPConfig.theme expects,
     * plus feature flags the SDK uses to hide unavailable widgets.
     * Cached: this endpoint receives traffic from every end-user page load.
     */
    @Cacheable(value = "tenant-config", key = "#id")
    public Map<String, Object> getPublicConfig(String id) {
        Tenant t = get(id);
        return Map.of(
                "tenantId", t.getId(),
                "name", t.getName(),
                "theme", Map.of(
                        "primaryColor",     t.getPrimaryColor(),
                        "primaryTextColor", t.getPrimaryTextColor(),
                        "borderRadius",     t.getBorderRadius(),
                        "logoUrl",          t.getLogoUrl() != null ? t.getLogoUrl() : ""
                ),
                "currency", t.getDefaultCurrency(),
                "locale",   t.getDefaultLocale(),
                "allowedCurrencies", t.getAllowedCurrencies().split(","),
                "features", Map.of(
                        "auctions",   t.isFeatureAuctions(),
                        "bookings",   t.isFeatureBookings(),
                        "messaging",  t.isFeatureMessaging(),
                        "ai",         t.isFeatureAi(),
                        "livestream", t.isFeatureLivestream()
                )
        );
    }

    @Transactional
    @CacheEvict(value = "tenant-config", key = "#id")
    public Tenant updateBranding(String id, Map<String, String> updates) {
        Tenant t = get(id);
        if (updates.containsKey("logoUrl"))          t.setLogoUrl(updates.get("logoUrl"));
        if (updates.containsKey("primaryColor"))     t.setPrimaryColor(updates.get("primaryColor"));
        if (updates.containsKey("primaryTextColor")) t.setPrimaryTextColor(updates.get("primaryTextColor"));
        if (updates.containsKey("borderRadius"))     t.setBorderRadius(updates.get("borderRadius"));
        if (updates.containsKey("defaultCurrency"))  t.setDefaultCurrency(updates.get("defaultCurrency"));
        if (updates.containsKey("defaultLocale"))    t.setDefaultLocale(updates.get("defaultLocale"));
        if (updates.containsKey("allowedCurrencies"))t.setAllowedCurrencies(updates.get("allowedCurrencies"));
        return tenantRepository.save(t);
    }

    public List<Tenant> listAll() {
        return tenantRepository.findAll();
    }
}
