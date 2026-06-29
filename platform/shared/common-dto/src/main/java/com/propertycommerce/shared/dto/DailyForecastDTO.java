package com.propertycommerce.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DailyForecastDTO {
    private LocalDate date;
    private Double maxTempCelsius;
    private Double minTempCelsius;
    private String condition;
    private String icon;
    private Double precipitationMm;
}
