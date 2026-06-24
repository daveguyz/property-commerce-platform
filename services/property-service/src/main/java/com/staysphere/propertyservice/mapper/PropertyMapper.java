package com.staysphere.propertyservice.mapper;

import com.staysphere.propertyservice.model.*;
import com.staysphere.shared.dto.*;
import org.mapstruct.*;
import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PropertyMapper {

    @Mapping(target = "location", expression = "java(locationToDTO(property.getLocation()))")
    @Mapping(target = "pricing", expression = "java(pricingToDTO(property.getPricing()))")
    @Mapping(target = "amenities", expression = "java(amenityListToDTO(property.getAmenities()))")
    PropertyDTO toDTO(Property property);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "shopifyProductId", ignore = true)
    @Mapping(target = "hostId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "trustScore", ignore = true)
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "totalReviews", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "safetyData", ignore = true)
    @Mapping(target = "weatherData", ignore = true)
    @Mapping(target = "nearbyAttractions", ignore = true)
    Property toEntity(PropertyDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "shopifyProductId", ignore = true)
    @Mapping(target = "hostId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "trustScore", ignore = true)
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "totalReviews", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDTO(PropertyDTO dto, @MappingTarget Property property);

    default List<AmenityDTO> amenityListToDTO(List<Amenity> amenities) {
        if (amenities == null) return null;
        return amenities.stream().map(a -> AmenityDTO.builder()
                .id(a.getId()).name(a.getName()).category(a.getCategory())
                .icon(a.getIcon()).detail(a.getDetail()).build()).toList();
    }

    default LocationDTO locationToDTO(PropertyLocation l) {
        if (l == null) return null;
        return LocationDTO.builder().streetAddress(l.getStreetAddress()).city(l.getCity())
                .region(l.getRegion()).country(l.getCountry()).postalCode(l.getPostalCode())
                .latitude(l.getLatitude()).longitude(l.getLongitude())
                .neighborhood(l.getNeighborhood()).distanceToBeach(l.getDistanceToBeach())
                .distanceToCity(l.getDistanceToCity()).build();
    }

    default PricingConfigDTO pricingToDTO(PricingConfig p) {
        if (p == null) return null;
        return PricingConfigDTO.builder().baseRatePerNight(p.getBaseRatePerNight())
                .floorRatePerNight(p.getFloorRatePerNight()).weeklyDiscount(p.getWeeklyDiscount())
                .monthlyDiscount(p.getMonthlyDiscount()).cleaningFee(p.getCleaningFee())
                .securityDeposit(p.getSecurityDeposit()).currency(p.getCurrency())
                .dynamicPricingEnabled(p.getDynamicPricingEnabled())
                .currentDynamicRate(p.getCurrentDynamicRate()).build();
    }
}
