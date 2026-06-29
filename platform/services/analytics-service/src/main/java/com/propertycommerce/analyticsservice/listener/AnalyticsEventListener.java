package com.propertycommerce.analyticsservice.listener;
import com.propertycommerce.analyticsservice.service.AnalyticsService;
import com.propertycommerce.shared.events.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
@Component @Slf4j @RequiredArgsConstructor
public class AnalyticsEventListener {
    private final AnalyticsService analyticsService;

    @KafkaListener(topics = BookingCreatedEvent.TOPIC, groupId = "analytics-service-group")
    public void onBookingCreated(BookingCreatedEvent event) {
        try {
            analyticsService.recordBookingMetric(event.getPropertyId(), event.getHostId(),
                    event.getTotalAmount(), event.getPlatformFee(), event.getHostPayout(), false);
        } catch (Exception e) { log.error("Analytics booking event error: {}", e.getMessage()); }
    }

    @KafkaListener(topics = BookingCancelledEvent.TOPIC, groupId = "analytics-service-group")
    public void onBookingCancelled(BookingCancelledEvent event) {
        try {
            analyticsService.recordBookingMetric(event.getPropertyId(), event.getHostId(),
                    java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, true);
        } catch (Exception e) { log.error("Analytics cancel event error: {}", e.getMessage()); }
    }
}
