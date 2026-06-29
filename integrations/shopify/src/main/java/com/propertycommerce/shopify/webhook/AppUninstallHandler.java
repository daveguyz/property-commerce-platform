package com.propertycommerce.shopify.webhook;

import com.propertycommerce.shopify.model.ShopifyStoreToken;
import com.propertycommerce.shopify.repository.ShopifyStoreTokenRepository;
import com.propertycommerce.shopify.service.ThemeProvisioningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Handles app/uninstalled webhook from Shopify.
 * Called when a merchant removes the Property Commerce Platform app from their store.
 *
 * Steps:
 *   1. Verify HMAC signature
 *   2. Mark the store token as inactive
 *   3. Optionally delete the provisioned theme
 *   4. Log for audit
 */
@RestController
@RequestMapping("/webhook/shopify/app")
@Slf4j @RequiredArgsConstructor
public class AppUninstallHandler {

    private final ShopifyStoreTokenRepository tokenRepository;
    private final ThemeProvisioningService provisioningService;

    @Value("${shopify.webhook-secret}") private String webhookSecret;

    @PostMapping("/uninstalled")
    public ResponseEntity<String> handleUninstall(
            @RequestHeader("X-Shopify-Hmac-Sha256") String hmac,
            @RequestHeader(value = "X-Shopify-Shop-Domain", required = false) String shopDomain,
            @RequestBody String payload) {

        if (!verifyHmac(payload, hmac)) {
            log.warn("[Uninstall] Invalid HMAC for shop {}", shopDomain);
            return ResponseEntity.status(401).body("Invalid signature");
        }

        if (shopDomain == null) {
            log.warn("[Uninstall] Missing shop domain header");
            return ResponseEntity.badRequest().body("Missing shop domain");
        }

        tokenRepository.findByShopDomain(shopDomain).ifPresent(token -> {
            // 1. Mark inactive
            token.setActive(false);
            token.setUninstalledAt(LocalDateTime.now());
            tokenRepository.save(token);
            log.info("[Uninstall] Store {} marked inactive", shopDomain);

            // 2. Clean up theme (best-effort)
            if (Boolean.TRUE.equals(token.getThemeProvisioned())
                    && token.getProvisionedThemeId() != null
                    && token.getAccessToken() != null) {
                try {
                    provisioningService.deprovisionTheme(
                            shopDomain, token.getAccessToken(), token.getProvisionedThemeId());
                } catch (Exception e) {
                    log.warn("[Uninstall] Theme cleanup failed for {}: {}", shopDomain, e.getMessage());
                }
            }
        });

        if (!tokenRepository.existsByShopDomain(shopDomain)) {
            log.info("[Uninstall] Unknown shop uninstalled: {} (no record found)", shopDomain);
        }

        return ResponseEntity.ok("ok");
    }

    private boolean verifyHmac(String payload, String receivedHmac) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(), "HmacSHA256"));
            String computed = Base64.getEncoder().encodeToString(
                    mac.doFinal(payload.getBytes()));
            return computed.equals(receivedHmac);
        } catch (Exception e) {
            log.error("[Uninstall] HMAC error: {}", e.getMessage());
            return false;
        }
    }
}
