package com.staysphere.auctionservice.controller;

import com.staysphere.auctionservice.model.AuctionLot;
import com.staysphere.auctionservice.service.AuctioneerAssignmentService;
import com.staysphere.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Auctioneer management endpoints.
 *
 * Assignment (seller or admin):
 *   PUT    /api/v1/auctions/{lotId}/auctioneer          — assign auctioneer to lot
 *   DELETE /api/v1/auctions/{lotId}/auctioneer          — remove auctioneer from lot
 *
 * Dashboard queries (auctioneer):
 *   GET    /api/v1/auctions/auctioneer/my-lots          — all lots assigned to me
 *   GET    /api/v1/auctions/auctioneer/my-lots/active   — active + upcoming only
 *   GET    /api/v1/auctions/{lotId}/auctioneer/my-role  — my role on this lot
 *
 * Note: role verification (user holds 'auctioneer' in JWT) is enforced by
 * the shared JwtAuthenticationFilter; we trust X-User-Roles header here.
 * The selector can also be enriched with a Feign call to auth-service if
 * strict runtime role verification is required before assignment.
 */
@RestController
@RequestMapping("/api/v1/auctions")
@RequiredArgsConstructor
public class AuctioneerController {

    private final AuctioneerAssignmentService assignmentService;

    // ─── Assignment ───────────────────────────────────────────────────────

    /**
     * Assign an auctioneer to a lot.
     * Caller must be the lot seller (enforced in service) or an admin
     * (pass X-User-Roles: admin to bypass seller check — TODO: enforce properly in Phase 7).
     *
     * Body: { "auctioneerId": "user-uuid" }
     */
    @PutMapping("/{lotId}/auctioneer")
    public ResponseEntity<ApiResponse<AuctionLot>> assignAuctioneer(
            @PathVariable String lotId,
            @RequestBody Map<String, String> body,
            @RequestHeader("X-User-Id") String requesterId) {

        String auctioneerId = body.get("auctioneerId");
        if (auctioneerId == null || auctioneerId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("auctioneerId is required"));
        }

        AuctionLot updated = assignmentService.assignAuctioneer(lotId, auctioneerId, requesterId);
        return ResponseEntity.ok(ApiResponse.success(updated,
                "Auctioneer assigned to lot " + lotId));
    }

    /**
     * Remove the auctioneer assignment from a lot.
     * Caller must be the lot seller.
     */
    @DeleteMapping("/{lotId}/auctioneer")
    public ResponseEntity<ApiResponse<AuctionLot>> removeAuctioneer(
            @PathVariable String lotId,
            @RequestHeader("X-User-Id") String requesterId) {

        AuctionLot updated = assignmentService.removeAuctioneer(lotId, requesterId);
        return ResponseEntity.ok(ApiResponse.success(updated,
                "Auctioneer removed from lot " + lotId));
    }

    // ─── Dashboard queries ────────────────────────────────────────────────

    /**
     * All lots assigned to the authenticated auctioneer, paginated.
     * Used by the auctioneer dashboard State A card list.
     */
    @GetMapping("/auctioneer/my-lots")
    public ResponseEntity<ApiResponse<Page<AuctionLot>>> getMyLots(
            @RequestHeader("X-User-Id") String auctioneerId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<AuctionLot> lots = assignmentService.getLotsForAuctioneer(auctioneerId, pageable);
        return ResponseEntity.ok(ApiResponse.success(lots));
    }

    /**
     * Active + upcoming lots only (SCHEDULED, OPEN, EXTENDED).
     * Used by the dashboard to auto-redirect when there is exactly one active lot.
     */
    @GetMapping("/auctioneer/my-lots/active")
    public ResponseEntity<ApiResponse<List<AuctionLot>>> getMyActiveLots(
            @RequestHeader("X-User-Id") String auctioneerId) {

        List<AuctionLot> lots = assignmentService.getActiveLotsForAuctioneer(auctioneerId);
        return ResponseEntity.ok(ApiResponse.success(lots));
    }

    /**
     * Returns this user's role on a specific lot.
     * Response: { "lotId": "...", "role": "auctioneer" | "seller" | "both" | "none" }
     * Used by plugin-auction-room.js to decide whether to show the auctioneer overlay.
     */
    @GetMapping("/{lotId}/auctioneer/my-role")
    public ResponseEntity<ApiResponse<Map<String, String>>> getMyRole(
            @PathVariable String lotId,
            @RequestHeader("X-User-Id") String userId) {

        String role = assignmentService.getLotRole(lotId, userId);
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("lotId", lotId, "userId", userId, "role", role)));
    }
}
