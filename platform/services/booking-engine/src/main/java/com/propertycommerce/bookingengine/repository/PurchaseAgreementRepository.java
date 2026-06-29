package com.propertycommerce.bookingengine.repository;

import com.propertycommerce.bookingengine.model.AgreementStatus;
import com.propertycommerce.bookingengine.model.PurchaseAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PurchaseAgreementRepository extends JpaRepository<PurchaseAgreement, String> {

    Optional<PurchaseAgreement> findByLotId(String lotId);

    Optional<PurchaseAgreement> findByBuyerSigningTokenHash(String hash);
    Optional<PurchaseAgreement> findBySellerSigningTokenHash(String hash);

    List<PurchaseAgreement> findByWinnerIdOrderByCreatedAtDesc(String winnerId);
    List<PurchaseAgreement> findBySellerIdOrderByCreatedAtDesc(String sellerId);

    /** Agreements past payment deadline still in FULLY_EXECUTED (not defaulted or paid). */
    @Query("""
            SELECT a FROM PurchaseAgreement a
            WHERE a.status = 'FULLY_EXECUTED'
              AND a.paymentDeadline < :now
              AND a.paymentConfirmedAt IS NULL
            """)
    List<PurchaseAgreement> findOverdueAgreements(@Param("now") LocalDateTime now);

    /** Signing tokens expiring within next 24h — used by reminder scheduler. */
    @Query("""
            SELECT a FROM PurchaseAgreement a
            WHERE a.status = 'SENT'
              AND a.signingTokensExpireAt BETWEEN :now AND :in24h
            """)
    List<PurchaseAgreement> findExpiringSoon(
            @Param("now") LocalDateTime now,
            @Param("in24h") LocalDateTime in24h);
}
