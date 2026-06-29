package com.propertycommerce.authservice.controller;

import com.propertycommerce.authservice.service.RoleManagementService;
import com.propertycommerce.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

/**
 * Admin-only REST endpoints for role management.
 *
 * PUT  /api/v1/auth/users/{userId}/roles/{role}       — grant role
 * DELETE /api/v1/auth/users/{userId}/roles/{role}     — revoke role
 * GET  /api/v1/auth/users/{userId}/roles              — list user's roles
 *
 * All endpoints require the caller to hold the ADMIN or SUPERADMIN role.
 * Role is extracted from the JWT by @PreAuthorize.
 */
@RestController
@RequestMapping("/api/v1/auth/users")
@RequiredArgsConstructor
public class RoleManagementController {

    private final RoleManagementService roleManagementService;

    /**
     * Grant a role to a user.
     * Body: { "role": "auctioneer" }
     */
    @PutMapping("/{userId}/roles/{role}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> grantRole(
            @PathVariable String userId,
            @PathVariable String role,
            @AuthenticationPrincipal String adminUserId) {

        var updated = roleManagementService.grantRole(userId, role, adminUserId);
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("userId", updated.getId(),
                       "roles",  updated.getRoles(),
                       "action", "GRANTED",
                       "role",   role),
                "Role '" + role + "' granted to user " + userId));
    }

    /**
     * Revoke a role from a user.
     */
    @DeleteMapping("/{userId}/roles/{role}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> revokeRole(
            @PathVariable String userId,
            @PathVariable String role,
            @AuthenticationPrincipal String adminUserId) {

        var updated = roleManagementService.revokeRole(userId, role, adminUserId);
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("userId", updated.getId(),
                       "roles",  updated.getRoles(),
                       "action", "REVOKED",
                       "role",   role),
                "Role '" + role + "' revoked from user " + userId));
    }

    /**
     * Get all roles for a user.
     */
    @GetMapping("/{userId}/roles")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin') or #userId == authentication.principal")
    public ResponseEntity<ApiResponse<Set<String>>> getRoles(
            @PathVariable String userId) {

        return ResponseEntity.ok(ApiResponse.success(
                roleManagementService.getRoles(userId)));
    }
}
