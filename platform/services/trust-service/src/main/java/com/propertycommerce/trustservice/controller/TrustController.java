package com.propertycommerce.trustservice.controller;
import com.propertycommerce.trustservice.service.*;
import com.propertycommerce.shared.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequiredArgsConstructor
public class TrustController {
    private final TrustScoreService trustScoreService;
    private final ReviewService reviewService;

    @GetMapping("/api/v1/trust/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TrustScoreService.TrustScoreBreakdown>> getTrustScore(
            @PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(trustScoreService.getScoreBreakdown(userId)));
    }

    @PostMapping("/api/v1/trust/{userId}/recalculate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Double>> recalculate(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(trustScoreService.recalculateTrustScore(userId)));
    }

    @PostMapping("/api/v1/reviews")
    @PreAuthorize("hasRole('GUEST')")
    public ResponseEntity<ApiResponse<ReviewDTO>> createReview(
            @Valid @RequestBody ReviewDTO dto, @AuthenticationPrincipal String guestId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(reviewService.createReview(dto, guestId), "Review submitted"));
    }

    @GetMapping("/api/v1/properties/{propertyId}/reviews")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewDTO>>> getPropertyReviews(
            @PathVariable String propertyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                reviewService.getPropertyReviews(propertyId, PageRequest.of(page, size))));
    }
}
