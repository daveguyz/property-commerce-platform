package com.staysphere.analyticsservice.model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.*;
@Entity @Table(name = "site_metrics",
    indexes = @Index(name = "idx_sm_date_type", columnList = "metric_date, metric_type"))
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SiteMetric {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private String id;
    private LocalDate metricDate;
    private String metricType;
    private Long totalPageViews, uniqueSessions, uniqueUsers;
    private Double bounceRate, avgSessionDurationSeconds;
    private Long totalSearches, searchesWithResults;
    private Long totalBookingsStarted, totalBookingsCompleted;
    private Double bookingConversionRate;
    private Long activeListings, newListings;
    private Long newUsers, returningUsers;
    private Double p95ResponseTimeMs, p99ResponseTimeMs;
    private Double errorRate;
    @CreationTimestamp private LocalDateTime computedAt;
}
