package com.propertycommerce.authservice.repository;

import com.propertycommerce.authservice.model.TenantApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TenantApiKeyRepository extends JpaRepository<TenantApiKey, String> {

    Optional<TenantApiKey> findByKeyHashAndActiveTrue(String keyHash);

    List<TenantApiKey> findByTenantIdAndActiveTrueOrderByCreatedAtDesc(String tenantId);

    /** Touch last_used_at without loading the entity — called on every valid API request. */
    @Modifying
    @Query("UPDATE TenantApiKey k SET k.lastUsedAt = :now WHERE k.id = :id")
    void touchLastUsed(String id, @org.springframework.data.repository.query.Param("now") LocalDateTime now);
}
