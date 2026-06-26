package com.staysphere.shopify.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * ThemeProvisioningService — pushes the StaySphere Shopify theme
 * to a merchant's store via the Shopify Admin Assets API when they
 * install the StaySphere AOS app.
 *
 * Flow:
 *   1. Create a new theme on the store ("StaySphere AOS")
 *   2. Iterate over all theme files packed in the classpath (theme-files/)
 *   3. PUT each file to /admin/api/{version}/themes/{id}/assets.json
 *   4. Persist the theme ID against the store token record
 *
 * Files are read from src/main/resources/theme-files/ at build time.
 * In production, this directory is populated by the CI/CD pipeline
 * from the shopify-theme-branch of the repository.
 */
@Service @Slf4j @RequiredArgsConstructor
public class ThemeProvisioningService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${shopify.api.version:2024-01}") private String apiVersion;
    @Value("${staysphere.app.name:StaySphere AOS}")  private String appName;
    @Value("${staysphere.app.api-gateway-url:}")     private String defaultApiGatewayUrl;

    private static final int RATE_LIMIT_DELAY_MS = 500; // Shopify: ~2 req/s on asset PUT

    /**
     * Provision the full theme to a store after OAuth install.
     *
     * @param shopDomain   e.g. "staysphere-aos.myshopify.com"
     * @param accessToken  Admin API token (shpat_...)
     * @return the created Shopify theme ID
     */
    public long provisionTheme(String shopDomain, String accessToken) {
        log.info("[Provisioning] Starting theme provisioning for {}", shopDomain);

        // Step 1: Create theme
        long themeId = createTheme(shopDomain, accessToken);
        log.info("[Provisioning] Created theme {} on {}", themeId, shopDomain);

        // Step 2: Push all theme files
        List<String[]> files = collectThemeFiles();
        log.info("[Provisioning] Pushing {} theme files to {}", files.size(), shopDomain);

        int pushed = 0;
        int failed = 0;
        for (String[] keyAndContent : files) {
            String key     = keyAndContent[0];
            String content = keyAndContent[1];
            try {
                pushAsset(shopDomain, accessToken, themeId, key, content);
                pushed++;
                if (pushed % 10 == 0) {
                    log.debug("[Provisioning] {}/{} files pushed to {}", pushed, files.size(), shopDomain);
                }
                // Respect Shopify's asset API rate limit
                Thread.sleep(RATE_LIMIT_DELAY_MS);
            } catch (Exception e) {
                failed++;
                log.warn("[Provisioning] Failed to push {}: {}", key, e.getMessage());
            }
        }

        log.info("[Provisioning] Complete for {}. Pushed={} Failed={} ThemeId={}",
                shopDomain, pushed, failed, themeId);
        return themeId;
    }

    /**
     * Update an existing theme (re-push changed files only).
     * Used when the merchant updates to a new version of StaySphere AOS.
     */
    public void updateTheme(String shopDomain, String accessToken, long themeId) {
        log.info("[Provisioning] Updating theme {} on {}", themeId, shopDomain);
        List<String[]> files = collectThemeFiles();
        int updated = 0;
        for (String[] keyAndContent : files) {
            try {
                pushAsset(shopDomain, accessToken, themeId, keyAndContent[0], keyAndContent[1]);
                updated++;
                Thread.sleep(RATE_LIMIT_DELAY_MS);
            } catch (Exception e) {
                log.warn("[Provisioning] Update failed for {}: {}", keyAndContent[0], e.getMessage());
            }
        }
        log.info("[Provisioning] Update complete. {} files updated on {}", updated, shopDomain);
    }

    /**
     * Delete our theme when the app is uninstalled.
     * Merchant's other themes are unaffected.
     */
    public void deprovisionTheme(String shopDomain, String accessToken, long themeId) {
        try {
            HttpHeaders headers = buildHeaders(accessToken);
            restTemplate.exchange(
                    shopifyUrl(shopDomain) + "/themes/" + themeId + ".json",
                    HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
            log.info("[Provisioning] Deleted theme {} from {}", themeId, shopDomain);
        } catch (Exception e) {
            log.warn("[Provisioning] Could not delete theme {}: {}", themeId, e.getMessage());
        }
    }

    // ─── Private helpers ──────────────────────────────────────────────────

    private long createTheme(String shopDomain, String accessToken) {
        HttpHeaders headers = buildHeaders(accessToken);
        Map<String, Object> theme = Map.of(
                "name", appName,
                "role", "unpublished"  // don't auto-publish; let merchant decide
        );
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                    shopifyUrl(shopDomain) + "/themes.json",
                    HttpMethod.POST,
                    new HttpEntity<>(Map.of("theme", theme), headers),
                    Map.class
            );
            @SuppressWarnings("unchecked")
            Map<String, Object> created = (Map<String, Object>) resp.getBody().get("theme");
            return ((Number) created.get("id")).longValue();
        } catch (Exception e) {
            log.error("[Provisioning] Theme creation failed for {}: {}", shopDomain, e.getMessage());
            throw new IllegalStateException("Theme creation failed: " + e.getMessage(), e);
        }
    }

    private void pushAsset(String shopDomain, String accessToken,
                           long themeId, String key, String value) {
        HttpHeaders headers = buildHeaders(accessToken);
        Map<String, Object> asset = new LinkedHashMap<>();
        asset.put("key", key);
        asset.put("value", value);

        restTemplate.exchange(
                shopifyUrl(shopDomain) + "/themes/" + themeId + "/assets.json",
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("asset", asset), headers),
                Map.class
        );
    }

    /**
     * Collect all theme files from the classpath.
     * Returns a list of [key, content] pairs.
     *
     * In CI, theme-files/ is populated from the theme branch.
     * Locally, if theme-files/ is empty, returns an empty list
     * (provisioning is a no-op — safe for local dev).
     */
    private List<String[]> collectThemeFiles() {
        List<String[]> result = new ArrayList<>();
        try {
            PathMatchingResourcePatternResolver resolver =
                    new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:theme-files/**/*");

            for (Resource resource : resources) {
                if (!resource.isReadable()) continue;
                String filename = resource.getURL().getPath();
                // Convert classpath path → Shopify theme key
                // e.g. .../theme-files/sections/hero.liquid → sections/hero.liquid
                int idx = filename.indexOf("theme-files/");
                if (idx < 0) continue;
                String key = filename.substring(idx + "theme-files/".length());
                if (key.isBlank()) continue;

                String content = new String(resource.getInputStream().readAllBytes(),
                        StandardCharsets.UTF_8);
                result.add(new String[]{ key, content });
            }
        } catch (Exception e) {
            log.warn("[Provisioning] Could not read theme files from classpath: {}", e.getMessage());
        }
        log.debug("[Provisioning] Collected {} theme files", result.size());
        return result;
    }

    private HttpHeaders buildHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Shopify-Access-Token", accessToken);
        return headers;
    }

    private String shopifyUrl(String shopDomain) {
        String domain = shopDomain.startsWith("https://") ? shopDomain
                : "https://" + shopDomain;
        return domain + "/admin/api/" + apiVersion;
    }
}
