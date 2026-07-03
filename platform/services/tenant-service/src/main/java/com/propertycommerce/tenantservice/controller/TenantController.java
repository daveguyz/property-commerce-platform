package com.propertycommerce.tenantservice.controller;

import com.propertycommerce.shared.dto.ApiResponse;
import com.propertycommerce.tenantservice.model.Tenant;
import com.propertycommerce.tenantservice.service.TenantService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Tenant management API.
 *
 * PUBLIC (no auth — served to every visitor's browser via the SDK):
 *   GET /api/v1/tenants/{id}/config — white-label branding + feature flags
 *
 * ADMIN (platform-admin JWT):
 *   POST /api/v1/tenants           — register a new tenant
 *   GET  /api/v1/tenants           — list all tenants
 *
 * TENANT (tenant JWT or API key):
 *   PATCH /api/v1/tenants/{id}/branding — update branding tokens
 */
@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    public ResponseEntity<ApiResponse<Tenant>> register(@RequestBody RegisterRequest req) {
        if (req.getSlug() == null || req.getName() == null || req.getContactEmail() == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("slug, name, and contactEmail are required"));
        }
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    tenantService.register(req.getSlug(), req.getName(), req.getContactEmail()),
                    "Tenant registered"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Tenant>>> listAll() {
        return ResponseEntity.ok(ApiResponse.success(tenantService.listAll()));
    }

    /** Public — the SDK fetches this on PCP.init(). Cached server-side. */
    @GetMapping("/{id}/config")
    public ResponseEntity<ApiResponse<Map<String, Object>>> publicConfig(@PathVariable String id) {
        try {
            return ResponseEntity.ok(ApiResponse.success(tenantService.getPublicConfig(id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(ApiResponse.error("Tenant not found"));
        }
    }

    @PatchMapping("/{id}/branding")
    public ResponseEntity<ApiResponse<Tenant>> updateBranding(
            @PathVariable String id,
            @RequestBody Map<String, String> updates) {
        return ResponseEntity.ok(ApiResponse.success(
                tenantService.updateBranding(id, updates), "Branding updated"));
    }

    @Data
    public static class RegisterRequest {
        private String slug;
        private String name;
        private String contactEmail;
    }
}
