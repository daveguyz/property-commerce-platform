package com.propertycommerce.bookingengine.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Records the purchase agreement generated after an auction closes
 * with a winning bid. Tracks the e-signature lifecycle.
 *
 * Lifecycle:
 *   DRAFT → SENT → BUYER_SIGNED → FULLY_EXECUTED
 *                               → DEFAULTED (payment not received by deadline)
 *
 * On DEFAULTED: seller may offer the lot to the next highest bidder.
 * On FULLY_EXECUTED: conveyancing workflow begins.
 *
 * E-signature tokens: time-limited UUID tokens are emailed to each party.
 * Signatures are stored as SHA-256 hashes of (signatureData + timestamp + userId).
 */
@Entity
@Table(name = "purchase_agreements", indexes = {
    @Index(name = "idx_pa_lot",    columnList = "lot_id"),
    @Index(name = "idx_pa_winner", columnList = "winner_id"),
    @Index(name = "idx_pa_status", columnList = "status")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PurchaseAgreement {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "lot_id", nullable = false, unique = true)
    private String lotId;               // one agreement per lot

    @Column(name = "property_id", nullable = false)
    private String propertyId;

    @Column(name = "lot_title")
    private String lotTitle;

    @Column(name = "winner_id", nullable = false)
    private String winnerId;

    @Column(name = "winner_email")
    private String winnerEmail;

    @Column(name = "seller_id", nullable = false)
    private String sellerId;

    @Column(name = "seller_email")
    private String sellerEmail;

    @Column(name = "winning_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal winningAmount;

    @Column(name = "deposit_amount", precision = 14, scale = 2)
    private BigDecimal depositAmount;

    @Column(name = "balance_due", precision = 14, scale = 2)
    private BigDecimal balanceDue;     // winningAmount - depositAmount

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AgreementStatus status = AgreementStatus.DRAFT;

    // ── E-signature lifecycle ─────────────────────────────────────────
    /** SHA-256(buyerToken) — token emailed to buyer, not stored plain */
    @Column(name = "buyer_signing_token_hash", length = 64)
    private String buyerSigningTokenHash;

    /** SHA-256(sellerToken) — token emailed to seller */
    @Column(name = "seller_signing_token_hash", length = 64)
    private String sellerSigningTokenHash;

    /** Signing tokens expire 72 hours after sending */
    @Column(name = "signing_tokens_expire_at")
    private LocalDateTime signingTokensExpireAt;

    /** SHA-256(signatureData + timestamp + winnerId) */
    @Column(name = "buyer_signature_hash", length = 64)
    private String buyerSignatureHash;

    @Column(name = "buyer_signed_at")
    private LocalDateTime buyerSignedAt;

    /** SHA-256(signatureData + timestamp + sellerId) */
    @Column(name = "seller_signature_hash", length = 64)
    private String sellerSignatureHash;

    @Column(name = "seller_signed_at")
    private LocalDateTime sellerSignedAt;

    @Column(name = "fully_executed_at")
    private LocalDateTime fullyExecutedAt;

    // ── Payment deadline ──────────────────────────────────────────────
    @Column(name = "payment_deadline", nullable = false)
    private LocalDateTime paymentDeadline;   // auctionClosedAt + paymentDeadlineDays

    @Column(name = "payment_deadline_days")
    @Builder.Default
    private Integer paymentDeadlineDays = 10;

    @Column(name = "payment_confirmed_at")
    private LocalDateTime paymentConfirmedAt;

    // ── Default handling ──────────────────────────────────────────────
    @Column(name = "defaulted_at")
    private LocalDateTime defaultedAt;

    @Column(name = "next_bidder_offered")
    @Builder.Default
    private Boolean nextBidderOffered = false;

    @Column(name = "next_bidder_id")
    private String nextBidderId;

    // ── Conveyancing ──────────────────────────────────────────────────
    @Column(name = "conveyancing_initiated_at")
    private LocalDateTime conveyancingInitiatedAt;

    @Column(name = "conveyancer_ref")
    private String conveyancerRef;   // external conveyancer reference number

    // ── Timestamps ────────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
