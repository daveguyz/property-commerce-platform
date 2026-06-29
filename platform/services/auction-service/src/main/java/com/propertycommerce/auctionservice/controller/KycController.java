package com.propertycommerce.auctionservice.controller;

import com.propertycommerce.auctionservice.model.KycRecord;
import com.propertycommerce.auctionservice.service.KycService;
import com.propertycommerce.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/kyc")
@RequiredArgsConstructor @Slf4j
public class KycController {

    private final KycService kycService;

    /** Create a new Stripe Identity verification session for the authenticated user. */
    @PostMapping("/session")
    public ResponseEntity<ApiResponse<KycRecord>> createSession(
            @RequestHeader("X-User-Id")    String userId,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestParam(required = false) String lotId) {
        KycRecord record = kycService.createVerificationSession(userId, userEmail, lotId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(record, "KYC session created — visit verificationUrl to complete"));
    }

    /** Get the current KYC status for the authenticated user. */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<KycRecord>> getStatus(
            @RequestHeader("X-User-Id") String userId) {
        KycRecord record = kycService.getKycStatus(userId);
        return ResponseEntity.ok(ApiResponse.success(record));
    }

    /** Check if the authenticated user is KYC-verified (simple boolean). */
    @GetMapping("/verified")
    public ResponseEntity<ApiResponse<Boolean>> isVerified(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.success(kycService.isUserVerified(userId)));
    }

    /**
     * Stripe Identity webhook — receives verification events.
     * This endpoint must be publicly accessible (no JWT) — Stripe calls it directly.
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> stripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            kycService.handleStripeWebhook(payload, sigHeader);
            return ResponseEntity.ok("ok");
        } catch (IllegalArgumentException e) {
            log.error("[KYC Webhook] Invalid signature: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            log.error("[KYC Webhook] Processing error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook error");
        }
    }
}
