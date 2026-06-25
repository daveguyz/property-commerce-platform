package com.staysphere.auctionservice.controller;

import com.staysphere.auctionservice.model.*;
import com.staysphere.auctionservice.repository.*;
import com.staysphere.auctionservice.service.*;
import com.staysphere.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/auctions")
@RequiredArgsConstructor @Slf4j
public class AuctionAdminController {

    private final AuctionLotRepository lotRepository;
    private final BidRepository bidRepository;
    private final KycRecordRepository kycRecordRepository;
    private final AuctionLotService lotService;
    private final AuctionSettlementService settlementService;
    private final KycService kycService;

    /** All lots (admin view — no status filter). */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuctionLot>>> getAllLots(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(lotRepository.findAll(pageable)));
    }

    /** Manually open a lot (e.g., scheduled lot that failed to auto-open). */
    @PostMapping("/{lotId}/open")
    public ResponseEntity<ApiResponse<AuctionLot>> openLot(@PathVariable String lotId) {
        return ResponseEntity.ok(ApiResponse.success(lotService.openLot(lotId)));
    }

    /** Manually close a lot. */
    @PostMapping("/{lotId}/close")
    public ResponseEntity<ApiResponse<AuctionLot>> closeLot(@PathVariable String lotId) {
        AuctionLot closed = lotService.closeLot(lotId);
        return ResponseEntity.ok(ApiResponse.success(closed));
    }

    /** Manually trigger settlement for a closed lot. */
    @PostMapping("/{lotId}/settle")
    public ResponseEntity<ApiResponse<AuctionSettlementService.SettlementResult>> settleLot(
            @PathVariable String lotId) {
        AuctionSettlementService.SettlementResult result = settlementService.settle(lotId);
        return ResponseEntity.ok(ApiResponse.success(result, "Settlement complete"));
    }

    /** Get flagged bids for fraud review. */
    @GetMapping("/fraud/flagged")
    public ResponseEntity<ApiResponse<List<Bid>>> getFlaggedBids(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size);
        // Return bids flagged for review
        Page<Bid> flagged = bidRepository.findAll(pageable); // refine with query as needed
        List<Bid> flaggedList = flagged.getContent().stream()
                .filter(b -> Boolean.TRUE.equals(b.getFlaggedForReview()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(flaggedList));
    }

    /** Override a bid's fraud flag (admin can clear false positives). */
    @PostMapping("/fraud/bids/{bidId}/clear")
    public ResponseEntity<ApiResponse<Bid>> clearFraudFlag(@PathVariable String bidId) {
        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new IllegalArgumentException("Bid not found"));
        bid.setFlaggedForReview(false);
        return ResponseEntity.ok(ApiResponse.success(bidRepository.save(bid), "Flag cleared"));
    }

    /** Get all KYC records (admin view). */
    @GetMapping("/kyc")
    public ResponseEntity<ApiResponse<Page<KycRecord>>> getAllKycRecords(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(kycRecordRepository.findAll(pageable)));
    }

    /** Platform stats for admin dashboard. */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Object>> getStats() {
        var stats = java.util.Map.of(
                "totalLots",       lotRepository.count(),
                "liveLots",        lotRepository.countByStatus(AuctionLotStatus.OPEN)
                                 + lotRepository.countByStatus(AuctionLotStatus.EXTENDED),
                "scheduledLots",   lotRepository.countByStatus(AuctionLotStatus.SCHEDULED),
                "settledLots",     lotRepository.countByStatus(AuctionLotStatus.SETTLED),
                "totalBids",       bidRepository.count(),
                "kycVerified",     kycRecordRepository.existsByUserIdAndStatus("dummy", KycStatus.VERIFIED)
        );
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
