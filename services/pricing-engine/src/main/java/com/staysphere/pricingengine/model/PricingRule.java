package com.staysphere.pricingengine.model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "pricing_rules")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PricingRule {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private String id;
    private String propertyId;
    @Enumerated(EnumType.STRING) private RuleType ruleType;
    private String name;
    private LocalDate startDate, endDate;
    private Integer dayOfWeek;
    private BigDecimal multiplier;
    private BigDecimal fixedPrice;
    private Boolean enabled;
    private Integer priority;
    @CreationTimestamp private LocalDateTime createdAt;

    public enum RuleType { DATE_RANGE, DAY_OF_WEEK, SEASONAL, EVENT, MINIMUM_STAY }
}
