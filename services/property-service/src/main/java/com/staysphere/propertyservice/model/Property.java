package com.staysphere.propertyservice.model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.*;
import java.time.LocalDateTime;
import java.util.*;
@Entity
@Table(name = "properties", indexes = {
    @Index(name = "idx_prop_host", columnList = "host_id"),
    @Index(name = "idx_prop_status", columnList = "status"),
    @Index(name = "idx_prop_city", columnList = "city")})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Property {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private String id;
    @Column(unique = true) private String shopifyProductId;
    @Column(name = "host_id", nullable = false) private String hostId;
    @Column(nullable = false) private String title;
    @Column(columnDefinition = "TEXT", nullable = false) private String description;
    @Embedded private PropertyLocation location;
    @Embedded private PricingConfig pricing;
    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default private List<Amenity> amenities = new ArrayList<>();
    @ElementCollection
    @CollectionTable(name = "property_images", joinColumns = @JoinColumn(name = "property_id"))
    @Column(name = "image_url") @Builder.Default private List<String> imageUrls = new ArrayList<>();
    @Column(nullable = false) private Integer maxGuests, bedrooms, bathrooms;
    @Builder.Default private Boolean petFriendly = false, hasParking = false, hasPool = false;
    @Builder.Default private Boolean hasWifi = false, hasKitchen = false, hasAirConditioning = false;
    @Builder.Default private Boolean hasHeating = false, hasWasher = false, hasDryer = false;
    @Builder.Default private Boolean hasTv = false, hasWorkspace = false;
    @Column(nullable = false) @Enumerated(EnumType.STRING)
    @Builder.Default private PropertyStatus status = PropertyStatus.DRAFT;
    @Enumerated(EnumType.STRING) @Builder.Default
    private CancellationPolicy cancellationPolicy = CancellationPolicy.MODERATE;
    @Builder.Default private Integer minNights = 1, maxNights = 365;
    private Double trustScore, averageRating;
    @Builder.Default private Integer totalReviews = 0;
    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp private LocalDateTime updatedAt;
}
