package com.propertycommerce.propertyservice.model;
import jakarta.persistence.Embeddable;
import lombok.*;
@Embeddable @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PropertyLocation {
    private String streetAddress, city, region, country, postalCode, neighborhood, distanceToBeach, distanceToCity;
    private Double latitude, longitude;
}
