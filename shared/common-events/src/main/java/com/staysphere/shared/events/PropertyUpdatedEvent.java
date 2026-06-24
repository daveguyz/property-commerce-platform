package com.staysphere.shared.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PropertyUpdatedEvent {
    public static final String TOPIC = "property.updated";
    private String eventId;
    private String propertyId;
    private String hostId;
    private String shopifyProductId;
    private String updateType;
    private LocalDateTime occurredAt;
}
