package com.propertycommerce.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LocationDTO {
    @NotBlank private String streetAddress;
    @NotBlank private String city;
    @NotBlank private String region;
    @NotBlank private String country;
    private String postalCode;
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")
    private Double latitude;
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
    private Double longitude;
    private String neighborhood;
    private String distanceToBeach;
    private String distanceToCity;
}
