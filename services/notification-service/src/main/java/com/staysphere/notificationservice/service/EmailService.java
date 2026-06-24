package com.staysphere.notificationservice.service;

import com.staysphere.notificationservice.model.NotificationLog;
import com.staysphere.notificationservice.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.Map;

@Service @Slf4j @RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final NotificationLogRepository notificationLogRepository;

    @Value("${spring.mail.from:noreply@staysphere.com}") private String fromEmail;
    @Value("${staysphere.brand.name:StaySphere}") private String brandName;

    public void sendBookingConfirmation(String to, String userId, String bookingId,
            Map<String, Object> templateVars) {
        sendEmail(to, userId, bookingId, "booking-confirmation",
                "Your StaySphere Booking is Confirmed!", templateVars,
                NotificationLog.NotificationType.BOOKING_CONFIRMED);
    }

    public void sendBookingCancellation(String to, String userId, String bookingId,
            Map<String, Object> templateVars) {
        sendEmail(to, userId, bookingId, "booking-cancellation",
                "Your StaySphere Booking Has Been Cancelled", templateVars,
                NotificationLog.NotificationType.BOOKING_CANCELLED);
    }

    public void sendCheckInReminder(String to, String userId, String bookingId,
            Map<String, Object> templateVars) {
        sendEmail(to, userId, bookingId, "check-in-reminder",
                "Check-In Tomorrow — Your Access Code is Ready", templateVars,
                NotificationLog.NotificationType.CHECK_IN_REMINDER);
    }

    public void sendCheckOutReminder(String to, String userId, String bookingId,
            Map<String, Object> templateVars) {
        sendEmail(to, userId, bookingId, "check-out-reminder",
                "Check-Out Reminder for Tomorrow", templateVars,
                NotificationLog.NotificationType.CHECK_OUT_REMINDER);
    }

    public void sendReviewRequest(String to, String userId, String bookingId,
            Map<String, Object> templateVars) {
        sendEmail(to, userId, bookingId, "review-request",
                "How Was Your Stay? Leave a Review", templateVars,
                NotificationLog.NotificationType.REVIEW_REQUEST);
    }

    public void sendPaymentConfirmation(String to, String userId, String bookingId,
            Map<String, Object> templateVars) {
        sendEmail(to, userId, bookingId, "payment-confirmed",
                "Payment Received for Your Booking", templateVars,
                NotificationLog.NotificationType.PAYMENT_CONFIRMED);
    }

    public void sendPaymentFailed(String to, String userId, String bookingId,
            Map<String, Object> templateVars) {
        sendEmail(to, userId, bookingId, "payment-failed",
                "Payment Failed for Your Booking", templateVars,
                NotificationLog.NotificationType.PAYMENT_FAILED);
    }

    public void sendWelcomeEmail(String to, String userId, Map<String, Object> templateVars) {
        sendEmail(to, userId, null, "welcome",
                "Welcome to StaySphere — Your Namibian Home Away From Home", templateVars,
                NotificationLog.NotificationType.WELCOME);
    }

    private void sendEmail(String to, String userId, String bookingId, String templateName,
            String subject, Map<String, Object> vars,
            NotificationLog.NotificationType type) {

        // Use 'notifLog' — NOT 'log', which is Lombok's SLF4J logger
        NotificationLog notifLog = NotificationLog.builder()
                .userId(userId).bookingId(bookingId).type(type)
                .channel(NotificationLog.NotificationChannel.EMAIL)
                .recipient(to).subject(subject).retryCount(0)
                .status(NotificationLog.NotificationStatus.PENDING).build();

        try {
            Context context = new Context();
            context.setVariables(vars);
            context.setVariable("brandName", brandName);
            context.setVariable("year", java.time.Year.now().getValue());
            String html = templateEngine.process("email/" + templateName, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, brandName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);

            notifLog.setStatus(NotificationLog.NotificationStatus.SENT);
            notifLog.setSentAt(LocalDateTime.now());
            notifLog.setBody(html.substring(0, Math.min(500, html.length())));
            log.info("Email sent to {} [{}]", to, type);

        } catch (Exception e) {
            log.error("Email send failed to {} [{}]: {}", to, type, e.getMessage());
            notifLog.setStatus(NotificationLog.NotificationStatus.FAILED);
            notifLog.setErrorMessage(e.getMessage());
        }
        notificationLogRepository.save(notifLog);
    }
}
