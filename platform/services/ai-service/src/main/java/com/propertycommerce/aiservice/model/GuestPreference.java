package com.propertycommerce.aiservice.model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "guest_preferences")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GuestPreference {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private String id;
    @Column(nullable = false, unique = true) private String guestId;
    private String preferredCities;
    private Integer preferredBedrooms;
    private Integer preferredMinGuests;
    private BigDecimal preferredMaxPrice;
    private Boolean prefersPetFriendly;
    private Boolean prefersParking;
    private Boolean prefersPool;
    private Boolean prefersBeachProximity;
    private String preferredAmenities;
    private String searchHistory;
    private Integer totalSearches;
    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp private LocalDateTime updatedAt;
}
