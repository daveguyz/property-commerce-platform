package com.propertycommerce.paymentservice.controller;
import com.propertycommerce.paymentservice.service.StripePaymentService;
import com.propertycommerce.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Map;

@RestController @RequestMapping("/api/v1/payments") @RequiredArgsConstructor @Slf4j
public class PaymentController {
    private final StripePaymentService stripePaymentService;

    @PostMapping("/initiate")
    @PreAuthorize("hasRole('GUEST')")
    public ResponseEntity<ApiResponse<Map<String, String>>> initiatePayment(
            @RequestBody Map<String, Object> request) throws Exception {
        Map<String, String> result = stripePaymentService.createPaymentIntent(
                (String) request.get("bookingId"), (String) request.get("guestId"),
                (String) request.get("hostId"), new BigDecimal(request.get("amount").toString()),
                new BigDecimal(request.get("hostPayout").toString()),
                new BigDecimal(request.get("platformFee").toString()),
                (String) request.getOrDefault("currency", "NAD"));
        return ResponseEntity.ok(ApiResponse.success(result, "Payment intent created"));
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(@RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        stripePaymentService.handleWebhookEvent(payload, sigHeader);
        return ResponseEntity.ok("received");
    }

    @PostMapping("/host/connect")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<ApiResponse<Map<String, String>>> connectStripeAccount(
            @RequestBody Map<String, String> request, @AuthenticationPrincipal String hostId) throws Exception {
        return ResponseEntity.ok(ApiResponse.success(
                stripePaymentService.createHostConnectAccount(hostId, request.get("email"),
                        request.getOrDefault("country", "NA"))));
    }

    @PostMapping("/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> processRefund(
            @RequestBody Map<String, Object> request) throws Exception {
        return ResponseEntity.ok(ApiResponse.success(
                stripePaymentService.processRefund((String) request.get("bookingId"),
                        new BigDecimal(request.get("refundAmount").toString()))));
    }
}
