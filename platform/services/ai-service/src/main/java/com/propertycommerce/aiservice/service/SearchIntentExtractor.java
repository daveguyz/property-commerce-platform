package com.propertycommerce.aiservice.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.propertycommerce.shared.dto.SearchRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Service @Slf4j @RequiredArgsConstructor
public class SearchIntentExtractor {
    private final ClaudeApiService claudeApiService;
    private final ObjectMapper objectMapper;

    public SearchRequestDTO extractFromNaturalLanguage(String userQuery) {
        String prompt = """
            Extract accommodation search parameters from this query and return ONLY valid JSON (no markdown, no explanation):
            Query: "%s"

            JSON schema:
            {
              "city": "string or null",
              "region": "string or null",
              "guests": number or null,
              "bedrooms": number or null,
              "checkIn": "YYYY-MM-DD or null",
              "checkOut": "YYYY-MM-DD or null",
              "maxPrice": number or null,
              "minPrice": number or null,
              "petFriendly": boolean or null,
              "hasParking": boolean or null,
              "hasPool": boolean or null,
              "hasWifi": boolean or null,
              "hasKitchen": boolean or null,
              "sortBy": "averageRating|price|distance or null"
            }
            """.formatted(userQuery);

        try {
            String json = claudeApiService.singleTurn(prompt, 500);
            // Strip any markdown if model adds it anyway
            json = json.replaceAll("```json", "").replaceAll("```", "").trim();
            Map<String, Object> extracted = objectMapper.readValue(json, Map.class);
            return buildSearchRequest(extracted);
        } catch (Exception e) {
            log.warn("Failed to extract intent from '{}': {}", userQuery, e.getMessage());
            return SearchRequestDTO.builder().query(userQuery).build();
        }
    }

    private SearchRequestDTO buildSearchRequest(Map<String, Object> data) {
        SearchRequestDTO req = new SearchRequestDTO();
        req.setCity(getStr(data, "city"));
        req.setRegion(getStr(data, "region"));
        req.setQuery(null);
        if (data.get("guests") != null) req.setGuests(((Number) data.get("guests")).intValue());
        if (data.get("bedrooms") != null) req.setBedrooms(((Number) data.get("bedrooms")).intValue());
        if (data.get("maxPrice") != null) req.setMaxPrice(new BigDecimal(data.get("maxPrice").toString()));
        if (data.get("minPrice") != null) req.setMinPrice(new BigDecimal(data.get("minPrice").toString()));
        if (data.get("checkIn") != null) { try { req.setCheckIn(LocalDate.parse(data.get("checkIn").toString())); } catch (Exception ignored) {} }
        if (data.get("checkOut") != null) { try { req.setCheckOut(LocalDate.parse(data.get("checkOut").toString())); } catch (Exception ignored) {} }
        req.setPetFriendly(getBool(data, "petFriendly"));
        req.setHasParking(getBool(data, "hasParking"));
        req.setHasPool(getBool(data, "hasPool"));
        req.setHasWifi(getBool(data, "hasWifi"));
        req.setHasKitchen(getBool(data, "hasKitchen"));
        req.setSortBy(getStr(data, "sortBy"));
        return req;
    }

    private String getStr(Map<String, Object> m, String k) { Object v = m.get(k); return v instanceof String ? (String) v : null; }
    private Boolean getBool(Map<String, Object> m, String k) { Object v = m.get(k); return v instanceof Boolean ? (Boolean) v : null; }
}
