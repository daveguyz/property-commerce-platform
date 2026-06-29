package com.propertycommerce.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PricingConfigDTO {
    @NotNull @DecimalMin("1.0") private BigDecimal baseRatePerNight;
    @NotNull @DecimalMin("1.0") private BigDecimal floorRatePerNight;
    private BigDecimal weeklyDiscount;
    private BigDecimal monthlyDiscount;
    private BigDecimal cleaningFee;
    private BigDecimal securityDeposit;
    private String currency;
    private Boolean dynamicPricingEnabled;
    private BigDecimal currentDynamicRate;
}
