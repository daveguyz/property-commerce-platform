package com.propertycommerce.propertyservice.model;
import jakarta.persistence.Embeddable;
import lombok.*;
import java.math.BigDecimal;
@Embeddable @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PricingConfig {
    private BigDecimal baseRatePerNight, floorRatePerNight, weeklyDiscount, monthlyDiscount, cleaningFee, securityDeposit, currentDynamicRate;
    private String currency;
    private Boolean dynamicPricingEnabled;
}
