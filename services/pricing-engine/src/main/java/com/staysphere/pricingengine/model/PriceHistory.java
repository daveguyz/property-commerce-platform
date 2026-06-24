package com.staysphere.pricingengine.model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "price_history",
    indexes = { @Index(name = "idx_price_property_date", columnList = "property_id, date") })
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PriceHistory {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private String id;
    @Column(nullable = false) private String propertyId;
    @Column(nullable = false) private LocalDate date;
    private BigDecimal baseRate, dynamicRate, floorRate;
    private Double occupancyMultiplier, eventMultiplier, seasonMultiplier, demandMultiplier;
    private String pricingFactors;
    @CreationTimestamp private LocalDateTime createdAt;
}
