package com.staysphere.pricingengine.controller;
import com.staysphere.pricingengine.service.DynamicPricingService;
import com.staysphere.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController @RequestMapping("/api/v1/pricing") @RequiredArgsConstructor
public class PricingController {
    private final DynamicPricingService pricingService;

    @GetMapping("/forecast/{propertyId}")
    public ResponseEntity<ApiResponse<Object>> getPriceForecast(
            @PathVariable String propertyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(pricingService.getPriceForecast(propertyId, from, to)));
    }

    @PostMapping("/recalculate/{propertyId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DynamicPricingService.PricingRecommendation>> recalculate(
            @PathVariable String propertyId) {
        var result = pricingService.recalculatePropertyPrice(
                com.staysphere.shared.dto.PropertyDTO.builder().id(propertyId).build());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
