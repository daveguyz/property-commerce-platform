package com.staysphere.searchservice.controller;
import com.staysphere.searchservice.service.PropertySearchService;
import com.staysphere.shared.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/search") @RequiredArgsConstructor
public class SearchController {
    private final PropertySearchService searchService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<PropertyDTO>>> search(
            @ModelAttribute SearchRequestDTO request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(searchService.search(request, page, size)));
    }

    @PostMapping("/index")
    public ResponseEntity<ApiResponse<Void>> indexProperty(@RequestBody PropertyDTO dto) {
        searchService.indexProperty(dto);
        return ResponseEntity.ok(ApiResponse.success(null, "Property indexed"));
    }
}

// NOTE: AuctionSearchController is a separate controller in the same package.
// Added in Phase C — see AuctionSearchController.java
