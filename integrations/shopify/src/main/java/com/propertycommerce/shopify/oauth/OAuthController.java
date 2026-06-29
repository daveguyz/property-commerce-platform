package com.propertycommerce.shopify.oauth;

import com.propertycommerce.shopify.model.ShopifyStoreToken;
import com.propertycommerce.shopify.repository.ShopifyStoreTokenRepository;
import com.propertycommerce.shopify.service.ThemeProvisioningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.util.*;

/**
 * Handles the Shopify OAuth 2.0 install flow:
 *
 * 1. GET /oauth/shopify/install?shop=xxx.myshopify.com
 *    → Redirects merchant to Shopify's permission screen
 *
 * 2. GET /oauth/shopify/callback?code=xxx&shop=xxx&hmac=xxx&state=xxx
 *    → Exchanges code for access token
 *    → Persists token
 *    → Triggers async theme provisioning
 *    → Redirects merchant to their Shopify admin
 */
@RestController
@RequestMapping("/oauth/shopify")
@Slf4j @RequiredArgsConstructor
public class OAuthController {

    private final RestTemplate restTemplate;
    private final ShopifyStoreTokenRepository tokenRepository;
    private final ThemeProvisioningService provisioningService;

    @Value("${shopify.api.key}")              private String apiKey;
    @Value("${shopify.api.secret}")           private String apiSecret;
    @Value("${shopify.oauth.scopes}")         private String scopes;
    @Value("${shopify.oauth.redirect-uri}")   private String redirectUri;
    @Value("${pcp.frontend.url:https://propertycommerce.io}")
    private String frontendUrl;

    // CSRF nonce store (in-memory; use Redis in production for multi-instance)
    private final Set<String> validNonces = Collections.synchronizedSet(new HashSet<>());

