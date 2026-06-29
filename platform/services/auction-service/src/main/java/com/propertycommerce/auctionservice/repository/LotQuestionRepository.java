package com.propertycommerce.auctionservice.repository;

import com.propertycommerce.auctionservice.model.LotQuestion;
import com.propertycommerce.auctionservice.model.QuestionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LotQuestionRepository extends JpaRepository<LotQuestion, String> {

    // ── Auctioneer queue queries ──────────────────────────────────────────

    /** All questions for a lot, ordered by submission time descending. */
    Page<LotQuestion> findByLotIdOrderBySubmittedAtDesc(String lotId, Pageable pageable);

    /** Questions filtered by status, ordered newest first. */
    Page<LotQuestion> findByLotIdAndStatusOrderBySubmittedAtDesc(
            String lotId, QuestionStatus status, Pageable pageable);

    /** URGENT questions first, then newest — for the 'urgent first' sort option. */
    @Query("""
            SELECT q FROM LotQuestion q
            WHERE q.lotId = :lotId AND q.status = :status
            ORDER BY CASE q.priority WHEN 'URGENT' THEN 0 WHEN 'HIGH' THEN 1 ELSE 2 END,
                     q.submittedAt DESC
            """)
    Page<LotQuestion> findByLotIdAndStatusOrderByPriorityThenSubmitted(
            @Param("lotId") String lotId,
            @Param("status") QuestionStatus status,
            Pageable pageable);

    /** Count of PENDING questions — drives the tab badge. */
    long countByLotIdAndStatus(String lotId, QuestionStatus status);

    // ── Bidder view queries ───────────────────────────────────────────────

    /** A bidder's own questions on a lot (regardless of status or flag). */
    List<LotQuestion> findByLotIdAndBidderIdOrderBySubmittedAtDesc(String lotId, String bidderId);

    /**
     * Public answers — visible to all room attendees.
     * Excludes flagged questions regardless of answeredPublicly.
     */
    @Query("""
            SELECT q FROM LotQuestion q
            WHERE q.lotId = :lotId
              AND q.answeredPublicly = true
              AND q.flaggedAsAbusive = false
            ORDER BY q.respondedAt DESC
            """)
    List<LotQuestion> findPublicAnswersForLot(@Param("lotId") String lotId);

    // ── Pseudonym assignment ──────────────────────────────────────────────

    /**
     * Count distinct bidders who have already posted on this lot.
     * Used to assign the next sequential display name ("Bidder #N").
     */
    @Query("SELECT COUNT(DISTINCT q.bidderId) FROM LotQuestion q WHERE q.lotId = :lotId")
    long countDistinctBiddersByLotId(@Param("lotId") String lotId);

    /**
     * Check whether a bidder already has a display name on this lot.
     * Returns the existing name if so (ensures consistent pseudonymity).
     */
    @Query("""
            SELECT q.bidderDisplayName FROM LotQuestion q
            WHERE q.lotId = :lotId AND q.bidderId = :bidderId
            ORDER BY q.submittedAt ASC
            """)
    Optional<String> findExistingDisplayName(
            @Param("lotId") String lotId,
            @Param("bidderId") String bidderId);
}
