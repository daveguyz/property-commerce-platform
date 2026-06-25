package com.staysphere.searchservice.controller;

import com.staysphere.searchservice.model.AuctionSearchDocument;
import com.staysphere.searchservice.service.AuctionSearchService;
import com.staysphere.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/search/auctions")
@RequiredArgsConstructor
public class AuctionSearchController {

    private final AuctionSearchService auctionSearchService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AuctionSearchDocument>>> searchAuctions(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String auctionType,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean liveOnly,
            @RequestParam(required = false) Boolean hasLivestream,
            @RequestParam(required = false) Integer startsWithinDays,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "24") int size) {

        AuctionSearchService.AuctionSearchRequest req = new AuctionSearchService.AuctionSearchRequest(
                query, city, auctionType, status, minPrice, maxPrice,
                liveOnly, hasLivestream, startsWithinDays);

        List<AuctionSearchDocument> results = auctionSearchService.search(req, page, size);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/live")
    public ResponseEntity<ApiResponse<List<AuctionSearchDocument>>> getLiveLots() {
        return ResponseEntity.ok(ApiResponse.success(auctionSearchService.findLiveLots()));
    }

    @GetMapping("/city/{city}")
    public ResponseEntity<ApiResponse<List<AuctionSearchDocument>>> getLotsByCity(
            @PathVariable String city) {
        return ResponseEntity.ok(ApiResponse.success(auctionSearchService.findUpcomingByCity(city)));
    }
}
