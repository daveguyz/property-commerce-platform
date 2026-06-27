package com.staysphere.shopify.service;

import com.staysphere.shared.events.RoleAssignedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Set;

/**
 * Syncs role assignments to Shopify customer tags so Liquid templates
 * can use {% if customer.tags contains 'auctioneer' %} for routing.
 *
 * Consumes Kafka topic "auth.role.assigned" published by auth-service
 * whenever an admin grants or revokes a role.
 *
 * Role → Shopify tag mapping:
 *   auctioneer → "auctioneer"
 *   host       → "host"
 *   admin      → "admin"
 *   superadmin → "superadmin"
 *
 * GUEST and HOST_PENDING are not synced (no Shopify-level routing needed).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RoleTagSyncService {

    private final RestTemplate restTemplate;

    @Value("${shopify.access-token}")  private String accessToken;
    @Value("${shopify.store-url}")     private String storeUrl;
    @Value("${shopify.api.version:2024-01}") private String apiVersion;

    /** Roles that map to a Shopify customer tag. */
    private static final Set<String> SYNCED_ROLES = Set.of(
            "auctioneer", "host", "admin", "superadmin"
    );

    @KafkaListener(topics = RoleAssignedEvent.TOPIC,
                   groupId = "shopify-role-tag-sync")
    public void onRoleAssigned(RoleAssignedEvent event) {
        if (!SYNCED_ROLES.contains(event.getRole())) {
            log.debug("[RoleTagSync] Role '{}' not synced to Shopify — skipping", event.getRole());
            return;
        }

        String shopifyCustomerId = event.getShopifyCustomerId();
        if (shopifyCustomerId == null || shopifyCustomerId.isBlank()) {
            log.info("[RoleTagSync] User {} has no linked Shopify customer — tag sync skipped",
                    event.getUserId());
            return;
        }

        try {
            if ("GRANTED".equals(event.getAction())) {
                addCustomerTag(shopifyCustomerId, event.getRole());
            } else if ("REVOKED".equals(event.getAction())) {
                removeCustomerTag(shopifyCustomerId, event.getRole());
            }
        } catch (Exception e) {
            log.error("[RoleTagSync] Failed to sync tag '{}' for customer {}: {}",
                    event.getRole(), shopifyCustomerId, e.getMessage());
            // Non-fatal — role is already set in auth-service JWT; Shopify tag
            // is a display convenience. A retry can be triggered manually.
        }
    }

    // ─── Private: Shopify Customer API calls ─────────────────────────────

    private void addCustomerTag(String shopifyCustomerId, String tag) {
        String currentTags = fetchCurrentTags(shopifyCustomerId);
        if (currentTags != null && currentTags.contains(tag)) {
            log.debug("[RoleTagSync] Customer {} already has tag '{}' — skipping", shopifyCustomerId, tag);
            return;
        }
        String newTags = currentTags == null || currentTags.isBlank()
                ? tag
                : currentTags + "," + tag;
        updateTags(shopifyCustomerId, newTags);
        log.info("[RoleTagSync] Added tag '{}' to Shopify customer {}", tag, shopifyCustomerId);
    }

    private void removeCustomerTag(String shopifyCustomerId, String tag) {
        String currentTags = fetchCurrentTags(shopifyCustomerId);
        if (currentTags == null || !currentTags.contains(tag)) {
            log.debug("[RoleTagSync] Customer {} does not have tag '{}' — skipping", shopifyCustomerId, tag);
            return;
        }
        String newTags = java.util.Arrays.stream(currentTags.split(","))
                .map(String::trim)
                .filter(t -> !t.equals(tag))
                .reduce((a, b) -> a + "," + b)
                .orElse("");
        updateTags(shopifyCustomerId, newTags);
        log.info("[RoleTagSync] Removed tag '{}' from Shopify customer {}", tag, shopifyCustomerId);
    }

    private String fetchCurrentTags(String shopifyCustomerId) {
        try {
            ResponseEntity<Map> res = restTemplate.exchange(
                    shopifyUrl() + "/customers/" + shopifyCustomerId + ".json?fields=tags",
                    HttpMethod.GET, new HttpEntity<>(buildHeaders()), Map.class);
            if (res.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> customer = (Map<String, Object>) res.getBody().get("customer");
                return customer != null ? (String) customer.get("tags") : null;
            }
        } catch (Exception e) {
            log.warn("[RoleTagSync] Could not fetch tags for customer {}: {}", shopifyCustomerId, e.getMessage());
        }
        return null;
    }

    private void updateTags(String shopifyCustomerId, String tags) {
        Map<String, Object> body = Map.of(
                "customer", Map.of("id", shopifyCustomerId, "tags", tags));
        restTemplate.exchange(
                shopifyUrl() + "/customers/" + shopifyCustomerId + ".json",
                HttpMethod.PUT, new HttpEntity<>(body, buildHeaders()), Map.class);
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Shopify-Access-Token", accessToken);
        return headers;
    }

    private String shopifyUrl() {
        String base = storeUrl.startsWith("https://") ? storeUrl : "https://" + storeUrl;
        return base + "/admin/api/" + apiVersion;
    }
}
