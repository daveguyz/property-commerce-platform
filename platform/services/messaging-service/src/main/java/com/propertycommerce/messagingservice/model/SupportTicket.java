package com.propertycommerce.messagingservice.model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.*;
import java.time.*;

@Entity @Table(name = "support_tickets",
    indexes = { @Index(name = "idx_ticket_user", columnList = "user_id"),
                @Index(name = "idx_ticket_status", columnList = "status") })
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SupportTicket {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private String id;
    @Column(nullable = false) private String userId;
    private String bookingId, propertyId;
    @Column(nullable = false) private String subject;
    @Column(columnDefinition = "TEXT", nullable = false) private String description;
    @Enumerated(EnumType.STRING) @Builder.Default private TicketStatus status = TicketStatus.OPEN;
    @Enumerated(EnumType.STRING) @Builder.Default private TicketPriority priority = TicketPriority.NORMAL;
    @Enumerated(EnumType.STRING) private TicketCategory category;
    private String assignedAdminId;
    private String conversationId;
    private LocalDateTime resolvedAt;
    private String resolutionNotes;
    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp private LocalDateTime updatedAt;

    public enum TicketStatus { OPEN, IN_PROGRESS, WAITING_USER, RESOLVED, CLOSED }
    public enum TicketPriority { LOW, NORMAL, HIGH, URGENT }
    public enum TicketCategory { BOOKING_ISSUE, PAYMENT_ISSUE, PROPERTY_QUALITY,
        HOST_BEHAVIOUR, GUEST_BEHAVIOUR, REFUND_REQUEST, TECHNICAL, GENERAL }
}
