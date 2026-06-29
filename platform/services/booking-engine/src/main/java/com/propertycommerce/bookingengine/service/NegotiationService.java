package com.propertycommerce.bookingengine.service;
import com.propertycommerce.bookingengine.client.PropertyServiceClient;
import com.propertycommerce.bookingengine.model.BookingNegotiation;
import com.propertycommerce.bookingengine.repository.BookingNegotiationRepository;
import com.propertycommerce.shared.dto.PropertyDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service @Slf4j @RequiredArgsConstructor
public class NegotiationService {
    private final BookingNegotiationRepository negotiationRepository;
    private final PropertyServiceClient propertyServiceClient;
    private final RestTemplate restTemplate;
    @Value("${anthropic.api.key}") private String anthropicApiKey;

    @Transactional
    public BookingNegotiation initiateNegotiation(String propertyId, String guestId,
            BigDecimal offeredPrice, String guestMessage, java.time.LocalDate checkIn,
            java.time.LocalDate checkOut, int guestCount) {

        var propertyResponse = propertyServiceClient.getProperty(propertyId);
        if (!propertyResponse.isSuccess()) throw new RuntimeException("Property not found");
        PropertyDTO property = propertyResponse.getData();

        // Floor price check — no deal below host floor
        BigDecimal floorPrice = property.getPricing().getFloorRatePerNight();
        if (floorPrice != null && offeredPrice.compareTo(floorPrice) < 0)
            throw new IllegalArgumentException("Offer is below minimum allowed price");

        String aiSuggestion = getAiNegotiationAdvice(property, offeredPrice, guestMessage);

        BookingNegotiation negotiation = BookingNegotiation.builder()
                .propertyId(propertyId).guestId(guestId).hostId(property.getHostId())
                .checkIn(checkIn).checkOut(checkOut).guestCount(guestCount)
                .originalPrice(property.getPricing().getBaseRatePerNight())
                .offeredPrice(offeredPrice).guestMessage(guestMessage)
                .aiSuggestion(aiSuggestion).status(BookingNegotiation.NegotiationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusHours(48)).build();

        return negotiationRepository.save(negotiation);
    }

    @Transactional
    public BookingNegotiation respondToNegotiation(String negotiationId, String hostId,
            boolean accepted, BigDecimal counterPrice, String hostResponse) {
        BookingNegotiation negotiation = negotiationRepository.findById(negotiationId)
                .orElseThrow(() -> new RuntimeException("Negotiation not found"));
        if (!negotiation.getHostId().equals(hostId)) throw new RuntimeException("Not authorized");
        if (negotiation.getStatus() != BookingNegotiation.NegotiationStatus.PENDING)
            throw new IllegalStateException("Negotiation is no longer pending");

        negotiation.setHostResponse(hostResponse);
        negotiation.setRespondedAt(LocalDateTime.now());

        if (accepted) {
            negotiation.setStatus(BookingNegotiation.NegotiationStatus.ACCEPTED);
        } else if (counterPrice != null) {
            negotiation.setCounterPrice(counterPrice);
            negotiation.setStatus(BookingNegotiation.NegotiationStatus.COUNTERED);
        } else {
            negotiation.setStatus(BookingNegotiation.NegotiationStatus.REJECTED);
        }
        return negotiationRepository.save(negotiation);
    }

    private String getAiNegotiationAdvice(PropertyDTO property, BigDecimal offeredPrice, String message) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-api-key", anthropicApiKey);
            headers.set("anthropic-version", "2023-06-01");
            headers.setContentType(MediaType.APPLICATION_JSON);

            String prompt = String.format("""
                You are a negotiation advisor for a property rental platform.
                Property: %s in %s
                Base price: %s NAD/night
                Guest offer: %s NAD/night
                Guest message: %s
                Provide a brief, professional suggestion for the host on whether to accept, counter, or decline.
                Keep it under 100 words.
                """, property.getTitle(), property.getLocation().getCity(),
                    property.getPricing().getBaseRatePerNight(), offeredPrice, message);

            Map<String, Object> body = Map.of("model", "claude-sonnet-4-6", "max_tokens", 200,
                    "messages", List.of(Map.of("role", "user", "content", prompt)));
            ResponseEntity<Map> response = restTemplate.exchange("https://api.anthropic.com/v1/messages",
                    HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
            List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().get("content");
            return content != null && !content.isEmpty() ? (String) content.get(0).get("text") : "";
        } catch (Exception e) {
            log.error("AI negotiation advice failed: {}", e.getMessage());
            return "";
        }
    }
}
