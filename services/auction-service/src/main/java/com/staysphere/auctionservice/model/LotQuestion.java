package com.staysphere.auctionservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * A question or comment posted by a qualified bidder on a specific lot.
 *
 * Lifecycle: PENDING → ANSWERED | DISMISSED | ESCALATED
 *
 * Privacy model:
 *   - The bidder always sees their own questions + any answers.
 *   - Other bidders see only questions where answeredPublicly = true.
 *   - The auctioneer and seller see all questions for their lots.
 *   - Flagged questions are hidden from the bidder (silent moderation).
 *
 * Pseudonymity:
 *   - bidderDisplayName is assigned at submission time ("Bidder #N")
 *     so the auctioneer can reference the bidder publicly without
 *     revealing their identity to other room participants.
 */
@Entity
@Table(name = "lot_questions", indexes = {
    @Index(name = "idx_lq_lot",       columnList = "lot_id"),
    @Index(name = "idx_lq_bidder",    columnList = "bidder_id"),
    @Index(name = "idx_lq_status",    columnList = "status"),
    @Index(name = "idx_lq_submitted", columnList = "submitted_at")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LotQuestion {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /** FK to auction_lots.id */
    @Column(name = "lot_id", nullable = false)
    private String lotId;

    /** User ID of the bidder who asked */
    @Column(name = "bidder_id", nullable = false)
    private String bidderId;

    /** Email for notification delivery */
    @Column(name = "bidder_email")
    private String bidderEmail;

    /**
     * Stable pseudonym for this bidder on this lot ("Bidder #4").
     * Derived from position in the BidAccessRequest queue.
     * Consistent for all questions the same bidder posts on the same lot.
     */
    @Column(name = "bidder_display_name", nullable = false, length = 20)
    private String bidderDisplayName;

    /** The question text — max 500 chars enforced at service layer */
    @Column(nullable = false, columnDefinition = "TEXT", length = 500)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private QuestionStatus status = QuestionStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private QuestionVisibility visibility = QuestionVisibility.PRIVATE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private QuestionCategory category = QuestionCategory.GENERAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private QuestionPriority priority = QuestionPriority.NORMAL;

    /** Auctioneer's response text */
    @Column(columnDefinition = "TEXT")
    private String response;

    /** User ID of the auctioneer who answered */
    @Column(name = "response_by")
    private String responseBy;

    private LocalDateTime respondedAt;

    /**
     * If true, the answer was broadcast to /topic/auction/{lotId}
     * and is visible to all room attendees.
     * If false, only the asking bidder can see the answer.
     */
    @Column(name = "answered_publicly")
    @Builder.Default
    private Boolean answeredPublicly = false;

    /**
     * Silent moderation flag. Flagged questions disappear from the bidder's
     * view immediately. The bidder is not notified that they were flagged.
     */
    @Builder.Default
    private Boolean flaggedAsAbusive = false;

    private LocalDateTime flaggedAt;

    /** Set on ESCALATED — references the SupportTicket created in messaging-service */
    @Column(name = "support_ticket_id")
    private String supportTicketId;

    @CreationTimestamp
    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
