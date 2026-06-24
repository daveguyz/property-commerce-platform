package com.staysphere.notificationservice.listener;
import com.staysphere.notificationservice.service.EmailService;
import com.staysphere.notificationservice.service.SmsService;
import com.staysphere.shared.events.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.*;

@Component @Slf4j @RequiredArgsConstructor
public class BookingEventListener {
    private final EmailService emailService;
    private final SmsService smsService;

    @KafkaListener(topics = BookingCreatedEvent.TOPIC, groupId = "notification-service-group")
    public void onBookingCreated(BookingCreatedEvent event) {
        log.info("Notification: booking created {}", event.getBookingId());
        try {
            Map<String, Object> vars = new HashMap<>();
            vars.put("guestName", "Valued Guest");
            vars.put("propertyName", event.getPropertyName());
            vars.put("checkIn", event.getCheckIn().toString());
            vars.put("checkOut", event.getCheckOut().toString());
            vars.put("totalAmount", event.getTotalAmount());
            vars.put("currency", event.getCurrency());
            vars.put("bookingId", event.getBookingId());
            if (event.getGuestEmail() != null)
                emailService.sendBookingConfirmation(event.getGuestEmail(),
                        event.getGuestId(), event.getBookingId(), vars);
        } catch (Exception e) {
            log.error("Notification failed for booking created {}: {}", event.getBookingId(), e.getMessage());
        }
    }

    @KafkaListener(topics = BookingConfirmedEvent.TOPIC, groupId = "notification-service-group")
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        log.info("Notification: booking confirmed, access code dispatched for {}", event.getBookingId());
    }

    @KafkaListener(topics = BookingCancelledEvent.TOPIC, groupId = "notification-service-group")
    public void onBookingCancelled(BookingCancelledEvent event) {
        log.info("Notification: booking cancelled {}", event.getBookingId());
    }

    @KafkaListener(topics = PaymentConfirmedEvent.TOPIC, groupId = "notification-service-group")
    public void onPaymentConfirmed(PaymentConfirmedEvent event) {
        log.info("Notification: payment confirmed for booking {}", event.getBookingId());
    }

    @KafkaListener(topics = PaymentFailedEvent.TOPIC, groupId = "notification-service-group")
    public void onPaymentFailed(PaymentFailedEvent event) {
        log.warn("Notification: payment failed for booking {}", event.getBookingId());
    }
}
