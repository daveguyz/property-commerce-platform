package com.staysphere.auctionservice.service;

import com.staysphere.auctionservice.model.*;
import com.staysphere.auctionservice.repository.*;
import com.staysphere.auctionservice.websocket.AuctionBroadcastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service @Slf4j @RequiredArgsConstructor
public class ReverseBidService {

    private final AuctionLotRepository lotRepository;
    private final BidRepository bidRepository;
    private final AuctionBroadcastService broadcastService;

    /**
     * Settle a reverse auction (lowest unique bid wins).
     * A "unique" bid is one whose amount is not shared by any other bidder.
     * If no unique bids exist, the tie is broken by earliest submission time.
     */
    @Transactional
    public ReverseSettleResult settle(String lotId) {
        AuctionLot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new IllegalArgumentException("Lot not found: " + lotId));

        if (lot.getAuctionType() != AuctionType.REVERSE) {
            throw new IllegalStateException("Lot " + lotId + " is not a reverse auction");
        }

        List<Bid> allBids = bidRepository.findAllBidsForLotOrdered(lotId);
        if (allBids.isEmpty()) {
            lot.setStatus(AuctionLotStatus.NO_RESERVE);
            lotRepository.save(lot);
            return new ReverseSettleResult(null, BigDecimal.ZERO, false, "No bids received");
        }

        // Group bids by amount, find lowest amount with exactly one bidder
        Map<BigDecimal, List<Bid>> grouped = allBids.stream()
                .collect(Collectors.groupingBy(Bid::getAmount));

        Optional<Map.Entry<BigDecimal, List<Bid>>> lowestUnique = grouped.entrySet().stream()
                .filter(e -> e.getValue().size() == 1)
                .min(Comparator.comparing(Map.Entry::getKey));

        Bid winner;
        String note;

        if (lowestUnique.isPresent()) {
            winner = lowestUnique.get().getValue().get(0);
            note = "Lowest unique bid";
        } else {
            // All bids are tied — pick the earliest submitted bid at the globally lowest amount
            BigDecimal lowestAmount = allBids.stream()
                    .map(Bid::getAmount)
                    .min(Comparator.naturalOrder())
                    .orElseThrow();
            winner = allBids.stream()
                    .filter(b -> b.getAmount().compareTo(lowestAmount) == 0)
                    .min(Comparator.comparing(Bid::getPlacedAt))
                    .orElseThrow();
            note = "Tie-broken by earliest submission time";
        }

        boolean reserveMet = lot.getReservePrice() == null
                || winner.getAmount().compareTo(lot.getReservePrice()) <= 0; // reverse: win if low enough

        // Set statuses
        allBids.forEach(b -> b.setStatus(b.getId().equals(winner.getId())
                ? (reserveMet ? BidStatus.WON : BidStatus.LOST) : BidStatus.LOST));
        bidRepository.saveAll(allBids);

        if (reserveMet) {
            lot.setStatus(AuctionLotStatus.CLOSED);
            lot.setWinnerId(winner.getBidderId());
            lot.setWinningBidId(winner.getId());
            lot.setWinningAmount(winner.getAmount());
        } else {
            lot.setStatus(AuctionLotStatus.NO_RESERVE);
        }
        lotRepository.save(lot);

        broadcastService.broadcastLotClosed(lot);
        log.info("[Reverse] Lot {} settled. Winner: {} @ {} ({})", lotId,
                winner.getBidderId(), winner.getAmount(), note);

        return new ReverseSettleResult(winner, winner.getAmount(), reserveMet, note);
    }

    public record ReverseSettleResult(
            Bid winner,
            BigDecimal winningAmount,
            boolean reserveMet,
            String note
    ) {}
}
