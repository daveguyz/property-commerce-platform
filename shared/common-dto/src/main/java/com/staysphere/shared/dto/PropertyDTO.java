package com.staysphere.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PropertyDTO {

    private String id;
    private String shopifyProductId;

    @NotBlank(message = "Host ID is required")
    private String hostId;

    @NotBlank(message = "Property title is required")
    @Size(min = 10, max = 255)
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 50, max = 5000)
    private String description;

    @NotNull
    private LocationDTO location;

    @NotNull
    private PricingConfigDTO pricing;

    private List<AmenityDTO> amenities;
    private List<String> imageUrls;

    @NotNull @Min(1) @Max(50)
    private Integer maxGuests;

    @NotNull @Min(1) @Max(50)
    private Integer bedrooms;

    @NotNull @Min(1) @Max(50)
    private Integer bathrooms;

    private Boolean petFriendly;
    private Boolean hasParking;
    private Boolean hasPool;
    private Boolean hasWifi;
    private Boolean hasKitchen;
    private Boolean hasAirConditioning;
    private Boolean hasHeating;
    private Boolean hasWasher;
    private Boolean hasDryer;
    private Boolean hasTv;
    private Boolean hasWorkspace;

    private Double trustScore;
    private Double averageRating;
    private Integer totalReviews;
    private String status;
    private String cancellationPolicy;
    private Integer minNights;
    private Integer maxNights;

    private SafetyDataDTO safetyData;
    private WeatherDataDTO weatherData;
    private List<NearbyAttractionDTO> nearbyAttractions;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
