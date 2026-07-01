package com.propertycommerce.webhookrouter.controller;

import com.propertycommerce.shared.dto.ApiResponse;
import com.propertycommerce.webhookrouter.model.WebhookDelivery;
import com.propertycommerce.webhookrouter.model.WebhookEndpoint;
import com.propertycommerce.webhookrouter.repository.WebhookDeliveryRepository;
import com.propertycommerce.webhookrouter.service.WebhookEndpointService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoints for webhook management.
 *
 * POST /api/v1/webhooks
 *   Register a new endpoint. Returns the endpoint including the signing
 *   secret — the secret is shown once and cannot be retrieved again.
 *
 * GET  /api/v1/webhooks
 *   List all active endpoints for the tenant.
 *
 * DELETE /api/v1/webhooks/{id}
 *   Disable an endpoint.
 *
 * GET  /api/v1/webhooks/{id}/deliveries
 *   Paginated delivery log for an endpoint — used by the tenant
 *   dashboard and VS Code extension log stream.
 *
 * POST /api/v1/webhooks/{id}/test
 *   Send a synthetic test event to verify the endpoint is reachable.
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookEndpointService endpointService;
    private final WebhookDeliveryRepository deliveryRepo;

    @PostMapping
    public ResponseEntity<ApiResponse<WebhookEndpoint>> register(
            @RequestBody RegisterRequest req,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        if (req.getUrl() == null || req.getUrl().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("URL is required"));
        }

        WebhookEndpoint endpoint = endpointService.register(
                tenantId, req.getUrl(), req.getEventFilter());

        return ResponseEntity.ok(ApiResponse.success(endpoint,
                "Endpoint registered. Store the signingSecret — it cannot be retrieved again."));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WebhookEndpoint>>> list(
            @RequestHeader("X-Tenant-Id") String tenantId) {
        return ResponseEntity.ok(ApiResponse.success(
                endpointService.listForTenant(tenantId)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String id,
            @RequestHeader("X-Tenant-Id") String tenantId) {
        endpointService.delete(tenantId, id);
        return ResponseEntity.ok(ApiResponse.success(null, "Endpoint disabled"));
    }

    @GetMapping("/{id}/deliveries")
    public ResponseEntity<ApiResponse<Page<WebhookDelivery>>> deliveries(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                deliveryRepo.findByEndpointIdOrderByCreatedAtDesc(
                        id, PageRequest.of(page, size))));
    }

    @Data
    public static class RegisterRequest {
        private String url;
        private String eventFilter; // comma-separated, e.g. "auction.*,bid.placed"
    }
}
