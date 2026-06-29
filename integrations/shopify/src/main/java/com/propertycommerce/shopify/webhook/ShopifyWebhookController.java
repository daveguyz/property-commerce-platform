package com.propertycommerce.shopify.webhook;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@RestController @RequestMapping("/webhook/shopify") @Slf4j @RequiredArgsConstructor
public class ShopifyWebhookController {
    private final ShopifyWebhookHandlerService handlerService;
    @Value("${shopify.webhook-secret}") private String webhookSecret;

    @PostMapping("/orders/create")
    public ResponseEntity<String> orderCreated(
            @RequestHeader("X-Shopify-Hmac-Sha256") String hmac,
            @RequestBody String payload) {
        if (!verifyWebhook(payload, hmac)) return ResponseEntity.status(401).body("Invalid signature");
        handlerService.handleOrderCreated(payload);
        return ResponseEntity.ok("ok");
    }

    @PostMapping("/orders/cancelled")
    public ResponseEntity<String> orderCancelled(
            @RequestHeader("X-Shopify-Hmac-Sha256") String hmac,
            @RequestBody String payload) {
        if (!verifyWebhook(payload, hmac)) return ResponseEntity.status(401).body("Invalid signature");
        handlerService.handleOrderCancelled(payload);
        return ResponseEntity.ok("ok");
    }

    @PostMapping("/customers/create")
    public ResponseEntity<String> customerCreated(
            @RequestHeader("X-Shopify-Hmac-Sha256") String hmac,
            @RequestBody String payload) {
        if (!verifyWebhook(payload, hmac)) return ResponseEntity.status(401).body("Invalid signature");
        handlerService.handleCustomerCreated(payload);
        return ResponseEntity.ok("ok");
    }

    @PostMapping("/customers/update")
    public ResponseEntity<String> customerUpdated(
            @RequestHeader("X-Shopify-Hmac-Sha256") String hmac,
            @RequestBody String payload) {
        if (!verifyWebhook(payload, hmac)) return ResponseEntity.status(401).body("Invalid signature");
        handlerService.handleCustomerUpdated(payload);
        return ResponseEntity.ok("ok");
    }

    private boolean verifyWebhook(String payload, String receivedHmac) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(), "HmacSHA256"));
            String computed = Base64.getEncoder().encodeToString(mac.doFinal(payload.getBytes()));
            return computed.equals(receivedHmac);
        } catch (Exception e) {
            log.error("HMAC verification error: {}", e.getMessage());
            return false;
        }
    }
}
