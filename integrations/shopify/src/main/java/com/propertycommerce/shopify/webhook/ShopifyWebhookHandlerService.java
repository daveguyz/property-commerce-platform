package com.propertycommerce.shopify.webhook;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service @Slf4j @RequiredArgsConstructor
public class ShopifyWebhookHandlerService {
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void handleOrderCreated(String payload) {
        try {
            JsonNode order = objectMapper.readTree(payload);
            String shopifyOrderId = order.get("id").asText();
            String shopifyCustomerId = order.get("customer") != null
                    ? order.get("customer").get("id").asText() : null;

            // Extract booking metadata from line item properties
            if (order.has("line_items") && order.get("line_items").size() > 0) {
                JsonNode lineItem = order.get("line_items").get(0);
                String bookingId = null;
                if (lineItem.has("properties")) {
                    for (JsonNode prop : lineItem.get("properties")) {
                        if ("booking_id".equals(prop.get("name").asText())) {
                            bookingId = prop.get("value").asText();
                        }
                    }
                }
                log.info("Shopify order created: {} -> booking: {}", shopifyOrderId, bookingId);
                // Publish to booking engine to link order
                kafkaTemplate.send("shopify.order.created",
                        Map.of("shopifyOrderId", shopifyOrderId,
                               "bookingId", bookingId != null ? bookingId : "",
                               "customerId", shopifyCustomerId != null ? shopifyCustomerId : ""));
            }
        } catch (Exception e) {
            log.error("Error handling order created: {}", e.getMessage(), e);
        }
    }

    public void handleOrderCancelled(String payload) {
        try {
            JsonNode order = objectMapper.readTree(payload);
            String shopifyOrderId = order.get("id").asText();
            log.info("Shopify order cancelled: {}", shopifyOrderId);
            kafkaTemplate.send("shopify.order.cancelled",
                    Map.of("shopifyOrderId", shopifyOrderId,
                           "reason", order.has("cancel_reason") ? order.get("cancel_reason").asText() : ""));
        } catch (Exception e) {
            log.error("Error handling order cancelled: {}", e.getMessage(), e);
        }
    }

    public void handleCustomerCreated(String payload) {
        try {
            JsonNode customer = objectMapper.readTree(payload);
            log.info("Shopify customer created: {}", customer.get("id").asText());
            kafkaTemplate.send("shopify.customer.created",
                    Map.of("shopifyCustomerId", customer.get("id").asText(),
                           "email", customer.has("email") ? customer.get("email").asText() : "",
                           "firstName", customer.has("first_name") ? customer.get("first_name").asText() : "",
                           "lastName", customer.has("last_name") ? customer.get("last_name").asText() : ""));
        } catch (Exception e) {
            log.error("Error handling customer created: {}", e.getMessage(), e);
        }
    }

    public void handleCustomerUpdated(String payload) {
        try {
            JsonNode customer = objectMapper.readTree(payload);
            log.info("Shopify customer updated: {}", customer.get("id").asText());
        } catch (Exception e) {
            log.error("Error handling customer updated: {}", e.getMessage(), e);
        }
    }
}
