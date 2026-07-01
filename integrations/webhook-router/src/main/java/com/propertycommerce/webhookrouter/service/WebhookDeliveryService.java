package com.propertycommerce.webhookrouter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.propertycommerce.webhookrouter.model.WebhookDelivery;
import com.propertycommerce.webhookrouter.model.WebhookDelivery.DeliveryStatus;
import com.propertycommerce.webhookrouter.model.WebhookEndpoint;
import com.propertycommerce.webhookrouter.repository.WebhookDeliveryRepository;
import com.propertycommerce.webhookrouter.repository.WebhookEndpointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Delivers webhook events to tenant-registered endpoints.
 *
 * Delivery model:
 *   - Up to 4 attempts with exponential backoff: 1min, 5min, 30min, 2h
 *   - On 4th failure the endpoint's failureCount is incremented
 *   - After 10 consecutive failures the endpoint is automatically disabled
 *   - Every delivery (success or failure) is logged to webhook_deliveries
 *
 * Signature header:
 *   X-PCP-Signature: sha256=hex(HMAC-SHA256(requestBody, signingSecret))
 *   Timestamp header:
 *   X-PCP-Timestamp: Unix epoch seconds (for replay-attack prevention)
 *
 * The retry scheduler runs every minute, which means effective retry
 * delays are 1-2min, 5-6min, 30-31min, 2-2h1min.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WebhookDeliveryService {

    private final WebhookEndpointRepository endpointRepo;
    private final WebhookDeliveryRepository deliveryRepo;
    private final ObjectMapper mapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final int MAX_ATTEMPTS = 4;
    private static final int MAX_FAILURES_BEFORE_DISABLE = 10;
    private static final long[] RETRY_DELAYS_SECONDS = { 60, 300, 1800, 7200 };

    /** Called by WebhookEventListener for each Kafka event. */
    @Async
    @Transactional
    public void dispatchToTenant(String tenantId, String eventType,
                                  String eventId, Object payload) {
        List<WebhookEndpoint> endpoints = endpointRepo.findByTenantIdAndActiveTrue(tenantId);
        if (endpoints.isEmpty()) return;

        String payloadJson;
        try {
            payloadJson = mapper.writeValueAsString(Map.of(
                    "eventType", eventType,
                    "eventId",   eventId != null ? eventId : "",
                    "timestamp", System.currentTimeMillis() / 1000L,
                    "tenantId",  tenantId,
                    "data",      payload
            ));
        } catch (Exception e) {
            log.error("[Webhook] Failed to serialize payload for {}/{}: {}", tenantId, eventType, e.getMessage());
            return;
        }

        for (WebhookEndpoint endpoint : endpoints) {
            if (!endpoint.matchesEvent(eventType)) continue;
            WebhookDelivery delivery = deliveryRepo.save(WebhookDelivery.builder()
                    .endpointId(endpoint.getId())
                    .tenantId(tenantId)
                    .eventType(eventType)
                    .eventId(eventId)
                    .payload(payloadJson)
                    .status(DeliveryStatus.PENDING)
                    .build());
            attemptDelivery(delivery, endpoint, payloadJson);
        }
    }

    /** Retry scheduler — every minute, picks up FAILED deliveries that are due. */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void retryFailedDeliveries() {
        List<WebhookDelivery> due = deliveryRepo.findDueForRetry(LocalDateTime.now());
        if (!due.isEmpty()) {
            log.info("[Webhook] Retrying {} failed deliveries", due.size());
            due.forEach(delivery ->
                endpointRepo.findById(delivery.getEndpointId()).ifPresent(endpoint -> {
                    if (endpoint.isActive()) {
                        attemptDelivery(delivery, endpoint, delivery.getPayload());
                    }
                })
            );
        }
    }

    private void attemptDelivery(WebhookDelivery delivery, WebhookEndpoint endpoint, String payloadJson) {
        delivery.setAttemptCount(delivery.getAttemptCount() + 1);
        long tsSeconds = System.currentTimeMillis() / 1000L;

        try {
            String signature = computeSignature(payloadJson, endpoint.getSigningSecret(), tsSeconds);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint.getUrl()))
                    .header("Content-Type", "application/json")
                    .header("X-PCP-Signature", "sha256=" + signature)
                    .header("X-PCP-Timestamp", String.valueOf(tsSeconds))
                    .header("X-PCP-Event", delivery.getEventType())
                    .header("X-PCP-Delivery", delivery.getId())
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(payloadJson))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            int status = resp.statusCode();

            if (status >= 200 && status < 300) {
                delivery.setStatus(DeliveryStatus.DELIVERED);
                delivery.setHttpStatus(status);
                delivery.setDeliveredAt(LocalDateTime.now());
                delivery.setNextRetryAt(null);
                endpoint.setFailureCount(0);
                endpoint.setLastDeliveryAt(LocalDateTime.now());
                endpointRepo.save(endpoint);
                log.info("[Webhook] Delivered {}/{} to {} (attempt {})",
                        delivery.getEventType(), delivery.getId(),
                        endpoint.getUrl(), delivery.getAttemptCount());
            } else {
                handleDeliveryFailure(delivery, endpoint, status,
                        resp.body(), "HTTP " + status);
            }
        } catch (Exception e) {
            handleDeliveryFailure(delivery, endpoint, null, null, e.getMessage());
        }
        deliveryRepo.save(delivery);
    }

    private void handleDeliveryFailure(WebhookDelivery delivery, WebhookEndpoint endpoint,
                                        Integer httpStatus, String responseBody, String reason) {
        delivery.setHttpStatus(httpStatus);
        delivery.setResponseBody(responseBody != null
                ? responseBody.substring(0, Math.min(500, responseBody.length()))
                : null);

        log.warn("[Webhook] Delivery {} failed (attempt {}/{}): {}",
                delivery.getId(), delivery.getAttemptCount(), MAX_ATTEMPTS, reason);

        if (delivery.getAttemptCount() >= MAX_ATTEMPTS) {
            delivery.setStatus(DeliveryStatus.ABANDONED);
            endpointRepo.incrementFailureCount(endpoint.getId());
            if (endpoint.getFailureCount() + 1 >= MAX_FAILURES_BEFORE_DISABLE) {
                endpointRepo.disableEndpoint(endpoint.getId());
                log.warn("[Webhook] Endpoint {} disabled after {} consecutive failures",
                        endpoint.getId(), MAX_FAILURES_BEFORE_DISABLE);
            }
        } else {
            delivery.setStatus(DeliveryStatus.FAILED);
            long delaySecs = RETRY_DELAYS_SECONDS[
                    Math.min(delivery.getAttemptCount() - 1, RETRY_DELAYS_SECONDS.length - 1)];
            delivery.setNextRetryAt(LocalDateTime.now().plusSeconds(delaySecs));
        }
    }

    private String computeSignature(String payload, String secret, long timestamp) throws Exception {
        String input = timestamp + "." + payload;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
    }
}
