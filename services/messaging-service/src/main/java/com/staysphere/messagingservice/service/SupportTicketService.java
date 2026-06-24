package com.staysphere.messagingservice.service;
import com.staysphere.messagingservice.model.*;
import com.staysphere.messagingservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Map;

@Service @Slf4j @RequiredArgsConstructor @Transactional(readOnly = true)
public class SupportTicketService {
    private final SupportTicketRepository ticketRepository;
    private final MessagingService messagingService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public SupportTicket createTicket(String userId, String subject, String description,
            SupportTicket.TicketCategory category, SupportTicket.TicketPriority priority,
            String bookingId, String propertyId) {
        // Create a support conversation (user ↔ admin)
        Conversation conv = messagingService.getOrCreateConversation(
                userId, "ADMIN_SUPPORT", null, propertyId, Conversation.ConversationType.GUEST_ADMIN);

        SupportTicket ticket = SupportTicket.builder()
                .userId(userId).subject(subject).description(description)
                .category(category != null ? category : SupportTicket.TicketCategory.GENERAL)
                .priority(priority != null ? priority : SupportTicket.TicketPriority.NORMAL)
                .bookingId(bookingId).propertyId(propertyId)
                .conversationId(conv.getId()).build();
        SupportTicket saved = ticketRepository.save(ticket);

        // Send initial message
        messagingService.sendMessage(conv.getId(), userId, description,
                Message.MessageType.SYSTEM, null);

        kafkaTemplate.send("support.ticket.created",
                Map.of("ticketId", saved.getId(), "userId", userId,
                       "subject", subject, "category", category != null ? category.name() : "GENERAL",
                       "priority", priority != null ? priority.name() : "NORMAL"));

        log.info("Support ticket {} created by user {}", saved.getId(), userId);
        return saved;
    }

    @Transactional
    public SupportTicket updateTicketStatus(String ticketId, String adminId,
            SupportTicket.TicketStatus newStatus, String resolutionNotes) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));
        ticket.setStatus(newStatus);
        ticket.setAssignedAdminId(adminId);
        if (newStatus == SupportTicket.TicketStatus.RESOLVED ||
                newStatus == SupportTicket.TicketStatus.CLOSED) {
            ticket.setResolvedAt(LocalDateTime.now());
            ticket.setResolutionNotes(resolutionNotes);
        }
        SupportTicket saved = ticketRepository.save(ticket);
        // Notify user via WebSocket/Kafka
        kafkaTemplate.send("support.ticket.updated",
                Map.of("ticketId", ticketId, "userId", ticket.getUserId(),
                       "newStatus", newStatus.name(), "adminId", adminId));
        return saved;
    }

    public Page<SupportTicket> getUserTickets(String userId, Pageable pageable) {
        return ticketRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public Page<SupportTicket> getOpenTickets(Pageable pageable) {
        return ticketRepository.findByStatusOrderByCreatedAtDesc(SupportTicket.TicketStatus.OPEN, pageable);
    }

    public Map<String, Long> getTicketStats() {
        return Map.of(
                "open", ticketRepository.countByStatus(SupportTicket.TicketStatus.OPEN),
                "inProgress", ticketRepository.countByStatus(SupportTicket.TicketStatus.IN_PROGRESS),
                "resolved", ticketRepository.countByStatus(SupportTicket.TicketStatus.RESOLVED));
    }
}
