package com.staysphere.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SearchRequestDTO {
    private String query;
    private String city;
    private String region;
    private Double latitude;
    private Double longitude;
    private Double radiusKm;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private Integer guests;
    private Integer bedrooms;
    private Integer bathrooms;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Boolean petFriendly;
    private Boolean hasParking;
    private Boolean hasPool;
    private Boolean hasWifi;
    private Boolean hasKitchen;
    private List<String> amenities;
    private String sortBy;
    private String sortDirection;
    private Integer page;
    private Integer size;
    private String guestId;
}
