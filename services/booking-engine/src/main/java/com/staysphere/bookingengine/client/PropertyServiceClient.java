package com.staysphere.bookingengine.client;
import com.staysphere.shared.dto.ApiResponse;
import com.staysphere.shared.dto.PropertyDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.Map;

@FeignClient(name = "property-service", fallback = PropertyServiceClientFallback.class)
public interface PropertyServiceClient {
    @GetMapping("/api/v1/properties/{id}")
    ApiResponse<PropertyDTO> getProperty(@PathVariable String id);

    @GetMapping("/api/v1/properties/{id}/availability")
    ApiResponse<Map<LocalDate, String>> getAvailability(@PathVariable String id,
        @RequestParam LocalDate from, @RequestParam LocalDate to);

    @PostMapping("/api/v1/internal/properties/{id}/block-dates")
    void blockDates(@PathVariable String id, @RequestBody Map<String, Object> request);

    @DeleteMapping("/api/v1/internal/properties/{id}/unblock/{bookingId}")
    void unblockDates(@PathVariable String id, @PathVariable String bookingId);
}
