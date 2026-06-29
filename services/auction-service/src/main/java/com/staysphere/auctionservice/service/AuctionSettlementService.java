package com.staysphere.auctionservice.service;

import com.staysphere.auctionservice.model.*;
import com.staysphere.auctionservice.repository.*;
import com.staysphere.auctionservice.websocket.AuctionBroadcastService;
import com.staysphere.auctionservice.service.LivestreamService;
import com.staysphere.shared.events.AuctionLotClosedEvent;
import com.staysphere.shared.events.PurchaseAgreementRequiredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Orchestrates the full settlement flow after a lot closes:
 *   1. Determine winner (type-specific)
 *   2. Capture winner's deposit
 *   3. Release all loser deposits
 *   4. Update lot status to SETTLED
 *   5. Emit Kafka event to trigger notifications
 *   6. Broadcast final state via WebSocket
 *
 * Called by AuctionSchedulerService after closeLot().
 */
@Service @Slf4j @RequiredArgsConstructor
public class AuctionSettlementService {

    @org.springframework.beans.factory.annotation.Value(
            "${staysphere.agreement.payment-deadline-days:10}")
    private int paymentDeadlineDays;

    private final AuctionLotRepository lotRepository;
    private final BidRepository bidRepository;
    private final DepositService depositService;
    private final BiddingCredentialService credentialService;
    private final SealedBidRevealService sealedBidRevealService;
    private final ReverseBidService reverseBidService;
    private final AuctionBroadcastService broadcastService;
    private final LivestreamService livestreamService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public SettlementResult settle(String lotId) {
        AuctionLot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new IllegalArgumentException("Lot not found: " + lotId));

        log.info("[Settlement] Starting settlement for lot {} type={} status={}",
                lotId, lot.getAuctionType(), lot.getStatus());

        // ── Step 1: Type-specific settlement ─────────────────────────────
        switch (lot.getAuctionType()) {
            case SEALED_BID -> {
                // Reveal and determine winner
                SealedBidRevealService.SealedBidRevealResult result =
                        sealedBidRevealService.revealAndSettle(lotId);
                // Reload lot after reveal updated it
                lot = lotRepository.findById(lotId).orElseThrow();
            }
            case REVERSE -> {
                ReverseBidService.ReverseSettleResult result =
                        reverseBidService.settle(lotId);
                lot = lotRepository.findById(lotId).orElseThrow();
            }
            case DUTCH -> {
                // Dutch lots close instantly via acceptDutchPrice() or scheduler —
                // winner already set on the lot. Nothing extra to do here.
                log.debug("[Settlement] Dutch lot {} — winner already set", lotId);
            }
            case ENGLISH -> {
                // English: winner already set by closeLot() in AuctionLotService
                log.debug("[Settlement] English lot {} — winner already set", lotId);
            }
        }

        // Must be CLOSED (not NO_RESERVE or CANCELLED) to have a payable winner
        if (lot.getStatus() != AuctionLotStatus.CLOSED) {
            log.info("[Settlement] Lot {} has no payable winner (status={})", lotId, lot.getStatus());
            // Still release all deposits
            releaseAllDeposits(lotId, null);
            return SettlementResult.noWinner(lotId, lot.getStatus());
        }

        String winnerId = lot.getWinnerId();

        // ── Step 2: Capture winner's deposit ─────────────────────────────
        if (Boolean.TRUE.equals(lot.getDepositRequired()) && winnerId != null) {
            try {
                depositService.captureWinnerDeposit(lotId, winnerId);
                log.info("[Settlement] Winner deposit captured for lot {} bidder {}", lotId, winnerId);
            } catch (Exception e) {
                log.error("[Settlement] Failed to capture winner deposit: {}", e.getMessage());
                // Non-fatal — payment team can handle manually
            }
        }

        // ── Step 3: Release loser deposits ───────────────────────────────
        releaseAllDeposits(lotId, winnerId);

        // ── Step 4: Mark lot as SETTLED ──────────────────────────────────
        lot.setStatus(AuctionLotStatus.SETTLED);
        lotRepository.save(lot);

        // Clean up livestream resources after settlement
        try {
            livestreamService.deleteStream(lotId);
        } catch (Exception e) {
            log.debug("[Settlement] Livestream cleanup skipped for lot {}: {}", lotId, e.getMessage());
        }

        // ── Step 5: Kafka event (triggers notification emails) ───────────
        kafkaTemplate.send(AuctionLotClosedEvent.TOPIC, AuctionLotClosedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .auctionLotId(lotId)
                .propertyId(lot.getPropertyId())
                .sellerId(lot.getSellerId())
                .winnerId(winnerId)
                .winningAmount(lot.getWinningAmount())
                .currency(lot.getCurrency())
                .hadWinner(true)
                .reserveMet(true)
                .occurredAt(LocalDateTime.now())
                .build());

        // Fire purchase agreement creation — booking-engine picks this up
        // Deposit amount = what was captured from the winner
        java.math.BigDecimal depositAmt = depositService
                .getWinnerDepositAmount(lotId, winnerId)
                .orElse(java.math.BigDecimal.ZERO);

        kafkaTemplate.send(PurchaseAgreementRequiredEvent.TOPIC,
                PurchaseAgreementRequiredEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .lotId(lotId)
                        .propertyId(lot.getPropertyId())
                        .lotTitle(lot.getTitle())
                        .winnerId(winnerId)
                        .winnerEmail("")  // TODO: resolve via auth-service Feign call
                        .sellerId(lot.getSellerId())
                        .sellerEmail("")  // TODO: resolve via auth-service Feign call
                        .auctioneerId(lot.getAuctioneerId())
                        .winningAmount(lot.getWinningAmount())
                        .depositAmount(depositAmt)
                        .currency(lot.getCurrency())
                        .auctionClosedAt(LocalDateTime.now())
                        .paymentDeadlineDays(paymentDeadlineDays)
                        .build());

        // ── Step 6: Final WebSocket broadcast ────────────────────────────
        broadcastService.broadcastLotClosed(lot);

        log.info("[Settlement] Lot {} SETTLED — winner={} amount={}",
                lotId, winnerId, lot.getWinningAmount());

        return SettlementResult.settled(lotId, winnerId, lot.getWinningAmount());
    }

    private void releaseAllDeposits(String lotId, String excludeBidderId) {
        try {
            depositService.releaseLoserDeposits(lotId, excludeBidderId != null ? excludeBidderId : "__no_winner__");
        } catch (Exception e) {
            log.error("[Settlement] Error releasing deposits for lot {}: {}", lotId, e.getMessage());
        }
    }

    public record SettlementResult(
            String lotId,
            String winnerId,
            java.math.BigDecimal winningAmount,
            AuctionLotStatus finalStatus,
            boolean hasWinner
    ) {
        static SettlementResult settled(String lotId, String winnerId, java.math.BigDecimal amount) {
            return new SettlementResult(lotId, winnerId, amount, AuctionLotStatus.SETTLED, true);
        }
        static SettlementResult noWinner(String lotId, AuctionLotStatus status) {
            return new SettlementResult(lotId, null, java.math.BigDecimal.ZERO, status, false);
        }
    }
}
