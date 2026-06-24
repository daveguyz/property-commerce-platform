package com.staysphere.notificationservice.service;

import com.staysphere.notificationservice.model.NotificationLog;
import com.staysphere.notificationservice.repository.NotificationLogRepository;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service @Slf4j @RequiredArgsConstructor
public class SmsService {

    private final NotificationLogRepository notificationLogRepository;

    @Value("${twilio.account-sid}") private String accountSid;
    @Value("${twilio.auth-token}") private String authToken;
    @Value("${twilio.from-number}") private String fromNumber;
    @Value("${twilio.whatsapp-number:}") private String whatsappNumber;

    public void sendSms(String to, String userId, String bookingId,
            String body, NotificationLog.NotificationType type) {
        // Use 'notifLog' — 'log' is the Lombok SLF4J logger
        NotificationLog notifLog = NotificationLog.builder()
                .userId(userId).bookingId(bookingId).type(type)
                .channel(NotificationLog.NotificationChannel.SMS)
                .recipient(to).body(body).retryCount(0)
                .status(NotificationLog.NotificationStatus.PENDING).build();
        try {
            Twilio.init(accountSid, authToken);
            Message message = Message.creator(
                    new PhoneNumber(to), new PhoneNumber(fromNumber), body).create();
            notifLog.setStatus(NotificationLog.NotificationStatus.SENT);
            notifLog.setSentAt(LocalDateTime.now());
            log.info("SMS sent to {} sid={}", to, message.getSid());
        } catch (Exception e) {
            log.error("SMS failed to {}: {}", to, e.getMessage());
            notifLog.setStatus(NotificationLog.NotificationStatus.FAILED);
            notifLog.setErrorMessage(e.getMessage());
        }
        notificationLogRepository.save(notifLog);
    }

    public void sendWhatsApp(String to, String userId, String bookingId,
            String body, NotificationLog.NotificationType type) {
        NotificationLog notifLog = NotificationLog.builder()
                .userId(userId).bookingId(bookingId).type(type)
                .channel(NotificationLog.NotificationChannel.WHATSAPP)
                .recipient(to).body(body).retryCount(0)
                .status(NotificationLog.NotificationStatus.PENDING).build();
        try {
            Twilio.init(accountSid, authToken);
            Message message = Message.creator(
                    new PhoneNumber("whatsapp:" + to),
                    new PhoneNumber("whatsapp:" + whatsappNumber), body).create();
            notifLog.setStatus(NotificationLog.NotificationStatus.SENT);
            notifLog.setSentAt(LocalDateTime.now());
            log.info("WhatsApp sent to {} sid={}", to, message.getSid());
        } catch (Exception e) {
            log.error("WhatsApp failed to {}: {}", to, e.getMessage());
            notifLog.setStatus(NotificationLog.NotificationStatus.FAILED);
            notifLog.setErrorMessage(e.getMessage());
        }
        notificationLogRepository.save(notifLog);
    }

    public void sendAccessCode(String phone, String guestName, String propertyName,
            String accessCode, String checkIn, String userId, String bookingId) {
        String body = String.format(
                "Hi %s! Your StaySphere access code for %s on %s is: *%s*. Enjoy your stay!",
                guestName, propertyName, checkIn, accessCode);
        sendSms(phone, userId, bookingId, body, NotificationLog.NotificationType.BOOKING_CONFIRMED);
        if (whatsappNumber != null && !whatsappNumber.isBlank()) {
            sendWhatsApp(phone, userId, bookingId, body, NotificationLog.NotificationType.BOOKING_CONFIRMED);
        }
    }
}
