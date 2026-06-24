package com.staysphere.aiservice.controller;
import com.staysphere.aiservice.service.*;
import com.staysphere.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/v1/ai") @RequiredArgsConstructor
public class AiController {
    private final TravelConciergeService conciergeService;

    @PostMapping("/concierge")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TravelConciergeService.ConciergeResponse>> chat(
            @RequestBody Map<String, String> request, @AuthenticationPrincipal String guestId) {
        String query = request.get("query");
        String sessionId = request.getOrDefault("sessionId", UUID.randomUUID().toString());
        if (query == null || query.isBlank())
            return ResponseEntity.badRequest().body(ApiResponse.error("Query cannot be empty"));
        return ResponseEntity.ok(ApiResponse.success(
                conciergeService.processQuery(query, guestId, sessionId)));
    }

    @PostMapping("/concierge/public")
    public ResponseEntity<ApiResponse<TravelConciergeService.ConciergeResponse>> publicChat(
            @RequestBody Map<String, String> request) {
        String query = request.get("query");
        if (query == null || query.isBlank())
            return ResponseEntity.badRequest().body(ApiResponse.error("Query cannot be empty"));
        return ResponseEntity.ok(ApiResponse.success(
                conciergeService.processQuery(query, "anonymous", UUID.randomUUID().toString())));
    }

    @PostMapping("/compare")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> compareProperties(
            @RequestBody Map<String, List<String>> request, @AuthenticationPrincipal String guestId) {
        List<String> propertyIds = request.get("propertyIds");
        if (propertyIds == null || propertyIds.size() < 2)
            return ResponseEntity.badRequest().body(ApiResponse.error("Provide at least 2 property IDs"));
        return ResponseEntity.ok(ApiResponse.success(
                conciergeService.compareProperties(propertyIds, guestId)));
    }

    @GetMapping("/calendar-insights")
    public ResponseEntity<ApiResponse<String>> calendarInsights(
            @RequestParam String propertyId, @RequestParam String checkIn, @RequestParam String checkOut) {
        return ResponseEntity.ok(ApiResponse.success(
                conciergeService.getSmartCalendarInsights(propertyId, checkIn, checkOut)));
    }

    @GetMapping("/area-intelligence")
    public ResponseEntity<ApiResponse<String>> areaIntelligence(
            @RequestParam double lat, @RequestParam double lon, @RequestParam String city) {
        return ResponseEntity.ok(ApiResponse.success(
                conciergeService.getAreaIntelligence(lat, lon, city)));
    }
}
