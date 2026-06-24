package com.staysphere.aiservice.service;
import com.staysphere.aiservice.client.PropertySearchClient;
import com.staysphere.aiservice.model.ConversationHistory;
import com.staysphere.aiservice.model.GuestPreference;
import com.staysphere.aiservice.repository.ConversationHistoryRepository;
import com.staysphere.aiservice.repository.GuestPreferenceRepository;
import com.staysphere.shared.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service @Slf4j @RequiredArgsConstructor
public class TravelConciergeService {
    private final ClaudeApiService claudeApiService;
    private final SearchIntentExtractor intentExtractor;
    private final PropertySearchClient propertySearchClient;
    private final ConversationHistoryRepository historyRepository;
    private final GuestPreferenceRepository preferenceRepository;

    private static final String SYSTEM_PROMPT = """
        You are an expert travel concierge for StaySphere, a premium accommodation platform in Namibia and Southern Africa.
        You help guests find the perfect accommodation with warmth, expertise, and local knowledge.
        When presenting properties, highlight what makes each unique. Be conversational but efficient.
        Always reference NAD (Namibian Dollar) for pricing. Know Namibia's regions: Windhoek, Swakopmund, Etosha, Sossusvlei, etc.
        When you recommend properties, number them clearly and explain WHY each suits the guest's needs.
        If no properties match, suggest adjusting criteria and explain alternatives.
        Keep responses concise: 2-4 sentences max per property. Total response under 400 words.
        """;

    @Transactional
    public ConciergeResponse processQuery(String userQuery, String guestId, String sessionId) {
        // 1. Load conversation history for context
        List<ConversationHistory> history = historyRepository.findTop10ByGuestIdOrderByCreatedAtDesc(guestId);

        // 2. Load guest preferences
        Optional<GuestPreference> preferences = preferenceRepository.findByGuestId(guestId);

        // 3. Extract search intent
        SearchRequestDTO searchRequest = intentExtractor.extractFromNaturalLanguage(userQuery);
        searchRequest.setGuestId(guestId);

        // 4. Apply saved preferences as defaults where not specified
        preferences.ifPresent(p -> applyPreferences(searchRequest, p));

        // 5. Fetch matching properties
        List<PropertyDTO> properties = new ArrayList<>();
        try {
            ApiResponse<PagedResponse<PropertyDTO>> searchResult =
                    propertySearchClient.searchProperties(searchRequest, 0, 5);
            if (searchResult.isSuccess() && searchResult.getData() != null)
                properties = searchResult.getData().getContent();
        } catch (Exception e) { log.warn("Property search failed: {}", e.getMessage()); }

        // 6. Build messages with context
        List<Map<String, String>> messages = buildMessages(history, userQuery, properties, searchRequest);

        // 7. Call Claude
        String aiResponse = claudeApiService.chat(SYSTEM_PROMPT, messages, 800);

        // 8. Save to history
        ConversationHistory saved = ConversationHistory.builder()
                .guestId(guestId).sessionId(sessionId).userMessage(userQuery)
                .aiResponse(aiResponse)
                .returnedPropertyIds(properties.stream().map(PropertyDTO::getId)
                        .collect(Collectors.joining(",")))
                .build();
        historyRepository.save(saved);

        // 9. Update preferences
        updateGuestPreferences(guestId, searchRequest);

        return new ConciergeResponse(aiResponse, properties, searchRequest);
    }

    public String compareProperties(List<String> propertyIds, String guestId) {
        List<PropertyDTO> props = new ArrayList<>();
        for (String id : propertyIds) {
            try {
                ApiResponse<PagedResponse<PropertyDTO>> result =
                        propertySearchClient.searchProperties(SearchRequestDTO.builder().build(), 0, 1);
                // In practice this would be a direct getById call
            } catch (Exception ignored) {}
        }

        String prompt = """
            Create a clear, helpful comparison table for these properties for a guest choosing accommodation in Namibia.
            Properties: %s
            Include: price, location, amenities, safety score, ratings, unique selling points.
            Format as a readable markdown table followed by a 2-sentence recommendation.
            """.formatted(buildPropertyContext(props));
        return claudeApiService.singleTurn(prompt, 600);
    }

