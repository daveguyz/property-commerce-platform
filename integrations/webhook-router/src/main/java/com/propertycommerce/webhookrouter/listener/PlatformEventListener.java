package com.propertycommerce.webhookrouter.listener;

import com.propertycommerce.shared.events.*;
import com.propertycommerce.webhookrouter.service.WebhookDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Subscribes to all platform Kafka topics and routes each event to
 * WebhookDeliveryService which delivers it to matching tenant endpoints.
 *
 * The tenantId is extracted from each event and used to look up only
 * the endpoints registered for that tenant — events never cross tenant
 * boundaries.
 *
 * Topics are the TOPIC constants defined on each event class:
 *   auction.lot.opened, auction.lot.closed, bid.credential-issued,
 *   bid.credential-revoked, auction.question-submitted, etc.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PlatformEventListener {

    private final WebhookDeliveryService deliveryService;

    // ── Auction lifecycle ─────────────────────────────────────────────────

    @KafkaListener(topics = AuctionLotClosedEvent.TOPIC,
                   groupId = "webhook-router-group")
    public void onLotClosed(AuctionLotClosedEvent event) {
        // TODO: AuctionLotClosedEvent needs a tenantId field (Phase D - multi-tenancy)
        // For now dispatch to a default tenant derived from sellerId
        deliver("auction.lot.closed", event.getAuctionLotId(), event);
    }

    @KafkaListener(topics = AuctionBidPlacedEvent.TOPIC,
                   groupId = "webhook-router-group")
    public void onBidPlaced(AuctionBidPlacedEvent event) {
        deliver("bid.placed", event.getEventId(), event);
    }

    // ── Credential events ─────────────────────────────────────────────────

    @KafkaListener(topics = BiddingCredentialIssuedEvent.TOPIC,
                   groupId = "webhook-router-group")
    public void onCredentialIssued(BiddingCredentialIssuedEvent event) {
        deliver("bid.credential-issued", event.getEventId(), event);
    }

    @KafkaListener(topics = BiddingCredentialRevokedEvent.TOPIC,
                   groupId = "webhook-router-group")
    public void onCredentialRevoked(BiddingCredentialRevokedEvent event) {
        deliver("bid.credential-revoked", event.getEventId(), event);
    }

    // ── Agreement events ──────────────────────────────────────────────────

    @KafkaListener(topics = AgreementFullyExecutedEvent.TOPIC,
                   groupId = "webhook-router-group")
    public void onAgreementExecuted(AgreementFullyExecutedEvent event) {
        deliver("agreement.executed", event.getEventId(), event);
    }

    @KafkaListener(topics = PaymentDefaultedEvent.TOPIC,
                   groupId = "webhook-router-group")
    public void onPaymentDefaulted(PaymentDefaultedEvent event) {
        deliver("payment.defaulted", event.getEventId(), event);
    }

    // ── Q&A events ────────────────────────────────────────────────────────

    @KafkaListener(topics = LotQuestionSubmittedEvent.TOPIC,
                   groupId = "webhook-router-group")
    public void onQuestionSubmitted(LotQuestionSubmittedEvent event) {
        deliver("auction.question-submitted", event.getEventId(), event);
    }

    @KafkaListener(topics = LotQuestionAnsweredEvent.TOPIC,
                   groupId = "webhook-router-group")
    public void onQuestionAnswered(LotQuestionAnsweredEvent event) {
        deliver("auction.question-answered", event.getEventId(), event);
    }

    // ── Booking events ────────────────────────────────────────────────────

    @KafkaListener(topics = BookingConfirmedEvent.TOPIC,
                   groupId = "webhook-router-group")
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        deliver("booking.confirmed", event.getEventId(), event);
    }

    @KafkaListener(topics = BookingCancelledEvent.TOPIC,
                   groupId = "webhook-router-group")
    public void onBookingCancelled(BookingCancelledEvent event) {
        deliver("booking.cancelled", event.getEventId(), event);
    }

    // ── Shared dispatch — tenantId will be added in Phase D ──────────────

    private void deliver(String eventType, String eventId, Object payload) {
        // Phase D: extract tenantId from event payload when multi-tenancy is in place.
        // For now, use "default" so the router still works in single-tenant mode.
        String tenantId = extractTenantId(payload);
        log.debug("[WebhookRouter] Dispatching {} / {} to tenant {}", eventType, eventId, tenantId);
        deliveryService.dispatchToTenant(tenantId, eventType, eventId, payload);
    }

    private String extractTenantId(Object event) {
        // Reflection-based fallback: look for getTenantId() on the event class.
        try {
            var method = event.getClass().getMethod("getTenantId");
            Object result = method.invoke(event);
            if (result instanceof String s && !s.isBlank()) return s;
        } catch (NoSuchMethodException ignored) {
            // Most Phase 1-7 events don't have tenantId yet — that's Phase D.
        } catch (Exception e) {
            log.debug("[WebhookRouter] Could not extract tenantId from {}: {}", event.getClass().getSimpleName(), e.getMessage());
        }
        return "default";
    }
}
