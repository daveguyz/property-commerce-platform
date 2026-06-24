package com.staysphere.analyticsservice.repository;
import com.staysphere.analyticsservice.model.SiteMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.*;
@Repository
public interface SiteMetricRepository extends JpaRepository<SiteMetric, String> {
    List<SiteMetric> findByMetricDateBetweenOrderByMetricDateAsc(LocalDate from, LocalDate to);
    Optional<SiteMetric> findByMetricDateAndMetricType(LocalDate date, String type);
    List<SiteMetric> findTop30ByOrderByMetricDateDesc();
}