    /** Step 1 — Redirect to Shopify's OAuth screen */
    @GetMapping("/install")
    public ResponseEntity<Void> install(@RequestParam String shop) {
        if (!isValidShopDomain(shop)) {
            return ResponseEntity.badRequest().build();
        }

        String nonce = UUID.randomUUID().toString();
        validNonces.add(nonce);

        String authUrl = "https://" + shop + "/admin/oauth/authorize" +
                "?client_id=" + apiKey +
                "&scope=" + scopes +
                "&redirect_uri=" + redirectUri +
                "&state=" + nonce;

        log.info("[OAuth] Redirecting install for shop {}", shop);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(authUrl))
                .build();
    }

    /** Step 2 — Receive callback with auth code, exchange for token */
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam String code,
            @RequestParam String shop,
            @RequestParam String hmac,
            @RequestParam String state,
            @RequestParam(required = false) String timestamp) {

        // Validate CSRF nonce
        if (!validNonces.remove(state)) {
            log.warn("[OAuth] Invalid nonce for shop {}", shop);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Validate HMAC signature
        if (!validateHmac(hmac, buildHmacMessage(shop, code, state, timestamp))) {
            log.warn("[OAuth] HMAC validation failed for shop {}", shop);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!isValidShopDomain(shop)) {
            return ResponseEntity.badRequest().build();
        }

        // Exchange code for permanent access token
        String accessToken = exchangeCodeForToken(shop, code);
        if (accessToken == null) {
            log.error("[OAuth] Token exchange failed for shop {}", shop);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        // Persist (upsert — handles re-installs)
        ShopifyStoreToken token = tokenRepository.findByShopDomain(shop)
                .orElse(ShopifyStoreToken.builder().shopDomain(shop).build());
        token.setAccessToken(accessToken);
        token.setActive(true);
        token.setUninstalledAt(null);
        ShopifyStoreToken saved = tokenRepository.save(token);
        log.info("[OAuth] Access token persisted for shop {}", shop);

        // Async theme provisioning (don't block the OAuth redirect)
        provisionThemeAsync(shop, accessToken, saved.getId());

        // Register required webhooks
        registerWebhooks(shop, accessToken);

        // Redirect merchant to their Shopify admin
        String adminUrl = "https://" + shop + "/admin";
        log.info("[OAuth] Install complete for {}. Redirecting to admin.", shop);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(adminUrl))
                .build();
    }

    // ─── Token exchange ───────────────────────────────────────────────────

    private String exchangeCodeForToken(String shop, String code) {
        try {
            Map<String, String> body = Map.of(
                    "client_id",     apiKey,
                    "client_secret", apiSecret,
                    "code",          code
            );
            ResponseEntity<Map> resp = restTemplate.postForEntity(
                    "https://" + shop + "/admin/oauth/access_token", body, Map.class);
            return resp.getBody() != null ? (String) resp.getBody().get("access_token") : null;
        } catch (Exception e) {
            log.error("[OAuth] Token exchange error for {}: {}", shop, e.getMessage());
            return null;
        }
    }

    // ─── Webhook registration ─────────────────────────────────────────────

    private void registerWebhooks(String shop, String accessToken) {
        String[] topics = {
                "orders/create", "orders/cancelled",
                "customers/create", "customers/update",
                "app/uninstalled"
        };
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Shopify-Access-Token", accessToken);

        for (String topic : topics) {
            try {
                Map<String, Object> webhook = Map.of(
                        "topic",   topic,
                        "address", frontendUrl.replace("https://propertycommerce.io",
                                System.getenv().getOrDefault("APP_PUBLIC_URL",
                                        "https://app.propertycommerce.io"))
                                + "/webhook/shopify/" + topic.replace("/", "/"),
                        "format", "json"
                );
                restTemplate.exchange(
                        "https://" + shop + "/admin/api/2024-01/webhooks.json",
                        HttpMethod.POST,
                        new HttpEntity<>(Map.of("webhook", webhook), headers),
                        Map.class
                );
                log.debug("[OAuth] Registered webhook {} for {}", topic, shop);
            } catch (Exception e) {
                log.warn("[OAuth] Webhook registration failed for {}/{}: {}", shop, topic, e.getMessage());
            }
        }
    }

    // ─── Async provisioning ───────────────────────────────────────────────

    @Async
    public void provisionThemeAsync(String shopDomain, String accessToken, String tokenId) {
        try {
            long themeId = provisioningService.provisionTheme(shopDomain, accessToken);
            // Update the token record with the theme ID
            tokenRepository.findById(tokenId).ifPresent(token -> {
                token.setProvisionedThemeId(themeId);
                token.setThemeProvisioned(true);
                tokenRepository.save(token);
            });
            log.info("[OAuth] Async provisioning complete for {}. ThemeId={}", shopDomain, themeId);
        } catch (Exception e) {
            log.error("[OAuth] Async provisioning failed for {}: {}", shopDomain, e.getMessage());
        }
    }

    // ─── Validation helpers ───────────────────────────────────────────────

    private boolean isValidShopDomain(String shop) {
        return shop != null && shop.matches("[a-zA-Z0-9][a-zA-Z0-9\\-]*\\.myshopify\\.com");
    }

    private String buildHmacMessage(String shop, String code, String state, String timestamp) {
        // Shopify signs: code=...&shop=...&state=...&timestamp=...
        List<String> parts = new ArrayList<>();
        parts.add("code=" + code);
        parts.add("shop=" + shop);
        parts.add("state=" + state);
        if (timestamp != null) parts.add("timestamp=" + timestamp);
        Collections.sort(parts);
        return String.join("&", parts);
    }

    private boolean validateHmac(String receivedHmac, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(apiSecret.getBytes(), "HmacSHA256"));
            byte[] digest = mac.doFinal(message.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) hex.append(String.format("%02x", b));
            return hex.toString().equals(receivedHmac);
        } catch (Exception e) {
            log.error("[OAuth] HMAC computation error: {}", e.getMessage());
            return false;
        }
    }
}
