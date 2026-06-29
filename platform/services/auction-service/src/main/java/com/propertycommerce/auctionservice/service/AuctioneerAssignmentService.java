package com.propertycommerce.auctionservice.service;

import com.propertycommerce.auctionservice.model.AuctionLot;
import com.propertycommerce.auctionservice.model.AuctionLotStatus;
import com.propertycommerce.auctionservice.repository.AuctionLotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Manages the assignment of an auctioneer to a specific lot.
 *
 * An auctioneer is a user who runs the live auction — they manage bid access
 * requests, answer bidder questions, and control the live room.
 * They are distinct from the seller (property owner / listing agent).
 *
 * A seller may assign themselves as auctioneer (auctioneerId == sellerId)
 * or delegate to a dedicated user who holds the 'auctioneer' role.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuctioneerAssignmentService {

    private final AuctionLotRepository lotRepository;

    /**
     * Assign an auctioneer to a lot.
     *
     * Permissions: caller must be the lot seller or an admin
     * (enforced at the controller layer via @PreAuthorize or header check).
     * Lot must be DRAFT or SCHEDULED — cannot reassign once OPEN.
     *
     * @param lotId          the auction lot
     * @param auctioneerId   user ID to assign (must already hold 'auctioneer' role —
     *                       verified by auth-service; not re-verified here to avoid
     *                       cross-service coupling in the hot path)
     * @param requesterId    seller or admin performing the assignment
     */
    @Transactional
    public AuctionLot assignAuctioneer(String lotId, String auctioneerId, String requesterId) {
        AuctionLot lot = findLotOrThrow(lotId);
        assertCanModify(lot, requesterId);

        if (lot.getStatus() == AuctionLotStatus.OPEN
                || lot.getStatus() == AuctionLotStatus.EXTENDED
                || lot.getStatus() == AuctionLotStatus.CLOSED
                || lot.getStatus() == AuctionLotStatus.SETTLED) {
            throw new IllegalStateException(
                    "Cannot change auctioneer once the lot is OPEN or closed. Status: "
                    + lot.getStatus());
        }

        String previous = lot.getAuctioneerId();
        lot.setAuctioneerId(auctioneerId);
        AuctionLot saved = lotRepository.save(lot);

        log.info("[AuctioneerAssignment] Lot {} assigned to auctioneer {} (was: {}) by {}",
                lotId, auctioneerId, previous, requesterId);
        return saved;
    }

    /**
     * Remove the auctioneer assignment from a lot.
     * After removal the seller manages the lot directly.
     */
    @Transactional
    public AuctionLot removeAuctioneer(String lotId, String requesterId) {
        AuctionLot lot = findLotOrThrow(lotId);
        assertCanModify(lot, requesterId);

        if (lot.getStatus() == AuctionLotStatus.OPEN
                || lot.getStatus() == AuctionLotStatus.EXTENDED) {
            throw new IllegalStateException("Cannot remove auctioneer from a live lot");
        }

        lot.setAuctioneerId(null);
        AuctionLot saved = lotRepository.save(lot);
        log.info("[AuctioneerAssignment] Auctioneer removed from lot {} by {}", lotId, requesterId);
        return saved;
    }

    /**
     * Get all lots assigned to a specific auctioneer, ordered by start time ascending.
     * Returns all statuses so the auctioneer can see past and upcoming lots.
     */
    public Page<AuctionLot> getLotsForAuctioneer(String auctioneerId, Pageable pageable) {
        return lotRepository.findByAuctioneerIdOrderByStartsAtAsc(auctioneerId, pageable);
    }

    /**
     * Get active or upcoming lots for an auctioneer (SCHEDULED, OPEN, EXTENDED).
     * Used by the auctioneer dashboard to show their current workload.
     */
    public List<AuctionLot> getActiveLotsForAuctioneer(String auctioneerId) {
        return lotRepository.findActiveLotsForAuctioneer(auctioneerId,
                List.of(AuctionLotStatus.SCHEDULED, AuctionLotStatus.OPEN, AuctionLotStatus.EXTENDED));
    }

    /**
     * Determine the role of a given user with respect to a specific lot.
     * Used by the frontend to decide which UI to render.
     *
     * Returns one of: "auctioneer", "seller", "both" (seller who is also auctioneer), "none"
     */
    public String getLotRole(String lotId, String userId) {
        AuctionLot lot = findLotOrThrow(lotId);
        boolean isSeller     = userId.equals(lot.getSellerId());
        boolean isAuctioneer = userId.equals(lot.getAuctioneerId());

        if (isSeller && isAuctioneer) return "both";
        if (isAuctioneer)             return "auctioneer";
        if (isSeller)                 return "seller";
        return "none";
    }

    /**
     * Check whether a user is the auctioneer or seller for a lot.
     * Used as a permission guard in LotQuestion and BidAccessRequest services.
     */
    public boolean isAuctioneerOrSeller(String lotId, String userId) {
        AuctionLot lot = findLotOrThrow(lotId);
        return userId.equals(lot.getAuctioneerId()) || userId.equals(lot.getSellerId());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private AuctionLot findLotOrThrow(String lotId) {
        return lotRepository.findById(lotId)
                .orElseThrow(() -> new IllegalArgumentException("Auction lot not found: " + lotId));
    }

    /**
     * Only the lot seller or an admin may change the auctioneer assignment.
     * Admin check is done at the HTTP layer via @PreAuthorize;
     * here we only enforce the seller check for non-admin callers.
     */
    private void assertCanModify(AuctionLot lot, String requesterId) {
        if (!lot.getSellerId().equals(requesterId)) {
            throw new SecurityException(
                    "Only the lot seller may change the auctioneer assignment. Lot: "
                    + lot.getId());
        }
    }
}
