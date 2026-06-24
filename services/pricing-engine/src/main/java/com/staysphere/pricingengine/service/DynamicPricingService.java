package com.staysphere.pricingengine.service;
import com.staysphere.pricingengine.client.PropertyClient;
import com.staysphere.pricingengine.model.PriceHistory;
import com.staysphere.pricingengine.model.PricingRule;
import com.staysphere.pricingengine.repository.PriceHistoryRepository;
import com.staysphere.pricingengine.repository.PricingRuleRepository;
import com.staysphere.shared.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;

@Service @Slf4j @RequiredArgsConstructor
public class DynamicPricingService {
    private final PropertyClient propertyClient;
    private final PriceHistoryRepository priceHistoryRepository;
    private final PricingRuleRepository pricingRuleRepository;

    /** Runs every hour to recalculate all active property prices */
    @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void recalculateAllPrices() {
        log.info("Starting dynamic pricing recalculation cycle");
        try {
            ApiResponse<PagedResponse<PropertyDTO>> response = propertyClient.getAllActive(0, 200);
            if (!response.isSuccess() || response.getData() == null) return;

            List<PropertyDTO> properties = response.getData().getContent();
            properties.parallelStream().forEach(this::recalculatePropertyPrice);
            log.info("Pricing cycle complete for {} properties", properties.size());
        } catch (Exception e) {
            log.error("Pricing cycle failed: {}", e.getMessage());
        }
    }

    @Transactional
    public PricingRecommendation recalculatePropertyPrice(PropertyDTO property) {
        if (property.getPricing() == null || property.getPricing().getBaseRatePerNight() == null) return null;

        BigDecimal baseRate = property.getPricing().getBaseRatePerNight();
        BigDecimal floorRate = property.getPricing().getFloorRatePerNight() != null
                ? property.getPricing().getFloorRatePerNight() : baseRate.multiply(BigDecimal.valueOf(0.7));

        // Factor 1: Demand/Occupancy (from recent booking data) — 0.8 to 1.4
        double occupancyMultiplier = calculateOccupancyMultiplier(property.getId());

        // Factor 2: Day of week (weekends +15%) — 1.0 to 1.15
        double dayOfWeekMultiplier = getDayOfWeekMultiplier();

        // Factor 3: Seasonal (Namibia peak: June-October) — 0.9 to 1.3
        double seasonMultiplier = getNamibiaSeasonMultiplier();

        // Factor 4: Lead time (last-minute discounts, far-future premium) — 0.85 to 1.1
        double leadTimeMultiplier = getLeadTimeMultiplier();

        // Factor 5: Custom rules for this property
        double customRuleMultiplier = getCustomRuleMultiplier(property.getId());

        double combinedMultiplier = occupancyMultiplier * dayOfWeekMultiplier
                * seasonMultiplier * leadTimeMultiplier * customRuleMultiplier;

        // Cap adjustment at ±50% of base rate
        combinedMultiplier = Math.max(0.5, Math.min(1.5, combinedMultiplier));

        BigDecimal recommendedRate = baseRate.multiply(BigDecimal.valueOf(combinedMultiplier))
                .setScale(2, RoundingMode.HALF_UP);

        // Never go below floor
        recommendedRate = recommendedRate.max(floorRate);

        // Save to history
        LocalDate today = LocalDate.now();
        PriceHistory history = priceHistoryRepository.findByPropertyIdAndDate(property.getId(), today)
                .orElse(PriceHistory.builder().propertyId(property.getId()).date(today).build());
        history.setBaseRate(baseRate);
        history.setDynamicRate(recommendedRate);
        history.setFloorRate(floorRate);
        history.setOccupancyMultiplier(occupancyMultiplier);
        history.setSeasonMultiplier(seasonMultiplier);
        history.setDemandMultiplier(dayOfWeekMultiplier);
        history.setPricingFactors(buildFactorsDescription(occupancyMultiplier, seasonMultiplier,
                dayOfWeekMultiplier, leadTimeMultiplier));
        priceHistoryRepository.save(history);

        // Push update to property service
        try {
            propertyClient.updateDynamicRate(property.getId(),
                    Map.of("currentDynamicRate", recommendedRate,
                            "lastUpdated", LocalDateTime.now().toString()));
        } catch (Exception e) {
            log.warn("Failed to push price update to property {}: {}", property.getId(), e.getMessage());
        }

        BigDecimal savings = baseRate.subtract(recommendedRate);
        String savingsMsg = savings.compareTo(BigDecimal.ZERO) > 0
                ? String.format("Book now and save N$%.0f (%.0f%% off base rate)",
                    savings, savings.divide(baseRate, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)))
                : null;

