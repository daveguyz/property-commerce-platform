package com.staysphere.auctionservice.service;

import com.staysphere.auctionservice.model.AuctionLot;
import com.staysphere.auctionservice.repository.AuctionLotRepository;
import com.staysphere.shared.events.AuctionLotOpenedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service @Slf4j @RequiredArgsConstructor
public class AuctionSchedulerService {

    private final AuctionLotRepository lotRepository;
    private final AuctionLotService lotService;
    private final AuctionSettlementService settlementService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ─── Open lots ─────────────────────────────────────────────────────────

    /** Every 10 seconds: open any lots whose startsAt has passed. */
    @Scheduled(fixedRate = 10_000)
    public void openScheduledLots() {
        List<AuctionLot> ready = lotRepository.findLotsReadyToOpen(LocalDateTime.now());
        for (AuctionLot lot : ready) {
            try {
                lotService.openLot(lot.getId());
                log.info("[Scheduler] Opened lot {}", lot.getId());
            } catch (Exception e) {
                log.error("[Scheduler] Failed to open lot {}: {}", lot.getId(), e.getMessage());
            }
        }
    }

    // ─── Close lots ────────────────────────────────────────────────────────

    /** Every 10 seconds: close any lots whose scheduledEndsAt has passed. */
    @Scheduled(fixedRate = 10_000)
    public void closeExpiredLots() {
        List<AuctionLot> expired = lotRepository.findLotsReadyToClose(LocalDateTime.now());
        for (AuctionLot lot : expired) {
            try {
                lotService.closeLot(lot.getId());
                log.info("[Scheduler] Closed lot {}", lot.getId());
                // Immediately trigger full settlement chain
                try {
                    settlementService.settle(lot.getId());
                    log.info("[Scheduler] Settled lot {}", lot.getId());
                } catch (Exception se) {
                    log.error("[Scheduler] Settlement failed for lot {}: {}", lot.getId(), se.getMessage());
                }
            } catch (Exception e) {
                log.error("[Scheduler] Failed to close lot {}: {}", lot.getId(), e.getMessage());
            }
        }
    }

    // ─── Reminders — wired to Kafka → notification-service ────────────────

    /**
     * Every 5 minutes: send 24-hour advance reminder for lots starting tomorrow.
     * Runs on cron at every :00 and :05 etc., checks window to avoid double-sends.
     */
    @Scheduled(fixedRate = 300_000)  // every 5 min
    public void send24HourReminders() {
        LocalDateTime from  = LocalDateTime.now().plusHours(23).plusMinutes(55);
        LocalDateTime until = LocalDateTime.now().plusHours(24).plusMinutes(5);
        List<AuctionLot> lots = lotRepository.findLotsStartingSoon(from, until);
        lots.forEach(lot -> {
            try {
                kafkaTemplate.send("auction.reminder.24h",
                        buildReminderPayload(lot, "24H"));
                log.info("[Scheduler] 24h reminder sent for lot {}", lot.getId());
            } catch (Exception e) {
                log.warn("[Scheduler] 24h reminder failed for lot {}: {}", lot.getId(), e.getMessage());
            }
        });
    }

    /**
     * Every minute: send 1-hour advance reminder.
     */
    @Scheduled(fixedRate = 60_000)  // every 1 min
    public void send1HourReminders() {
        LocalDateTime from  = LocalDateTime.now().plusMinutes(58);
        LocalDateTime until = LocalDateTime.now().plusMinutes(62);
        List<AuctionLot> lots = lotRepository.findLotsStartingSoon(from, until);
        lots.forEach(lot -> {
            try {
                kafkaTemplate.send("auction.reminder.1h",
                        buildReminderPayload(lot, "1H"));
                log.info("[Scheduler] 1h reminder sent for lot {}", lot.getId());
            } catch (Exception e) {
                log.warn("[Scheduler] 1h reminder failed for lot {}: {}", lot.getId(), e.getMessage());
            }
        });
    }

    /**
     * Every day at 09:00: index refresh — re-emit SCHEDULED lots as light-weight
     * index update events so the search index stays fresh.
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void dailyIndexRefresh() {
        long scheduled = lotRepository.countByStatus(
                com.staysphere.auctionservice.model.AuctionLotStatus.SCHEDULED);
        long open = lotRepository.countByStatus(
                com.staysphere.auctionservice.model.AuctionLotStatus.OPEN);
        log.info("[Scheduler] Daily stats — scheduled={} open={}", scheduled, open);
    }

    // ─── Stale EXTENDED lot safety net ────────────────────────────────────

    /**
     * Every minute: if an EXTENDED lot has had its actual end time pushed
     * more than maxAntiSnipeExtensions times AND its new end has passed, close it.
     * (Belt-and-suspenders in case the 10s closer missed it.)
     */
    @Scheduled(fixedRate = 60_000)
    public void safetyCloseExtendedLots() {
        List<AuctionLot> extended = lotRepository.findAllLiveLots().stream()
                .filter(l -> l.getStatus() == com.staysphere.auctionservice.model.AuctionLotStatus.EXTENDED)
                .filter(l -> l.getScheduledEndsAt().isBefore(LocalDateTime.now()))
                .toList();

        for (AuctionLot lot : extended) {
            try {
                lotService.closeLot(lot.getId());
                settlementService.settle(lot.getId());
                log.info("[Scheduler] Safety-closed EXTENDED lot {}", lot.getId());
            } catch (Exception e) {
                log.error("[Scheduler] Safety-close failed for lot {}: {}", lot.getId(), e.getMessage());
            }
        }
    }

    // ─── Helpers ───────────────────────────────────────────────────────────

    private java.util.Map<String, Object> buildReminderPayload(AuctionLot lot, String window) {
        return java.util.Map.of(
                "eventId",       UUID.randomUUID().toString(),
                "auctionLotId",  lot.getId(),
                "title",         lot.getTitle(),
                "sellerId",      lot.getSellerId(),
                "propertyId",    lot.getPropertyId(),
                "startingPrice", lot.getStartingPrice(),
                "currency",      lot.getCurrency(),
                "startsAt",      lot.getStartsAt().toString(),
                "reminderWindow", window
        );
    }
}
