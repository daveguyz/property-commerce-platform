package com.propertycommerce.authservice.service;

import com.propertycommerce.authservice.model.TenantApiKey;
import com.propertycommerce.authservice.repository.TenantApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Issues, validates, revokes, and records usage of tenant API keys.
 *
 * The plaintext key is returned once at issuance and never retrievable again.
 * The gateway calls validateKey() on every request bearing X-Api-Key.
 * Validation is deliberately lightweight — a single indexed hash lookup,
 * returning enough claims (tenantId, roles) for the gateway to inject
 * the right headers downstream without hitting auth-service again.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TenantApiKeyService {

    private final TenantApiKeyRepository keyRepository;
    private static final SecureRandom RANDOM = new SecureRandom();

    // ── Issuance ──────────────────────────────────────────────────────────

    /**
     * Issue a new API key for a tenant.
     *
     * @param tenantId the tenant this key belongs to
     * @param label    human-readable label ("Production", "WordPress plugin")
     * @param test     true → pcp_test_ prefix; false → pcp_live_ prefix
     * @return IssuedKey record — the plaintext key is in this object only
     */
    @Transactional
    public IssuedKey issueKey(String tenantId, String label, boolean test) {
        byte[] raw = new byte[16];
        RANDOM.nextBytes(raw);
        String hex = HexFormat.of().formatHex(raw);
        String prefix = test ? "pcp_test_" : "pcp_live_";
        String plaintext = prefix + hex;
        String hash = sha256(plaintext);
        String displayPrefix = plaintext.substring(0, Math.min(16, plaintext.length())) + "…";

        TenantApiKey key = TenantApiKey.builder()
                .tenantId(tenantId)
                .label(label)
                .keyHash(hash)
                .keyPrefix(displayPrefix)
                .active(true)
                .build();

        keyRepository.save(key);
        log.info("[ApiKey] Issued {} key '{}' for tenant {}", test ? "test" : "live", label, tenantId);
        return new IssuedKey(key.getId(), plaintext, displayPrefix, key.getCreatedAt());
    }

    // ── Validation — called by API gateway on every X-Api-Key request ──────

    /**
     * Validate an API key and return the tenant claims if valid.
     * Touches lastUsedAt asynchronously so it doesn't block the request.
     *
     * @param plaintextKey the raw key from the X-Api-Key header
     * @return Optional containing tenant claims if valid, empty if not
     */
    @Transactional
    public Optional<KeyClaims> validateKey(String plaintextKey) {
        if (plaintextKey == null || plaintextKey.isBlank()) return Optional.empty();
        String hash = sha256(plaintextKey);
        return keyRepository.findByKeyHashAndActiveTrue(hash)
                .map(key -> {
                    keyRepository.touchLastUsed(key.getId(), LocalDateTime.now());
                    return new KeyClaims(key.getTenantId(), List.of("TENANT_SERVICE_ACCOUNT"));
                });
    }

    // ── Management ────────────────────────────────────────────────────────

    public List<TenantApiKey> listKeys(String tenantId) {
        return keyRepository.findByTenantIdAndActiveTrueOrderByCreatedAtDesc(tenantId);
    }

    @Transactional
    public void revokeKey(String keyId) {
        keyRepository.findById(keyId).ifPresent(key -> {
            key.setActive(false);
            key.setRevokedAt(LocalDateTime.now());
            keyRepository.save(key);
            log.info("[ApiKey] Key {} revoked for tenant {}", keyId, key.getTenantId());
        });
    }

    // ── DTOs ──────────────────────────────────────────────────────────────

    /**
     * Returned once from issueKey(). The plaintext must be delivered to the
     * caller immediately — it is never stored and cannot be retrieved again.
     */
    public record IssuedKey(
            String keyId,
            String plaintextKey,   // deliver to caller, never store
            String displayPrefix,  // safe to show in UI ("pcp_live_ab12…")
            java.time.LocalDateTime createdAt
    ) {}

    /**
     * Returned from validateKey(). Contains everything the API gateway
     * needs to inject X-Tenant-Id and X-User-Roles headers downstream.
     */
    public record KeyClaims(
            String tenantId,
            List<String> roles
    ) {}

    // ── Crypto ────────────────────────────────────────────────────────────

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    md.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