    public String getSmartCalendarInsights(String propertyId, String checkIn, String checkOut) {
        String prompt = """
            Provide smart insights for booking %s from %s to %s in Namibia.
            Include: price forecast (whether to book now or wait), local events during this period,
            expected weather, typical occupancy levels, and any money-saving tips.
            Keep it to 5 bullet points. Be specific and actionable.
            """.formatted(propertyId, checkIn, checkOut);
        return claudeApiService.singleTurn(prompt, 400);
    }

    public String getAreaIntelligence(double lat, double lon, String city) {
        String prompt = """
            Provide area intelligence for a traveller considering accommodation near %s, Namibia
            (coordinates: %.4f, %.4f).
            Include: safety overview (general, not specific crime stats), nearby attractions,
            restaurant recommendations, transport links, local tips, best time to visit.
            Keep under 300 words. Be friendly and genuinely helpful.
            """.formatted(city, lat, lon);
        return claudeApiService.singleTurn(prompt, 500);
    }

    private List<Map<String, String>> buildMessages(List<ConversationHistory> history,
            String currentQuery, List<PropertyDTO> properties, SearchRequestDTO intent) {
        List<Map<String, String>> messages = new ArrayList<>();

        // Add recent history for context (last 5 exchanges)
        history.stream().limit(5).sorted(Comparator.comparing(ConversationHistory::getCreatedAt))
                .forEach(h -> {
                    messages.add(Map.of("role", "user", "content", h.getUserMessage()));
                    messages.add(Map.of("role", "assistant", "content", h.getAiResponse()));
                });

        // Current query with property context
        String userContent = currentQuery;
        if (!properties.isEmpty()) {
            userContent += "\n\n[Available properties matching your request:]\n" + buildPropertyContext(properties);
        } else {
            userContent += "\n\n[No exact matches found. Please suggest alternatives or ask for adjusted criteria.]";
        }
        messages.add(Map.of("role", "user", "content", userContent));
        return messages;
    }

    private String buildPropertyContext(List<PropertyDTO> properties) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < properties.size(); i++) {
            PropertyDTO p = properties.get(i);
            sb.append(String.format("[%d] %s | %s | %s NAD/night | %d beds | Rating: %.1f (%d reviews) | Trust: %.1f | Pet-friendly: %s | Parking: %s%n",
                    i + 1, p.getTitle(),
                    p.getLocation() != null ? p.getLocation().getCity() : "N/A",
                    p.getPricing() != null && p.getPricing().getCurrentDynamicRate() != null
                            ? p.getPricing().getCurrentDynamicRate() : "N/A",
                    p.getBedrooms() != null ? p.getBedrooms() : 0,
                    p.getAverageRating() != null ? p.getAverageRating() : 0.0,
                    p.getTotalReviews() != null ? p.getTotalReviews() : 0,
                    p.getTrustScore() != null ? p.getTrustScore() : 0.0,
                    Boolean.TRUE.equals(p.getPetFriendly()) ? "Yes" : "No",
                    Boolean.TRUE.equals(p.getHasParking()) ? "Yes" : "No"));
        }
        return sb.toString();
    }

    private void applyPreferences(SearchRequestDTO req, GuestPreference pref) {
        if (req.getCity() == null && pref.getPreferredCities() != null)
            req.setCity(pref.getPreferredCities().split(",")[0]);
        if (req.getBedrooms() == null) req.setBedrooms(pref.getPreferredBedrooms());
        if (req.getMaxPrice() == null) req.setMaxPrice(pref.getPreferredMaxPrice());
        if (req.getPetFriendly() == null) req.setPetFriendly(pref.getPrefersPetFriendly());
        if (req.getHasParking() == null) req.setHasParking(pref.getPrefersParking());
    }

    @Transactional
    void updateGuestPreferences(String guestId, SearchRequestDTO req) {
        GuestPreference pref = preferenceRepository.findByGuestId(guestId)
                .orElse(GuestPreference.builder().guestId(guestId).totalSearches(0).build());
        if (req.getCity() != null) pref.setPreferredCities(req.getCity());
        if (req.getBedrooms() != null) pref.setPreferredBedrooms(req.getBedrooms());
        if (req.getMaxPrice() != null) pref.setPreferredMaxPrice(req.getMaxPrice());
        pref.setTotalSearches(pref.getTotalSearches() == null ? 1 : pref.getTotalSearches() + 1);
        preferenceRepository.save(pref);
    }

    public record ConciergeResponse(String message, List<PropertyDTO> properties, SearchRequestDTO intent) {}
}
