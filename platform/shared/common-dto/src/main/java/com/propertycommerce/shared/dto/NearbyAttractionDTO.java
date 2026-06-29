package com.propertycommerce.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NearbyAttractionDTO {
    private String name;
    private String type;
    private Double distanceKm;
    private Double rating;
    private String description;
    private String googlePlaceId;
}
