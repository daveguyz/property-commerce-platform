package com.staysphere.bookingengine.controller;

import com.staysphere.bookingengine.model.PurchaseAgreement;
import com.staysphere.bookingengine.service.PurchaseAgreementService;
import com.staysphere.shared.dto.ApiResponse;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST endpoints for the purchase agreement workflow.
 *
 * Public signing (token-auth):
 *   POST /api/v1/agreements/sign/buyer?token={token}  → buyer e-signs
 *   POST /api/v1/agreements/sign/seller?token={token} → seller e-signs
 *
 * Authenticated queries:
 *   GET  /api/v1/agreements?lotId={id}   → get agreement for a lot
 *   GET  /api/v1/agreements/mine/buyer   → all agreements as buyer
 *   GET  /api/v1/agreements/mine/seller  → all agreements as seller
 *
 * Auctioneer / admin actions:
 *   POST /api/v1/agreements/{id}/offer-next  → offer lot to next bidder
 *   POST /api/v1/agreements/{id}/confirm-payment → mark payment confirmed
 */
@RestController
@RequestMapping("/api/v1/agreements")
@RequiredArgsConstructor
public class PurchaseAgreementController {

    private final PurchaseAgreementService agreementService;

    // ── Query ─────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<PurchaseAgreement>> getByLot(
            @RequestParam String lotId) {
        return ResponseEntity.ok(ApiResponse.success(
                agreementService.getByLotId(lotId)));
    }

    @GetMapping("/mine/buyer")
    public ResponseEntity<ApiResponse<List<PurchaseAgreement>>> getMyBuyerAgreements(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.success(
                agreementService.getForBuyer(userId)));
    }

    @GetMapping("/mine/seller")
    public ResponseEntity<ApiResponse<List<PurchaseAgreement>>> getMySellerAgreements(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.success(
                agreementService.getForSeller(userId)));
    }

    // ── Signing ───────────────────────────────────────────────────────

    /**
     * Buyer e-signs via the tokenised link emailed to them.
     * Body: { "signatureData": "base64png_or_typed_name" }
     */
    @PostMapping("/sign/buyer")
    public ResponseEntity<ApiResponse<PurchaseAgreement>> buyerSign(
            @RequestParam String token,
            @RequestBody SignRequest req,
            @RequestHeader("X-User-Id") String userId) {

        if (req.getSignatureData() == null || req.getSignatureData().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Signature data is required"));
        }

        PurchaseAgreement signed = agreementService.recordBuyerSignature(
                token, req.getSignatureData(), userId);

        return ResponseEntity.ok(ApiResponse.success(signed,
                "Your signature has been recorded. Awaiting seller signature."));
    }

    /**
     * Seller e-signs via the tokenised link emailed to them.
     */
    @PostMapping("/sign/seller")
    public ResponseEntity<ApiResponse<PurchaseAgreement>> sellerSign(
            @RequestParam String token,
            @RequestBody SignRequest req,
            @RequestHeader("X-User-Id") String userId) {

        if (req.getSignatureData() == null || req.getSignatureData().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Signature data is required"));
        }

        PurchaseAgreement signed = agreementService.recordSellerSignature(
                token, req.getSignatureData(), userId);

        String message = signed.getStatus().name().equals("FULLY_EXECUTED")
                ? "Agreement fully executed. Conveyancing has been initiated."
                : "Your signature has been recorded.";

        return ResponseEntity.ok(ApiResponse.success(signed, message));
    }

    // ── Post-close actions ────────────────────────────────────────────

    /**
     * Offer the lot to the next highest bidder after a default.
     * Body: { "nextBidderId": "user-uuid" }
     */
    @PostMapping("/{id}/offer-next")
    public ResponseEntity<ApiResponse<PurchaseAgreement>> offerToNextBidder(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {

        String nextBidderId = body != null ? body.get("nextBidderId") : null;
        if (nextBidderId == null || nextBidderId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("nextBidderId is required"));
        }

        return ResponseEntity.ok(ApiResponse.success(
                agreementService.offerToNextBidder(id, nextBidderId),
                "Lot offered to next bidder"));
    }

    /**
     * Confirm that the balance payment has been received.
     * Called by payment-service or manually by admin.
     * Body: { "lotId": "..." }
     */
    @PostMapping("/{id}/confirm-payment")
    public ResponseEntity<ApiResponse<PurchaseAgreement>> confirmPayment(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {

        String lotId = body != null ? body.get("lotId") : null;
        if (lotId == null || lotId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("lotId is required"));
        }

        return ResponseEntity.ok(ApiResponse.success(
                agreementService.confirmPayment(lotId),
                "Payment confirmed"));
    }

    // ── DTO ───────────────────────────────────────────────────────────

    @Data @NoArgsConstructor
    public static class SignRequest {
        private String signatureData; // base64 canvas PNG or typed name
    }
}
