package com.staysphere.auctionservice.repository;

import com.staysphere.auctionservice.model.KycRecord;
import com.staysphere.auctionservice.model.KycStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KycRecordRepository extends JpaRepository<KycRecord, String> {

    // Most recent KYC record for a user
    Optional<KycRecord> findTopByUserIdOrderByCreatedAtDesc(String userId);

    // Verified record (there can only be one active verified record per user)
    Optional<KycRecord> findByUserIdAndStatus(String userId, KycStatus status);

    Optional<KycRecord> findByStripeSessionId(String stripeSessionId);

    List<KycRecord> findByUserIdOrderByCreatedAtDesc(String userId);

    boolean existsByUserIdAndStatus(String userId, KycStatus status);
}
