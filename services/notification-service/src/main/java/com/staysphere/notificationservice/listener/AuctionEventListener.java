package com.staysphere.notificationservice.listener;

import com.staysphere.notificationservice.service.EmailService;
import com.staysphere.shared.events.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component @Slf4j @RequiredArgsConstructor
public class AuctionEventListener {

    private final EmailService emailService;

    @KafkaListener(topics = AuctionBidPlacedEvent.TOPIC, groupId = "notification-service-group")
    public void onBidPlaced(AuctionBidPlacedEvent event) {
        log.info("[Notification] Bid placed: lot={} bidder={} amount={}",
                event.getAuctionLotId(), event.getBidderId(), event.getAmount());
        // Phase B: send outbid notification to previous lead bidder
        // For now: just log; wiring email in Phase G
    }

    @KafkaListener(topics = AuctionLotOpenedEvent.TOPIC, groupId = "notification-service-group")
    public void onLotOpened(AuctionLotOpenedEvent event) {
        log.info("[Notification] Lot opened: {}", event.getAuctionLotId());
        // Phase G: send 'your lot is now live' email to seller
    }

    @KafkaListener(topics = AuctionLotClosedEvent.TOPIC, groupId = "notification-service-group")
    public void onLotClosed(AuctionLotClosedEvent event) {
        log.info("[Notification] Lot closed: {} winner={} amount={}",
                event.getAuctionLotId(), event.getWinnerId(), event.getWinningAmount());

        if (!Boolean.TRUE.equals(event.getHadWinner())) return;

        try {
            // Winner notification
            Map<String, Object> winnerVars = new HashMap<>();
            winnerVars.put("lotId",          event.getAuctionLotId());
            winnerVars.put("winningAmount",   event.getWinningAmount());
            winnerVars.put("currency",        event.getCurrency());
            winnerVars.put("propertyId",      event.getPropertyId());
            // Phase G: send full win confirmation email
            log.info("[Notification] Win notification queued for bidder {}", event.getWinnerId());
        } catch (Exception e) {
            log.error("[Notification] Failed to send win notification: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = KycVerifiedEvent.TOPIC, groupId = "notification-service-group")
    public void onKycVerified(KycVerifiedEvent event) {
        log.info("[Notification] KYC verified for user {}", event.getUserId());
        try {
            Map<String, Object> vars = new HashMap<>();
            vars.put("userEmail", event.getUserEmail());
            // Phase G: send 'identity verified — you can now bid on high-value lots' email
        } catch (Exception e) {
            log.error("[Notification] KYC notification failed: {}", e.getMessage());
        }
    }
}
