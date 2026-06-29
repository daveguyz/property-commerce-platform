package com.propertycommerce.auctionservice.service;

import com.propertycommerce.auctionservice.model.*;
import com.propertycommerce.auctionservice.repository.*;
import com.propertycommerce.shared.events.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Manages the lot Q&A mechanism.
 *
 * Bidder flow:
 *   submitQuestion() → auctioneer is notified → answerQuestion() or dismissQuestion()
 *
 * Escalation flow:
 *   escalateQuestion() → LotQuestionEscalatedEvent → messaging-service creates SupportTicket
 *
 * Privacy rules enforced here (not at the HTTP layer):
 *   - Bidders may only see their own questions and public answers.
 *   - Flagged questions are hidden from the bidder (silent moderation).
 *   - Only the auctioneer or seller of the lot may answer / dismiss / flag / escalate.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LotQuestionService {

    private final LotQuestionRepository questionRepository;
    private final AuctionLotRepository  lotRepository;
    private final AuctioneerAssignmentService assignmentService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final int MAX_CONTENT_LENGTH = 500;
    private static final int CONTENT_PREVIEW_LENGTH = 80;

    // ═══════════════════════════════════════════════════════════
    // BIDDER — submit a question
    // ═══════════════════════════════════════════════════════════

    /**
     * Submit a question on a lot.
     *
     * Validates:
     *   - Lot exists and is in a state that accepts questions (SCHEDULED/OPEN/EXTENDED)
     *   - Content is non-empty and within the 500-character limit
     *
     * Assigns a stable pseudonym ("Bidder #N") per bidder per lot.
     * Fires LotQuestionSubmittedEvent to Kafka.
     *
     * @param lotId     the auction lot
     * @param bidderId  authenticated user ID
     * @param email     bidder email for notifications
     * @param content   the question text (max 500 chars)
     * @param category  QuestionCategory enum string
     */
    @Transactional
    public LotQuestion submitQuestion(String lotId, String bidderId, String email,
                                      String content, String categoryStr) {

        AuctionLot lot = findLotOrThrow(lotId);
        assertLotAcceptsQuestions(lot);
        validateContent(content);

        QuestionCategory category = parseCategory(categoryStr);
        String displayName = resolveDisplayName(lotId, bidderId);

        LotQuestion question = LotQuestion.builder()
                .lotId(lotId)
                .bidderId(bidderId)
                .bidderEmail(email)
                .bidderDisplayName(displayName)
                .content(content.trim())
                .status(QuestionStatus.PENDING)
                .visibility(QuestionVisibility.PRIVATE)
                .category(category)
                .priority(QuestionPriority.NORMAL)
                .build();

        LotQuestion saved = questionRepository.save(question);
        log.info("[Q&A] Lot {} — {} ({}) submitted question {}",
                lotId, displayName, bidderId, saved.getId());

        // Notify auctioneer via Kafka (notification-service sends email)
        kafkaTemplate.send(LotQuestionSubmittedEvent.TOPIC, LotQuestionSubmittedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .questionId(saved.getId())
                .lotId(lotId)
                .bidderId(bidderId)
                .bidderDisplayName(displayName)
                .auctioneerId(lot.getAuctioneerId())
                .sellerId(lot.getSellerId())
                .category(category.name())
                .contentPreview(preview(content))
                .submittedAt(saved.getSubmittedAt())
                .build());

        return saved;
    }

    // ═══════════════════════════════════════════════════════════
    // AUCTIONEER — queue management
    // ═══════════════════════════════════════════════════════════

    /**
     * Retrieve the question queue for an auctioneer.
     *
     * @param lotId      the auction lot
     * @param callerId   must be auctioneer or seller of the lot
     * @param status     null = all, otherwise filter by QuestionStatus
     * @param sortUrgent if true, URGENT questions sort to top
     * @param pageable   pagination
     */
    public Page<LotQuestion> getQueue(String lotId, String callerId,
                                      String status, boolean sortUrgent,
                                      Pageable pageable) {
        assertIsAuctioneerOrSeller(lotId, callerId);

        if (status == null || "ALL".equalsIgnoreCase(status)) {
            return questionRepository.findByLotIdOrderBySubmittedAtDesc(lotId, pageable);
        }

        QuestionStatus qs = QuestionStatus.valueOf(status.toUpperCase());

        if (sortUrgent) {
            return questionRepository.findByLotIdAndStatusOrderByPriorityThenSubmitted(
                    lotId, qs, pageable);
        }
        return questionRepository.findByLotIdAndStatusOrderBySubmittedAtDesc(lotId, qs, pageable);
    }

    /** Count of PENDING questions — used by the dashboard tab badge. */
    public long countPending(String lotId) {
        return questionRepository.countByLotIdAndStatus(lotId, QuestionStatus.PENDING);
    }

    /**
     * Answer a question.
     *
     * @param questionId     the question to answer
     * @param callerId       must be auctioneer or seller
     * @param response       the answer text
     * @param answerPublicly if true, broadcasts to the whole room
     */
    @Transactional
    public LotQuestion answerQuestion(String questionId, String callerId,
                                       String response, boolean answerPublicly) {
        LotQuestion question = findQuestionOrThrow(questionId);
        assertIsAuctioneerOrSeller(question.getLotId(), callerId);

        if (response == null || response.isBlank()) {
            throw new IllegalArgumentException("Response text is required");
        }
        if (response.length() > 1000) {
            throw new IllegalArgumentException("Response must be 1000 characters or fewer");
        }

        question.setResponse(response.trim());
        question.setResponseBy(callerId);
        question.setRespondedAt(LocalDateTime.now());
        question.setStatus(QuestionStatus.ANSWERED);
        question.setAnsweredPublicly(answerPublicly);
        if (answerPublicly) {
            question.setVisibility(QuestionVisibility.PUBLIC);
        }

        LotQuestion saved = questionRepository.save(question);
        log.info("[Q&A] Question {} answered by {} (public={})", questionId, callerId, answerPublicly);

        // Only email the bidder for private answers (public answers are visible in the room)
        if (!answerPublicly) {
            kafkaTemplate.send(LotQuestionAnsweredEvent.TOPIC, LotQuestionAnsweredEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .questionId(questionId)
                    .lotId(question.getLotId())
                    .bidderId(question.getBidderId())
                    .bidderEmail(question.getBidderEmail())
                    .responsePreview(preview120(response))
                    .answeredPublicly(false)
                    .respondedAt(saved.getRespondedAt())
                    .build());
        }
        return saved;
    }

    /**
     * Dismiss a question without answering.
     * The bidder is NOT notified — the question simply disappears from pending.
     */
    @Transactional
    public LotQuestion dismissQuestion(String questionId, String callerId) {
        LotQuestion question = findQuestionOrThrow(questionId);
        assertIsAuctioneerOrSeller(question.getLotId(), callerId);

        question.setStatus(QuestionStatus.DISMISSED);
        LotQuestion saved = questionRepository.save(question);
        log.info("[Q&A] Question {} dismissed by {}", questionId, callerId);
        return saved;
    }

    /**
     * Escalate a question to platform support.
     * Creates a SupportTicket in messaging-service via Kafka.
     * The bidder receives a confirmation with a ticket reference.
     */
    @Transactional
    public LotQuestion escalateQuestion(String questionId, String callerId, String reason) {
        LotQuestion question = findQuestionOrThrow(questionId);
        assertIsAuctioneerOrSeller(question.getLotId(), callerId);

        question.setStatus(QuestionStatus.ESCALATED);
        question.setPriority(QuestionPriority.URGENT);
        LotQuestion saved = questionRepository.save(question);

        log.info("[Q&A] Question {} escalated by {} — reason: {}", questionId, callerId, reason);

        kafkaTemplate.send(LotQuestionEscalatedEvent.TOPIC, LotQuestionEscalatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .questionId(questionId)
                .lotId(question.getLotId())
                .bidderId(question.getBidderId())
                .bidderEmail(question.getBidderEmail())
                .reason(reason)
                .category(question.getCategory().name())
                .escalatedAt(LocalDateTime.now())
                .build());

        return saved;
    }

    /**
     * Flag a question as abusive (silent moderation).
     * The question disappears from the bidder's view immediately.
     * The bidder receives no notification that they were flagged.
     */
    @Transactional
    public LotQuestion flagAsAbusive(String questionId, String callerId) {
        LotQuestion question = findQuestionOrThrow(questionId);
        assertIsAuctioneerOrSeller(question.getLotId(), callerId);

        question.setFlaggedAsAbusive(true);
        question.setFlaggedAt(LocalDateTime.now());
        LotQuestion saved = questionRepository.save(question);
        log.info("[Q&A] Question {} flagged as abusive by {}", questionId, callerId);
        return saved;
    }

    // ═══════════════════════════════════════════════════════════
    // BIDDER — read own questions + public answers
    // ═══════════════════════════════════════════════════════════

    /**
     * Returns a bidder's own questions (all non-flagged) plus all public answers on the lot.
     * This is what the bidder sees in the auction room Q&A panel.
     */
    public BidderQuestionView getBidderView(String lotId, String bidderId) {
        List<LotQuestion> mine = questionRepository
                .findByLotIdAndBidderIdOrderBySubmittedAtDesc(lotId, bidderId)
                .stream()
                .filter(q -> !Boolean.TRUE.equals(q.getFlaggedAsAbusive()))
                .toList();

        List<LotQuestion> publicAnswers = questionRepository.findPublicAnswersForLot(lotId);

        return new BidderQuestionView(mine, publicAnswers);
    }

    /** Public answers only — no auth required (visible to all room attendees). */
    public List<LotQuestion> getPublicAnswers(String lotId) {
        return questionRepository.findPublicAnswersForLot(lotId);
    }

    // ═══════════════════════════════════════════════════════════
    // DTOs
    // ═══════════════════════════════════════════════════════════

    public record BidderQuestionView(
            List<LotQuestion> myQuestions,
            List<LotQuestion> publicAnswers
    ) {}

    // ═══════════════════════════════════════════════════════════
    // Private helpers
    // ═══════════════════════════════════════════════════════════

    private AuctionLot findLotOrThrow(String lotId) {
        return lotRepository.findById(lotId)
                .orElseThrow(() -> new IllegalArgumentException("Auction lot not found: " + lotId));
    }

    private LotQuestion findQuestionOrThrow(String questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));
    }

    private void assertLotAcceptsQuestions(AuctionLot lot) {
        AuctionLotStatus s = lot.getStatus();
        if (s != AuctionLotStatus.SCHEDULED
                && s != AuctionLotStatus.OPEN
                && s != AuctionLotStatus.EXTENDED) {
            throw new IllegalStateException(
                    "Questions can only be posted on SCHEDULED, OPEN or EXTENDED lots. Status: " + s);
        }
    }

    private void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Question content cannot be empty");
        }
        if (content.trim().length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException(
                    "Question must be " + MAX_CONTENT_LENGTH + " characters or fewer");
        }
    }

    private void assertIsAuctioneerOrSeller(String lotId, String userId) {
        if (!assignmentService.isAuctioneerOrSeller(lotId, userId)) {
            throw new SecurityException(
                    "Only the auctioneer or seller may manage questions for lot " + lotId);
        }
    }

    private QuestionCategory parseCategory(String categoryStr) {
        if (categoryStr == null || categoryStr.isBlank()) return QuestionCategory.GENERAL;
        try {
            return QuestionCategory.valueOf(categoryStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return QuestionCategory.GENERAL;
        }
    }

    /**
     * Assign a stable pseudonym for a bidder on a lot.
     * If the bidder has previously posted, return the existing name.
     * Otherwise assign the next sequential name ("Bidder #N").
     */
    private String resolveDisplayName(String lotId, String bidderId) {
        return questionRepository.findExistingDisplayName(lotId, bidderId)
                .orElseGet(() -> {
                    long position = questionRepository.countDistinctBiddersByLotId(lotId) + 1;
                    return "Bidder #" + position;
                });
    }

    private String preview(String text) {
        if (text == null) return "";
        return text.length() <= CONTENT_PREVIEW_LENGTH
                ? text : text.substring(0, CONTENT_PREVIEW_LENGTH) + "…";
    }

    private String preview120(String text) {
        if (text == null) return "";
        return text.length() <= 120 ? text : text.substring(0, 120) + "…";
    }
}
