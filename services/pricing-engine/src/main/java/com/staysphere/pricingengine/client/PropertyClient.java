package com.staysphere.pricingengine.client;
import com.staysphere.shared.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@FeignClient(name = "property-service")
public interface PropertyClient {
    @GetMapping("/api/v1/properties/search")
    ApiResponse<PagedResponse<PropertyDTO>> getAllActive(@RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "100") int size);

    @GetMapping("/api/v1/properties/{id}")
    ApiResponse<PropertyDTO> getProperty(@PathVariable String id);

    @PatchMapping("/api/v1/internal/properties/{id}/dynamic-rate")
    void updateDynamicRate(@PathVariable String id, @RequestBody java.util.Map<String, Object> request);
}
