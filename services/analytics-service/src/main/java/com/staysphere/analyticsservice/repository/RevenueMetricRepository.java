package com.staysphere.analyticsservice.repository;
import com.staysphere.analyticsservice.model.RevenueMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
@Repository
public interface RevenueMetricRepository extends JpaRepository<RevenueMetric, String> {
    List<RevenueMetric> findByMetricDateBetweenOrderByMetricDateAsc(LocalDate from, LocalDate to);
    List<RevenueMetric> findByHostIdAndMetricDateBetweenOrderByMetricDateAsc(String hostId, LocalDate from, LocalDate to);
    @Query("SELECT SUM(r.platformRevenue) FROM RevenueMetric r WHERE r.metricDate >= :since AND r.granularity = 'DAILY'")
    BigDecimal getTotalPlatformRevenue(@Param("since") LocalDate since);
    @Query("SELECT r.metricDate, SUM(r.grossRevenue) FROM RevenueMetric r WHERE r.metricDate BETWEEN :from AND :to AND r.granularity='DAILY' GROUP BY r.metricDate ORDER BY r.metricDate")
    List<Object[]> getDailyRevenueSeries(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
