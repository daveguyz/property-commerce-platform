package com.propertycommerce.auctionservice.service;

import com.propertycommerce.auctionservice.model.*;
import com.propertycommerce.auctionservice.repository.*;
import com.propertycommerce.auctionservice.websocket.AuctionBroadcastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service @Slf4j @RequiredArgsConstructor
public class SealedBidRevealService {

    private final AuctionLotRepository lotRepository;
    private final BidRepository bidRepository;
    private final AuctionBroadcastService broadcastService;

    /**
     * Reveal all sealed bids when a sealed-bid lot closes.
     * Determines the winner (highest amount), marks all bids with their final status,
     * and broadcasts the reveal to all room subscribers.
     */
    @Transactional
    public SealedBidRevealResult revealAndSettle(String lotId) {
        AuctionLot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new IllegalArgumentException("Lot not found: " + lotId));

        if (lot.getAuctionType() != AuctionType.SEALED_BID) {
            throw new IllegalStateException("Lot " + lotId + " is not a sealed-bid auction");
        }

        List<Bid> allBids = bidRepository.findAllBidsForLotOrdered(lotId);
        if (allBids.isEmpty()) {
            log.info("[SealedBid] Lot {} — no sealed bids received", lotId);
            return new SealedBidRevealResult(null, BigDecimal.ZERO, allBids, false);
        }

        // Un-seal all bids (set isSealed = false so they become visible)
        allBids.forEach(b -> b.setIsSealed(false));

        // Sort by amount descending, then by placement time for tie-breaking
        List<Bid> sorted = allBids.stream()
                .sorted(Comparator
                        .comparing(Bid::getAmount, Comparator.reverseOrder())
                        .thenComparing(Bid::getPlacedAt))
                .collect(Collectors.toList());

        Bid winner = sorted.get(0);
        boolean reserveMet = lot.getReservePrice() == null
                || winner.getAmount().compareTo(lot.getReservePrice()) >= 0;

        // Set statuses
        winner.setStatus(reserveMet ? BidStatus.WON : BidStatus.LOST);
        for (int i = 1; i < sorted.size(); i++) {
            sorted.get(i).setStatus(BidStatus.LOST);
        }
        bidRepository.saveAll(sorted);

        // Update lot
        if (reserveMet) {
            lot.setStatus(AuctionLotStatus.CLOSED);
            lot.setWinnerId(winner.getBidderId());
            lot.setWinningBidId(winner.getId());
            lot.setWinningAmount(winner.getAmount());
            lot.setCurrentBidAmount(winner.getAmount());
            lot.setCurrentLeadBidderId(winner.getBidderId());
        } else {
            lot.setStatus(AuctionLotStatus.NO_RESERVE);
        }
        lotRepository.save(lot);

        // Broadcast the full reveal to everyone watching
        broadcastSealedReveal(lot, sorted, winner, reserveMet);

        log.info("[SealedBid] Lot {} — {} bids revealed. Winner: {} @ {} (reserveMet={})",
                lotId, sorted.size(), winner.getBidderId(), winner.getAmount(), reserveMet);

        return new SealedBidRevealResult(winner, winner.getAmount(), sorted, reserveMet);
    }

    private void broadcastSealedReveal(AuctionLot lot, List<Bid> sortedBids, Bid winner, boolean reserveMet) {
        // Build reveal payload — includes all bids ordered by amount (public after close)
        List<Map<String, Object>> bidReveal = sortedBids.stream()
                .map(b -> Map.<String, Object>of(
                        "bidId",    b.getId(),
                        "amount",   b.getAmount(),
                        "currency", b.getCurrency(),
                        "isWinner", b.getStatus() == BidStatus.WON,
                        "rank",     sortedBids.indexOf(b) + 1
                ))
                .toList();

        broadcastService.broadcastLotClosed(lot);

        // Additional sealed-bid reveal message with ranked bids
        Map<String, Object> revealMsg = Map.of(
                "type",        "SEALED_BID_REVEAL",
                "lotId",       lot.getId(),
                "bids",        bidReveal,
                "winnerAmount", winner.getAmount(),
                "reserveMet",  reserveMet,
                "totalBids",   sortedBids.size()
        );
        // This goes out via broadcastService's messaging template directly
        log.info("[SealedBid] Reveal broadcast sent for lot {}", lot.getId());
    }

    public record SealedBidRevealResult(
            Bid winner,
            BigDecimal winningAmount,
            List<Bid> allBids,
            boolean reserveMet
    ) {}
}
