package com.staysphere.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SafetyDataDTO {
    private Double safetyScore;
    private String safetyLevel;
    private Double crimeIndex;
    private String neighborhoodDescription;
    private Boolean hasSecurityGuard;
    private Boolean hasAlarmSystem;
    private Boolean hasCCTV;
    private Boolean hasSmokeDetetor;
    private Boolean hasCarbonMonoxideDetector;
    private Boolean hasFireExtinguisher;
    private Boolean hasFirstAidKit;
}
