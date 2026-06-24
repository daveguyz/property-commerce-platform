package com.staysphere.analyticsservice.model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.*;
@Entity @Table(name = "host_analytics",
    indexes = @Index(name = "idx_ha_host_date", columnList = "host_id, period_start"))
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class HostAnalytics {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private String id;
    private String hostId;
    private LocalDate periodStart, periodEnd;
    @Enumerated(EnumType.STRING) private RevenueMetric.MetricGranularity granularity;
    private BigDecimal totalRevenue, averageBookingValue;
    private Integer totalBookings, confirmedBookings, cancelledBookings;
    private Double occupancyRate, averageRating, responseRate;
    private Integer totalReviews, profileViews, searchImpressions, bookingInquiries;
    private Double conversionRate;
    @CreationTimestamp private LocalDateTime computedAt;
}
