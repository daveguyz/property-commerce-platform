package com.staysphere.searchservice.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(indexName = "auction_lots", createIndex = true)
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AuctionSearchDocument {

    @Id
    private String id;

    // Core identity
    private String propertyId;
    private String sellerId;

    @MultiField(mainField = @Field(type = FieldType.Text, analyzer = "standard"),
                otherFields = { @InnerField(suffix = "keyword", type = FieldType.Keyword) })
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    // Location (mirrors PropertySearchDocument)
    @Field(type = FieldType.Keyword) private String city;
    @Field(type = FieldType.Keyword) private String region;
    @Field(type = FieldType.Keyword) private String country;

    @GeoPointField
    private PropertySearchDocument.GeoPoint location;

    // Auction type and status
    @Field(type = FieldType.Keyword) private String auctionType;    // ENGLISH, DUTCH, REVERSE, SEALED_BID
    @Field(type = FieldType.Keyword) private String status;          // SCHEDULED, OPEN, EXTENDED, CLOSED, SETTLED

    // Pricing
    @Field(type = FieldType.Scaled_Float, scalingFactor = 100) private BigDecimal startingPrice;
    @Field(type = FieldType.Scaled_Float, scalingFactor = 100) private BigDecimal reservePrice;
    @Field(type = FieldType.Scaled_Float, scalingFactor = 100) private BigDecimal currentBidAmount;
    @Field(type = FieldType.Scaled_Float, scalingFactor = 100) private BigDecimal winningAmount;
    @Field(type = FieldType.Keyword)                           private String currency;

    // Timing
    @Field(type = FieldType.Date) private LocalDateTime startsAt;
    @Field(type = FieldType.Date) private LocalDateTime scheduledEndsAt;
    @Field(type = FieldType.Date) private LocalDateTime actualEndsAt;

    // Activity metrics
    private Integer totalBids;
    private Integer uniqueBidders;

    // Feature flags
    private Boolean depositRequired;
    private Boolean kycRequired;
    private Boolean livestreamActive;
    private String livestreamProvider;

    // Media
    private String firstImageUrl;

    @Field(type = FieldType.Date) private LocalDateTime indexedAt;
}