        return new PricingRecommendation(property.getId(), baseRate, recommendedRate,
                combinedMultiplier, savingsMsg, buildFactorsDescription(
                        occupancyMultiplier, seasonMultiplier, dayOfWeekMultiplier, leadTimeMultiplier));
    }

    public List<PriceHistory> getPriceForecast(String propertyId, LocalDate from, LocalDate to) {
        return priceHistoryRepository.findByPropertyIdAndDateBetweenOrderByDateAsc(propertyId, from, to);
    }

    private double calculateOccupancyMultiplier(String propertyId) {
        // In production: query booking-engine for recent occupancy rate
        // Simulated: return neutral multiplier
        return 1.0 + (Math.random() * 0.2 - 0.1); // ±10% noise for demo
    }

    private double getDayOfWeekMultiplier() {
        DayOfWeek day = LocalDate.now().plusDays(3).getDayOfWeek();
        return switch (day) {
            case FRIDAY, SATURDAY -> 1.15;
            case SUNDAY -> 1.05;
            default -> 1.0;
        };
    }

    private double getNamibiaSeasonMultiplier() {
        Month month = LocalDate.now().getMonth();
        return switch (month) {
            // Peak: dry season / wildlife viewing
            case JUNE, JULY, AUGUST, SEPTEMBER, OCTOBER -> 1.25;
            // Shoulder: coastal season at Swakopmund
            case DECEMBER, JANUARY -> 1.15;
            // Low: rainy season
            case FEBRUARY, MARCH, APRIL -> 0.90;
            default -> 1.0;
        };
    }

    private double getLeadTimeMultiplier() {
        // Last-minute discount incentive within 48hrs
        return 0.90; // Slight default discount; would be dynamic based on checkout date proximity
    }

    private double getCustomRuleMultiplier(String propertyId) {
        List<PricingRule> rules = pricingRuleRepository
                .findByPropertyIdAndEnabledTrueOrderByPriorityDesc(propertyId);
        double multiplier = 1.0;
        LocalDate today = LocalDate.now();
        for (PricingRule rule : rules) {
            if (rule.getRuleType() == PricingRule.RuleType.DATE_RANGE) {
                if (rule.getStartDate() != null && rule.getEndDate() != null
                        && !today.isBefore(rule.getStartDate()) && !today.isAfter(rule.getEndDate())) {
                    multiplier *= rule.getMultiplier() != null ? rule.getMultiplier().doubleValue() : 1.0;
                }
            } else if (rule.getRuleType() == PricingRule.RuleType.DAY_OF_WEEK) {
                if (rule.getDayOfWeek() != null && rule.getDayOfWeek() == today.getDayOfWeek().getValue()) {
                    multiplier *= rule.getMultiplier() != null ? rule.getMultiplier().doubleValue() : 1.0;
                }
            }
        }
        return multiplier;
    }

    private String buildFactorsDescription(double occupancy, double season, double dayOfWeek, double leadTime) {
        return String.format("occupancy=%.2f, season=%.2f, dayOfWeek=%.2f, leadTime=%.2f",
                occupancy, season, dayOfWeek, leadTime);
    }

    public record PricingRecommendation(String propertyId, BigDecimal baseRate, BigDecimal recommendedRate,
                                         double multiplier, String savingsMessage, String factors) {}
}
