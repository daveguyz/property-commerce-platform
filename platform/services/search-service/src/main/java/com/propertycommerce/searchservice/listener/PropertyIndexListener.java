package com.propertycommerce.searchservice.listener;
import com.propertycommerce.searchservice.service.PropertySearchService;
import com.propertycommerce.shared.events.PropertyUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component @Slf4j @RequiredArgsConstructor
public class PropertyIndexListener {
    private final PropertySearchService searchService;

    @KafkaListener(topics = PropertyUpdatedEvent.TOPIC, groupId = "search-service-group")
    public void onPropertyUpdated(PropertyUpdatedEvent event) {
        log.info("Reindexing property {} ({})", event.getPropertyId(), event.getUpdateType());
        if ("DELETED".equals(event.getUpdateType())) {
            searchService.removeProperty(event.getPropertyId());
        }
    }
}
