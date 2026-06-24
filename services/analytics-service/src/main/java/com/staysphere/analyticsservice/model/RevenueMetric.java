package com.staysphere.analyticsservice.model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.*;
@Entity @Table(name = "revenue_metrics",
    indexes = { @Index(name = "idx_rm_date", columnList = "metric_date"),
                @Index(name = "idx_rm_property", columnList = "property_id") })
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RevenueMetric {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private String id;
    private LocalDate metricDate;
    private String propertyId, hostId;
    @Enumerated(EnumType.STRING) private MetricGranularity granularity;
    private BigDecimal grossRevenue, platformRevenue, hostRevenue;
    private Integer bookingCount, cancelledCount;
    private Double occupancyRate, averageNightlyRate;
    @CreationTimestamp private LocalDateTime createdAt;
    public enum MetricGranularity { DAILY, WEEKLY, MONTHLY }
}
