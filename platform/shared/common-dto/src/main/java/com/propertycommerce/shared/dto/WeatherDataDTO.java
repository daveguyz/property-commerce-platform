package com.propertycommerce.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WeatherDataDTO {
    private Double currentTempCelsius;
    private String currentCondition;
    private String currentIcon;
    private Double humidity;
    private Double windSpeedKmh;
    private List<DailyForecastDTO> forecast;
}
