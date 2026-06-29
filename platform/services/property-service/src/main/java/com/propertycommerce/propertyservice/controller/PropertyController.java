package com.propertycommerce.propertyservice.controller;

import com.propertycommerce.propertyservice.service.AvailabilityService;
import com.propertycommerce.propertyservice.service.PropertyService;
import com.propertycommerce.shared.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;
    private final AvailabilityService availabilityService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PropertyDTO>> getProperty(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(propertyService.getPropertyById(id)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PagedResponse<PropertyDTO>>> search(
            @ModelAttribute SearchRequestDTO request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "averageRating") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("DESC") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        return ResponseEntity.ok(ApiResponse.success(propertyService.searchProperties(request, PageRequest.of(page, size, sort))));
    }

    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<PagedResponse<PropertyDTO>>> findNearby(
            @RequestParam double lat, @RequestParam double lon,
            @RequestParam(defaultValue = "10.0") double radiusKm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(propertyService.findNearby(lat, lon, radiusKm, PageRequest.of(page, size))));
    }

    @GetMapping("/{id}/availability")
    public ResponseEntity<ApiResponse<Map<LocalDate, String>>> getAvailability(
            @PathVariable String id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(availabilityService.getAvailabilityCalendar(id, from, to)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HOST', 'ADMIN')")
    public ResponseEntity<ApiResponse<PropertyDTO>> createProperty(
            @Valid @RequestBody PropertyDTO dto,
            @AuthenticationPrincipal String hostId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(propertyService.createProperty(dto, hostId), "Property created"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HOST', 'ADMIN')")
    public ResponseEntity<ApiResponse<PropertyDTO>> updateProperty(
            @PathVariable String id, @Valid @RequestBody PropertyDTO dto,
            @AuthenticationPrincipal String hostId) {
        return ResponseEntity.ok(ApiResponse.success(propertyService.updateProperty(id, dto, hostId)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('HOST', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProperty(
            @PathVariable String id, @AuthenticationPrincipal String hostId) {
        propertyService.deleteProperty(id, hostId);
        return ResponseEntity.ok(ApiResponse.success(null, "Property deactivated"));
    }

    @GetMapping("/host/{hostId}")
    @PreAuthorize("hasAnyRole('HOST', 'ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<PropertyDTO>>> getHostProperties(
            @PathVariable String hostId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(propertyService.getHostProperties(hostId, PageRequest.of(page, size))));
    }
}
