package com.staysphere.propertyservice.repository;
import com.staysphere.propertyservice.model.Property;
import com.staysphere.propertyservice.model.PropertyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.*;

@Repository
public interface PropertyRepository extends JpaRepository<Property, String> {
    Page<Property> findByStatus(PropertyStatus status, Pageable pageable);
    Page<Property> findByHostId(String hostId, Pageable pageable);
    Optional<Property> findByShopifyProductId(String shopifyProductId);
    List<Property> findByHostIdAndStatus(String hostId, PropertyStatus status);

    @Query("""
        SELECT p FROM Property p WHERE p.status = 'ACTIVE'
        AND (:city IS NULL OR LOWER(p.location.city) LIKE LOWER(CONCAT('%',:city,'%')))
        AND (:minPrice IS NULL OR p.pricing.currentDynamicRate >= :minPrice)
        AND (:maxPrice IS NULL OR p.pricing.currentDynamicRate <= :maxPrice)
        AND (:bedrooms IS NULL OR p.bedrooms >= :bedrooms)
        AND (:maxGuests IS NULL OR p.maxGuests >= :maxGuests)
        AND (:petFriendly IS NULL OR p.petFriendly = :petFriendly)
        AND (:hasParking IS NULL OR p.hasParking = :hasParking)
        AND (:hasPool IS NULL OR p.hasPool = :hasPool)
        AND (:hasWifi IS NULL OR p.hasWifi = :hasWifi)
        ORDER BY p.averageRating DESC NULLS LAST, p.totalReviews DESC
        """)
    Page<Property> searchProperties(
            @Param("city") String city, @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice, @Param("bedrooms") Integer bedrooms,
            @Param("maxGuests") Integer maxGuests, @Param("petFriendly") Boolean petFriendly,
            @Param("hasParking") Boolean hasParking, @Param("hasPool") Boolean hasPool,
            @Param("hasWifi") Boolean hasWifi, Pageable pageable);

    @Query("""
        SELECT p FROM Property p WHERE p.status = 'ACTIVE'
        AND (6371 * acos(cos(radians(:lat)) * cos(radians(p.location.latitude))
             * cos(radians(p.location.longitude) - radians(:lon))
             + sin(radians(:lat)) * sin(radians(p.location.latitude)))) <= :radiusKm
        ORDER BY (6371 * acos(cos(radians(:lat)) * cos(radians(p.location.latitude))
             * cos(radians(p.location.longitude) - radians(:lon))
             + sin(radians(:lat)) * sin(radians(p.location.latitude)))) ASC
        """)
    Page<Property> findNearby(@Param("lat") double lat, @Param("lon") double lon,
                               @Param("radiusKm") double radiusKm, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Property p WHERE p.hostId = :hostId AND p.status = 'ACTIVE'")
    long countActiveByHost(@Param("hostId") String hostId);
}
