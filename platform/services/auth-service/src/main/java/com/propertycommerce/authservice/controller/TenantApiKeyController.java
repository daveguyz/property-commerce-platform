package com.propertycommerce.authservice.controller;

import com.propertycommerce.authservice.model.TenantApiKey;
import com.propertycommerce.authservice.service.TenantApiKeyService;
import com.propertycommerce.shared.dto.ApiResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Tenant API key management endpoints.
 *
 * POST /api/v1/tenants/{tenantId}/api-keys
 *   → Issue a new API key. Returns the plaintext key once.
 *   Body: { "label": "WordPress plugin", "test": false }
 *
 * GET  /api/v1/tenants/{tenantId}/api-keys
 *   → List all active keys for the tenant (prefix + metadata, never plaintext).
 *
 * DELETE /api/v1/tenants/{tenantId}/api-keys/{keyId}
 *   → Revoke a key immediately.
 *
 * POST /api/v1/auth/api-keys/validate   (gateway-internal — not externally documented)
 *   → Validate an API key and return tenant claims.
 *   Called by ApiKeyGatewayFilter via an internal Feign call.
 */
@RestController
@RequiredArgsConstructor
public class TenantApiKeyController {

    private final TenantApiKeyService keyService;

    // ── External tenant management ────────────────────────────────────────

    @PostMapping("/api/v1/tenants/{tenantId}/api-keys")
    public ResponseEntity<ApiResponse<TenantApiKeyService.IssuedKey>> issueKey(
            @PathVariable String tenantId,
            @RequestBody IssueKeyRequest req) {

        if (req.getLabel() == null || req.getLabel().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("A label is required"));
        }

        TenantApiKeyService.IssuedKey issued = keyService.issueKey(
                tenantId, req.getLabel(), req.isTest());

        return ResponseEntity.ok(ApiResponse.success(issued,
                "API key issued. Store the key securely — it cannot be retrieved again."));
    }

    @GetMapping("/api/v1/tenants/{tenantId}/api-keys")
    public ResponseEntity<ApiResponse<List<TenantApiKey>>> listKeys(
            @PathVariable String tenantId) {
        return ResponseEntity.ok(ApiResponse.success(keyService.listKeys(tenantId)));
    }

    @DeleteMapping("/api/v1/tenants/{tenantId}/api-keys/{keyId}")
    public ResponseEntity<ApiResponse<Void>> revokeKey(
            @PathVariable String tenantId,
            @PathVariable String keyId) {
        keyService.revokeKey(keyId);
        return ResponseEntity.ok(ApiResponse.success(null, "Key revoked"));
    }

    // ── Internal validation (called by ApiKeyGatewayFilter) ───────────────

    /**
     * Validates an API key and returns the tenant claims.
     * This endpoint is on the PUBLIC_PATHS list in JwtAuthGatewayFilter
     * so the gateway itself can call it without a JWT.
     *
     * In production this could be replaced by a Redis lookup to avoid
     * the intra-service hop — but starting with HTTP keeps it simple
     * and avoids cache-invalidation problems on key revocation.
     */
    @PostMapping("/api/v1/auth/api-keys/validate")
    public ResponseEntity<ApiResponse<TenantApiKeyService.KeyClaims>> validateKey(
            @RequestBody Map<String, String> body) {

        String key = body.get("key");
        return keyService.validateKey(key)
                .map(claims -> ResponseEntity.ok(ApiResponse.success(claims)))
                .orElseGet(() -> ResponseEntity.status(401)
                        .body(ApiResponse.error("Invalid or revoked API key")));
    }

    @Data
    public static class IssueKeyRequest {
        private String label;
        private boolean test = false;
    }
}
