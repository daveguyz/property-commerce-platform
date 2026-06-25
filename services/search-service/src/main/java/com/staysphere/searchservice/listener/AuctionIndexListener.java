package com.staysphere.searchservice.listener;

import com.staysphere.searchservice.model.AuctionSearchDocument;
import com.staysphere.searchservice.service.AuctionSearchService;
import com.staysphere.shared.events.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component @Slf4j @RequiredArgsConstructor
public class AuctionIndexListener {

    private final AuctionSearchService auctionSearchService;

    /**
     * Index a lot when it opens (we get the full lot data from the event).
     * If the lot isn't already indexed, this creates the document.
     * If it is, the status + price are updated.
     */
    @KafkaListener(topics = AuctionLotOpenedEvent.TOPIC, groupId = "search-service-group")
    public void onLotOpened(AuctionLotOpenedEvent event) {
        log.info("[AuctionIndex] Lot opened, indexing: {}", event.getAuctionLotId());
        try {
            AuctionSearchDocument doc = AuctionSearchDocument.builder()
                    .id(event.getAuctionLotId())
                    .propertyId(event.getPropertyId())
                    .sellerId(event.getSellerId())
                    .auctionType(event.getAuctionType())
                    .status("OPEN")
                    .startingPrice(event.getStartingPrice())
                    .currentBidAmount(event.getStartingPrice())
                    .currency("NAD")
                    .startsAt(LocalDateTime.now())
                    .scheduledEndsAt(event.getScheduledEndsAt())
                    .totalBids(0)
                    .uniqueBidders(0)
                    .livestreamActive(false)
                    .indexedAt(LocalDateTime.now())
                    .build();
            auctionSearchService.indexLot(doc);
        } catch (Exception e) {
            log.error("[AuctionIndex] Failed to index lot {}: {}", event.getAuctionLotId(), e.getMessage());
        }
    }

    /**
     * Update bid count, current price, and status on every bid.
     */
    @KafkaListener(topics = AuctionBidPlacedEvent.TOPIC, groupId = "search-service-group")
    public void onBidPlaced(AuctionBidPlacedEvent event) {
        log.debug("[AuctionIndex] Bid placed on lot {}, updating index", event.getAuctionLotId());
        try {
            auctionSearchService.updateLotStatus(
                    event.getAuctionLotId(),
                    event.getAntiSnipeExtended() ? "EXTENDED" : "OPEN",
                    event.getAmount(),
                    event.getTotalBids(),
                    null,   // uniqueBidders not in event — will be stale until next full reindex
                    false
            );
        } catch (Exception e) {
            log.error("[AuctionIndex] Failed to update index for lot {}: {}", event.getAuctionLotId(), e.getMessage());
        }
    }

    /**
     * Update lot status to CLOSED/SETTLED in the search index.
     */
    @KafkaListener(topics = AuctionLotClosedEvent.TOPIC, groupId = "search-service-group")
    public void onLotClosed(AuctionLotClosedEvent event) {
        log.info("[AuctionIndex] Lot closed, updating index: {}", event.getAuctionLotId());
        try {
            auctionSearchService.updateLotStatus(
                    event.getAuctionLotId(),
                    event.getReserveMet() ? "CLOSED" : "NO_RESERVE",
                    event.getWinningAmount(),
                    null, null, false
            );
        } catch (Exception e) {
            log.error("[AuctionIndex] Failed to update closed lot {}: {}", event.getAuctionLotId(), e.getMessage());
        }
    }

    /**
     * Update status to EXTENDED when anti-snipe triggers.
     */
    @KafkaListener(topics = AuctionLotExtendedEvent.TOPIC, groupId = "search-service-group")
    public void onLotExtended(AuctionLotExtendedEvent event) {
        log.debug("[AuctionIndex] Lot {} extended — updating index", event.getAuctionLotId());
        try {
            auctionSearchService.updateLotStatus(
                    event.getAuctionLotId(), "EXTENDED", null, null, null, false);
        } catch (Exception e) {
            log.error("[AuctionIndex] Failed to update extended lot {}: {}", event.getAuctionLotId(), e.getMessage());
        }
    }
}
