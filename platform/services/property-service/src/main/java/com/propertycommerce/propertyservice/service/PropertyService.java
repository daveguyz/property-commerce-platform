package com.propertycommerce.propertyservice.service;
import com.propertycommerce.propertyservice.exception.PropertyNotFoundException;
import com.propertycommerce.propertyservice.exception.UnauthorizedException;
import com.propertycommerce.propertyservice.mapper.PropertyMapper;
import com.propertycommerce.propertyservice.model.*;
import com.propertycommerce.propertyservice.repository.PropertyRepository;
import com.propertycommerce.shared.dto.*;
import com.propertycommerce.shared.events.PropertyUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.*;
import org.springframework.data.domain.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service @Slf4j @RequiredArgsConstructor @Transactional(readOnly = true)
public class PropertyService {
    private final PropertyRepository propertyRepository;
    private final AvailabilityService availabilityService;
    private final PropertyMapper propertyMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ShopifyProductSyncService shopifySyncService;

    @Cacheable(value = "properties", key = "#id")
    public PropertyDTO getPropertyById(String id) {
        return propertyMapper.toDTO(propertyRepository.findById(id)
                .orElseThrow(() -> new PropertyNotFoundException("Property not found: " + id)));
    }

    public PagedResponse<PropertyDTO> searchProperties(SearchRequestDTO request, Pageable pageable) {
        Page<Property> page = propertyRepository.searchProperties(
                request.getCity(), request.getMinPrice(), request.getMaxPrice(),
                request.getBedrooms(), request.getGuests(), request.getPetFriendly(),
                request.getHasParking(), request.getHasPool(), request.getHasWifi(), pageable);
        return toPagedResponse(page);
    }

    public PagedResponse<PropertyDTO> findNearby(double lat, double lon, double radiusKm, Pageable pageable) {
        return toPagedResponse(propertyRepository.findNearby(lat, lon, radiusKm, pageable));
    }

    public PagedResponse<PropertyDTO> getHostProperties(String hostId, Pageable pageable) {
        return toPagedResponse(propertyRepository.findByHostId(hostId, pageable));
    }

    @Transactional @CacheEvict(value = "properties", key = "#result.id")
    public PropertyDTO createProperty(PropertyDTO dto, String hostId) {
        Property property = propertyMapper.toEntity(dto);
        property.setHostId(hostId);
        property.setStatus(PropertyStatus.PENDING_REVIEW);
        property.setTrustScore(0.0);
        Property saved = propertyRepository.save(property);
        try {
            String shopifyId = shopifySyncService.createProduct(saved);
            saved.setShopifyProductId(shopifyId);
            saved = propertyRepository.save(saved);
        } catch (Exception e) { log.error("Shopify sync failed: {}", e.getMessage()); }
        publishEvent(saved, "CREATED");
        return propertyMapper.toDTO(saved);
    }

    @Transactional @CacheEvict(value = "properties", key = "#id")
    public PropertyDTO updateProperty(String id, PropertyDTO dto, String requestingHostId) {
        Property existing = propertyRepository.findById(id)
                .orElseThrow(() -> new PropertyNotFoundException("Property not found: " + id));
        if (!existing.getHostId().equals(requestingHostId))
            throw new UnauthorizedException("You do not own this property");
        propertyMapper.updateEntityFromDTO(dto, existing);
        Property saved = propertyRepository.save(existing);
        try { shopifySyncService.updateProduct(saved); } catch (Exception e) { log.error("Shopify update failed: {}", e.getMessage()); }
        publishEvent(saved, "UPDATED");
        return propertyMapper.toDTO(saved);
    }

    @Transactional @CacheEvict(value = "properties", key = "#id")
    public void deleteProperty(String id, String requestingHostId) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new PropertyNotFoundException("Property not found: " + id));
        if (!property.getHostId().equals(requestingHostId))
            throw new UnauthorizedException("You do not own this property");
        property.setStatus(PropertyStatus.INACTIVE);
        propertyRepository.save(property);
        publishEvent(property, "DELETED");
    }

    @Transactional
    public void updateRating(String propertyId, Double newRating, Integer reviewCount) {
        Property p = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new PropertyNotFoundException("Property not found: " + propertyId));
        p.setAverageRating(newRating); p.setTotalReviews(reviewCount);
        propertyRepository.save(p);
    }

    @Transactional
    public void updateTrustScore(String propertyId, Double trustScore) {
        Property p = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new PropertyNotFoundException("Property not found: " + propertyId));
        p.setTrustScore(trustScore);
        propertyRepository.save(p);
    }

    private PagedResponse<PropertyDTO> toPagedResponse(Page<Property> page) {
        return PagedResponse.<PropertyDTO>builder()
                .content(page.getContent().stream().map(propertyMapper::toDTO).toList())
                .page(page.getNumber()).size(page.getSize()).totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages()).first(page.isFirst()).last(page.isLast()).build();
    }

    private void publishEvent(Property p, String type) {
        kafkaTemplate.send(PropertyUpdatedEvent.TOPIC, PropertyUpdatedEvent.builder()
                .eventId(UUID.randomUUID().toString()).propertyId(p.getId())
                .hostId(p.getHostId()).shopifyProductId(p.getShopifyProductId())
                .updateType(type).occurredAt(LocalDateTime.now()).build());
    }
}
