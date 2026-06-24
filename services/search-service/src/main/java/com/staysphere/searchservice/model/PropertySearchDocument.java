package com.staysphere.searchservice.model;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Document(indexName = "properties", createIndex = true)
@Setting(settingPath = "/elasticsearch/settings.json")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PropertySearchDocument {
    @Id private String id;
    private String shopifyProductId, hostId;

    @MultiField(mainField = @Field(type = FieldType.Text, analyzer = "standard"),
                otherFields = { @InnerField(suffix = "keyword", type = FieldType.Keyword) })
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Keyword) private String city;
    @Field(type = FieldType.Keyword) private String region;
    @Field(type = FieldType.Keyword) private String country;
    @Field(type = FieldType.Keyword) private String neighborhood;

    @GeoPointField private GeoPoint location;

    @Field(type = FieldType.Scaled_Float, scalingFactor = 100) private BigDecimal baseRate;
    @Field(type = FieldType.Scaled_Float, scalingFactor = 100) private BigDecimal currentRate;

    private Integer bedrooms, bathrooms, maxGuests;
    private Boolean petFriendly, hasParking, hasPool, hasWifi, hasKitchen;
    private Boolean hasAirConditioning, hasWorkspace;

    @Field(type = FieldType.Keyword) private String status;
    @Field(type = FieldType.Keyword) private String cancellationPolicy;

    @Field(type = FieldType.Scaled_Float, scalingFactor = 100) private Double averageRating;
    private Integer totalReviews;
    private Double trustScore;
    private List<String> imageUrls;
    private List<String> amenities;
    private List<String> tags;

    @Field(type = FieldType.Date) private LocalDateTime indexedAt;

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class GeoPoint {
        private Double lat, lon;
    }
}
