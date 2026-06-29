package com.propertycommerce.analyticsservice.service;

import com.propertycommerce.analyticsservice.model.*;
import com.propertycommerce.analyticsservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service @Slf4j @RequiredArgsConstructor
public class AnalyticsService {

    private final PageViewRepository pageViewRepository;
    private final RevenueMetricRepository revenueMetricRepository;
    private final HostAnalyticsRepository hostAnalyticsRepository;
    private final SiteMetricRepository siteMetricRepository;
    private final StringRedisTemplate redisTemplate;   // String,String — no generics mismatch

    @Transactional
    public void recordPageView(String sessionId, String userId, String pageType, String pageId,
            String pageTitle, String referrer, String userAgent, String ipAddress,
            String country, String deviceType) {
        pageViewRepository.save(PageView.builder()
                .sessionId(sessionId).userId(userId).pageType(pageType)
                .pageId(pageId).pageTitle(pageTitle).referrer(referrer)
                .userAgent(userAgent).ipAddress(ipAddress).country(country)
                .deviceType(deviceType).build());
        String dateKey = LocalDate.now().toString();
        redisTemplate.opsForValue().increment("analytics:pageviews:" + dateKey);
        redisTemplate.opsForHyperLogLog().add("analytics:sessions:" + dateKey, sessionId);
        if (userId != null) {
            redisTemplate.opsForHyperLogLog().add("analytics:users:" + dateKey, userId);
        }
    }

    public AdminDashboard getAdminDashboard(LocalDate from, LocalDate to) {
        LocalDateTime fromDt = from.atStartOfDay();

        String todayKey = LocalDate.now().toString();
        Long todayViews = getRedisCount("analytics:pageviews:" + todayKey);
        Long todaySessions = pageViewRepository.countUniqueSessions(LocalDate.now().atStartOfDay());

        BigDecimal totalRevenue = revenueMetricRepository.getTotalPlatformRevenue(from);
        List<Object[]> dailyRevenueRows = revenueMetricRepository.getDailyRevenueSeries(from, to);

        List<Object[]> pageBreakdown = pageViewRepository.getPageViewsByType(fromDt);
        Map<String, Long> pagesByType = new LinkedHashMap<>();
        for (Object[] row : pageBreakdown) pagesByType.put((String) row[0], (Long) row[1]);

        List<Object[]> geoData = pageViewRepository.getViewsByCountry(fromDt);
        Map<String, Long> byCountry = new LinkedHashMap<>();
        for (Object[] row : geoData) byCountry.put((String) row[0], (Long) row[1]);

        List<Object[]> deviceData = pageViewRepository.getViewsByDevice(fromDt);
        Map<String, Long> byDevice = new LinkedHashMap<>();
        for (Object[] row : deviceData) byDevice.put((String) row[0], (Long) row[1]);

        List<SiteMetric> trend = siteMetricRepository.findByMetricDateBetweenOrderByMetricDateAsc(from, to);

        List<Map<String, Object>> revenueSeries = new ArrayList<>();
        for (Object[] row : dailyRevenueRows) {
            revenueSeries.add(Map.of("date", row[0].toString(),
                    "revenue", row[1] != null ? row[1] : BigDecimal.ZERO));
        }

        return new AdminDashboard(
                todayViews, todaySessions,
                totalRevenue != null ? totalRevenue : BigDecimal.ZERO,
                pagesByType, byCountry, byDevice, trend, revenueSeries,
                Map.of("totalPageViews", pageViewRepository.countByViewedAtAfter(fromDt),
                       "uniqueSessions", todaySessions)
        );
    }

    public HostDashboard getHostDashboard(String hostId, LocalDate from, LocalDate to) {
        List<HostAnalytics> monthly = hostAnalyticsRepository
                .findByHostIdAndGranularityOrderByPeriodStartDesc(
                        hostId, RevenueMetric.MetricGranularity.MONTHLY);
        List<RevenueMetric> daily = revenueMetricRepository
                .findByHostIdAndMetricDateBetweenOrderByMetricDateAsc(hostId, from, to);
        return new HostDashboard(hostId, monthly, daily, from, to);
    }

    @Transactional
    public void recordBookingMetric(String propertyId, String hostId, BigDecimal amount,
            BigDecimal platformFee, BigDecimal hostRevenue, boolean cancelled) {
        LocalDate today = LocalDate.now();
        RevenueMetric metric = revenueMetricRepository.findAll().stream()
                .filter(m -> today.equals(m.getMetricDate()) && propertyId.equals(m.getPropertyId()))
                .findFirst()
                .orElse(RevenueMetric.builder()
                        .metricDate(today).propertyId(propertyId).hostId(hostId)
                        .granularity(RevenueMetric.MetricGranularity.DAILY)
                        .grossRevenue(BigDecimal.ZERO).platformRevenue(BigDecimal.ZERO)
                        .hostRevenue(BigDecimal.ZERO).bookingCount(0).cancelledCount(0).build());

        if (!cancelled) {
            metric.setGrossRevenue(metric.getGrossRevenue().add(amount));
            metric.setPlatformRevenue(metric.getPlatformRevenue().add(platformFee));
            metric.setHostRevenue(metric.getHostRevenue().add(hostRevenue));
            metric.setBookingCount(metric.getBookingCount() + 1);
        } else {
            metric.setCancelledCount(metric.getCancelledCount() + 1);
        }
        revenueMetricRepository.save(metric);
    }

    @Scheduled(cron = "0 5 0 * * *")
    @Transactional
    public void computeDailyMetrics() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime from = yesterday.atStartOfDay();

        long totalViews = pageViewRepository.countByViewedAtAfter(from);
        long uniqueSessions = pageViewRepository.countUniqueSessions(from);
        long uniqueUsers = pageViewRepository.countUniqueUsers(from);

        SiteMetric metric = siteMetricRepository
                .findByMetricDateAndMetricType(yesterday, "DAILY")
                .orElse(SiteMetric.builder().metricDate(yesterday).metricType("DAILY").build());
        metric.setTotalPageViews(totalViews);
        metric.setUniqueSessions(uniqueSessions);
        metric.setUniqueUsers(uniqueUsers);
        siteMetricRepository.save(metric);
        log.info("Daily metrics computed for {}: {} views, {} sessions", yesterday, totalViews, uniqueSessions);
    }

    private Long getRedisCount(String key) {
        String val = redisTemplate.opsForValue().get(key);
        return val != null ? Long.parseLong(val) : 0L;
    }

    public record AdminDashboard(
            Long todayPageViews, Long todayUniqueSessions, BigDecimal totalRevenue,
            Map<String, Long> pagesByType, Map<String, Long> visitorsByCountry,
            Map<String, Long> visitorsByDevice, List<SiteMetric> metricsTrend,
            List<Map<String, Object>> revenueSeries, Map<String, Object> topMetrics) {}

    public record HostDashboard(
            String hostId, List<HostAnalytics> monthlyStats,
            List<RevenueMetric> dailyRevenue, LocalDate from, LocalDate to) {}
}
