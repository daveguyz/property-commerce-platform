package com.propertycommerce.shopify.client;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service @Slf4j @RequiredArgsConstructor
public class ShopifyStorefrontClient {
    private final RestTemplate restTemplate;
    @Value("${shopify.store-url}") private String storeUrl;
    @Value("${shopify.storefront-token}") private String storefrontToken;
    @Value("${shopify.access-token}") private String adminToken;

    public Map<String, Object> createDraftOrder(String bookingId, String variantId,
            String customerEmail, double amount, String currency) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Shopify-Access-Token", adminToken);

        Map<String, Object> lineItem = new HashMap<>();
        lineItem.put("variant_id", variantId);
        lineItem.put("quantity", 1);
        lineItem.put("price", String.valueOf(amount));
        lineItem.put("properties", List.of(Map.of("name", "booking_id", "value", bookingId)));

        Map<String, Object> draftOrder = new HashMap<>();
        draftOrder.put("line_items", List.of(lineItem));
        draftOrder.put("currency", currency);
        if (customerEmail != null) {
            Map<String, String> customer = new HashMap<>();
            customer.put("email", customerEmail);
            draftOrder.put("customer", customer);
        }

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    storeUrl + "/admin/api/2024-01/draft_orders.json",
                    HttpMethod.POST, new HttpEntity<>(Map.of("draft_order", draftOrder), headers), Map.class);
            return (Map<String, Object>) response.getBody().get("draft_order");
        } catch (Exception e) {
            log.error("Shopify draft order creation failed: {}", e.getMessage());
            throw new RuntimeException("Shopify order creation failed", e);
        }
    }

    public void updateProductInventory(String variantId, int quantity) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Shopify-Access-Token", adminToken);

        Map<String, Object> inventoryItem = Map.of(
                "inventory_item_id", variantId, "available", quantity);
        try {
            restTemplate.exchange(storeUrl + "/admin/api/2024-01/inventory_levels/set.json",
                    HttpMethod.POST, new HttpEntity<>(Map.of("inventory_level", inventoryItem), headers), Map.class);
        } catch (Exception e) {
            log.error("Inventory update failed: {}", e.getMessage());
        }
    }
}
