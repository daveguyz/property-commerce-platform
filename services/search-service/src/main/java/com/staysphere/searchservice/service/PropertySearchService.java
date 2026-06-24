package com.staysphere.searchservice.service;
import com.staysphere.searchservice.model.PropertySearchDocument;
import com.staysphere.searchservice.repository.PropertySearchRepository;
import com.staysphere.shared.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service @Slf4j @RequiredArgsConstructor
public class PropertySearchService {
    private final PropertySearchRepository searchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    public PagedResponse<PropertyDTO> search(SearchRequestDTO request, int page, int size) {
        // Build native ES query with multiple criteria
        Criteria criteria = new Criteria("status").is("ACTIVE");

        if (request.getCity() != null && !request.getCity().isBlank())
            criteria = criteria.and(new Criteria("city").is(request.getCity()));

        if (request.getQuery() != null && !request.getQuery().isBlank())
            criteria = criteria.and(new Criteria("title").contains(request.getQuery())
                    .or(new Criteria("description").contains(request.getQuery())));

        if (request.getMinPrice() != null)
            criteria = criteria.and(new Criteria("currentRate").greaterThanEqual(request.getMinPrice()));

        if (request.getMaxPrice() != null)
            criteria = criteria.and(new Criteria("currentRate").lessThanEqual(request.getMaxPrice()));

        if (request.getBedrooms() != null)
            criteria = criteria.and(new Criteria("bedrooms").greaterThanEqual(request.getBedrooms()));

        if (request.getGuests() != null)
            criteria = criteria.and(new Criteria("maxGuests").greaterThanEqual(request.getGuests()));

        if (Boolean.TRUE.equals(request.getPetFriendly()))
            criteria = criteria.and(new Criteria("petFriendly").is(true));

        if (Boolean.TRUE.equals(request.getHasPool()))
            criteria = criteria.and(new Criteria("hasPool").is(true));

        if (Boolean.TRUE.equals(request.getHasWifi()))
            criteria = criteria.and(new Criteria("hasWifi").is(true));

        CriteriaQuery query = new CriteriaQuery(criteria)
                .setPageable(org.springframework.data.domain.PageRequest.of(page, size));

        SearchHits<PropertySearchDocument> hits = elasticsearchOperations.search(query, PropertySearchDocument.class);

        List<PropertyDTO> content = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PagedResponse.<PropertyDTO>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(hits.getTotalHits())
                .totalPages((int) Math.ceil((double) hits.getTotalHits() / size))
                .first(page == 0)
                .last(content.size() < size)
                .build();
    }

    public void indexProperty(PropertyDTO dto) {
        PropertySearchDocument doc = PropertySearchDocument.builder()
                .id(dto.getId())
                .shopifyProductId(dto.getShopifyProductId())
                .hostId(dto.getHostId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .city(dto.getLocation() != null ? dto.getLocation().getCity() : null)
                .region(dto.getLocation() != null ? dto.getLocation().getRegion() : null)
                .country(dto.getLocation() != null ? dto.getLocation().getCountry() : null)
                .neighborhood(dto.getLocation() != null ? dto.getLocation().getNeighborhood() : null)
                .location(dto.getLocation() != null && dto.getLocation().getLatitude() != null
                        ? new PropertySearchDocument.GeoPoint(dto.getLocation().getLatitude(),
                                dto.getLocation().getLongitude()) : null)
                .baseRate(dto.getPricing() != null ? dto.getPricing().getBaseRatePerNight() : null)
                .currentRate(dto.getPricing() != null ? dto.getPricing().getCurrentDynamicRate() : null)
                .bedrooms(dto.getBedrooms())
                .bathrooms(dto.getBathrooms())
                .maxGuests(dto.getMaxGuests())
                .petFriendly(dto.getPetFriendly())
                .hasParking(dto.getHasParking())
                .hasPool(dto.getHasPool())
                .hasWifi(dto.getHasWifi())
                .hasKitchen(dto.getHasKitchen())
                .hasAirConditioning(dto.getHasAirConditioning())
                .hasWorkspace(dto.getHasWorkspace())
                .status(dto.getStatus())
                .averageRating(dto.getAverageRating())
                .totalReviews(dto.getTotalReviews())
                .trustScore(dto.getTrustScore())
                .imageUrls(dto.getImageUrls())
                .indexedAt(java.time.LocalDateTime.now())
                .build();
        searchRepository.save(doc);
        log.info("Indexed property {}", dto.getId());
    }

    public void removeProperty(String propertyId) {
        searchRepository.deleteById(propertyId);
    }

    private PropertyDTO toDTO(PropertySearchDocument doc) {
        LocationDTO location = doc.getCity() != null ? LocationDTO.builder()
                .city(doc.getCity()).region(doc.getRegion()).country(doc.getCountry())
                .neighborhood(doc.getNeighborhood())
                .latitude(doc.getLocation() != null ? doc.getLocation().getLat() : null)
                .longitude(doc.getLocation() != null ? doc.getLocation().getLon() : null)
                .build() : null;

        PricingConfigDTO pricing = PricingConfigDTO.builder()
                .baseRatePerNight(doc.getBaseRate())
                .currentDynamicRate(doc.getCurrentRate())
                .build();

        return PropertyDTO.builder()
                .id(doc.getId()).title(doc.getTitle()).description(doc.getDescription())
                .hostId(doc.getHostId()).location(location).pricing(pricing)
                .bedrooms(doc.getBedrooms()).bathrooms(doc.getBathrooms())
                .maxGuests(doc.getMaxGuests()).petFriendly(doc.getPetFriendly())
                .hasParking(doc.getHasParking()).hasPool(doc.getHasPool())
                .hasWifi(doc.getHasWifi()).hasKitchen(doc.getHasKitchen())
                .averageRating(doc.getAverageRating()).totalReviews(doc.getTotalReviews())
                .trustScore(doc.getTrustScore()).imageUrls(doc.getImageUrls())
                .status(doc.getStatus()).build();
    }
}
