package com.propertycommerce.analyticsservice.repository;
import com.propertycommerce.analyticsservice.model.HostAnalytics;
import com.propertycommerce.analyticsservice.model.RevenueMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.*;
@Repository
public interface HostAnalyticsRepository extends JpaRepository<HostAnalytics, String> {
    List<HostAnalytics> findByHostIdAndGranularityOrderByPeriodStartDesc(
            String hostId, RevenueMetric.MetricGranularity granularity);
    Optional<HostAnalytics> findByHostIdAndPeriodStartAndGranularity(
            String hostId, LocalDate periodStart, RevenueMetric.MetricGranularity granularity);
}
