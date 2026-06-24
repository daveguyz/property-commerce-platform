package com.staysphere.propertyservice.service;
import com.staysphere.propertyservice.model.Property;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service @Slf4j @RequiredArgsConstructor
public class ShopifyProductSyncService {
    private final RestTemplate restTemplate;
    @Value("${shopify.store-url}") private String shopifyStoreUrl;
    @Value("${shopify.access-token}") private String shopifyAccessToken;

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public String createProduct(Property property) {
        HttpHeaders headers = buildHeaders();
        ResponseEntity<Map> response = restTemplate.exchange(
                shopifyStoreUrl + "/admin/api/2024-01/products.json",
                HttpMethod.POST, new HttpEntity<>(Map.of("product", buildProductPayload(property)), headers), Map.class);
        Map<String, Object> created = (Map<String, Object>) response.getBody().get("product");
        String shopifyId = String.valueOf(created.get("id"));
        log.info("Created Shopify product {} for property {}", shopifyId, property.getId());
        return shopifyId;
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void updateProduct(Property property) {
        if (property.getShopifyProductId() == null) return;
        restTemplate.exchange(shopifyStoreUrl + "/admin/api/2024-01/products/" + property.getShopifyProductId() + ".json",
                HttpMethod.PUT, new HttpEntity<>(Map.of("product", buildProductPayload(property)), buildHeaders()), Map.class);
    }

    private Map<String, Object> buildProductPayload(Property property) {
        Map<String, Object> product = new HashMap<>();
        product.put("title", property.getTitle());
        product.put("body_html", property.getDescription());
        product.put("product_type", "Accommodation");
        product.put("status", property.getStatus().name().toLowerCase());
        product.put("tags", buildTags(property));
        Map<String, Object> variant = new HashMap<>();
        variant.put("price", property.getPricing().getCurrentDynamicRate() != null
                ? property.getPricing().getCurrentDynamicRate() : property.getPricing().getBaseRatePerNight());
        variant.put("sku", "STAY-" + property.getId().substring(0, 8).toUpperCase());
        variant.put("requires_shipping", false);
        product.put("variants", List.of(variant));
        product.put("metafields", buildMetafields(property));
        return product;
    }

    private List<Map<String, String>> buildMetafields(Property property) {
        return List.of(
            Map.of("namespace","staysphere","key","property_id","value",property.getId(),"type","single_line_text_field"),
            Map.of("namespace","staysphere","key","bedrooms","value",String.valueOf(property.getBedrooms()),"type","number_integer"),
            Map.of("namespace","staysphere","key","max_guests","value",String.valueOf(property.getMaxGuests()),"type","number_integer"),
            Map.of("namespace","staysphere","key","city","value",property.getLocation().getCity(),"type","single_line_text_field"),
            Map.of("namespace","staysphere","key","latitude","value",String.valueOf(property.getLocation().getLatitude()),"type","single_line_text_field"),
            Map.of("namespace","staysphere","key","longitude","value",String.valueOf(property.getLocation().getLongitude()),"type","single_line_text_field"));
    }

    private String buildTags(Property property) {
        List<String> tags = new ArrayList<>(List.of(property.getLocation().getCity(), property.getBedrooms() + "-bedroom"));
        if (Boolean.TRUE.equals(property.getPetFriendly())) tags.add("pet-friendly");
        if (Boolean.TRUE.equals(property.getHasParking())) tags.add("parking");
        if (Boolean.TRUE.equals(property.getHasPool())) tags.add("pool");
        return String.join(",", tags);
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("X-Shopify-Access-Token", shopifyAccessToken);
        return h;
    }
}
