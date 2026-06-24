package com.staysphere.aiservice.client;
import com.staysphere.shared.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "property-service")
public interface PropertySearchClient {
    @GetMapping("/api/v1/properties/search")
    ApiResponse<PagedResponse<PropertyDTO>> searchProperties(@ModelAttribute SearchRequestDTO request,
        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size);
}
