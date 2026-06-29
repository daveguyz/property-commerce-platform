package com.propertycommerce.bookingengine.client;
import com.propertycommerce.shared.dto.ApiResponse;
import com.propertycommerce.shared.dto.PropertyDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.Map;

@Component @Slf4j
public class PropertyServiceClientFallback implements PropertyServiceClient {
    @Override
    public ApiResponse<PropertyDTO> getProperty(String id) {
        log.warn("Property service fallback triggered for property {}", id);
        return ApiResponse.error("Property service unavailable");
    }
    @Override
    public ApiResponse<Map<LocalDate, String>> getAvailability(String id, LocalDate from, LocalDate to) {
        return ApiResponse.error("Property service unavailable");
    }
    @Override public void blockDates(String id, Map<String, Object> request) { log.warn("Cannot block dates - property service down"); }
    @Override public void unblockDates(String id, String bookingId) { log.warn("Cannot unblock dates - property service down"); }
}
