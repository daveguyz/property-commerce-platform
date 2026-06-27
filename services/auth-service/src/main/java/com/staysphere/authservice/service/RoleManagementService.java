package com.staysphere.authservice.service;

import com.staysphere.authservice.model.User;
import com.staysphere.authservice.repository.UserRepository;
import com.staysphere.shared.events.RoleAssignedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Manages role assignment on User accounts.
 *
 * Recognised platform roles:
 *   GUEST          — default, browse only
 *   HOST_PENDING   — awaiting agent approval
 *   host           — verified real-estate agent (Shopify tag: host)
 *   auctioneer     — manages specific auction lots (Shopify tag: auctioneer)
 *   admin          — platform administrator
 *   superadmin     — full platform access
 *
 * On every grant/revoke a RoleAssignedEvent is published to Kafka topic
 * "auth.role.assigned". The shopify-integration service consumes this event
 * and syncs the corresponding Shopify customer tag.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RoleManagementService {

    private final UserRepository userRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /** Roles that may be assigned via the admin API. */
    private static final Set<String> ASSIGNABLE_ROLES = Set.of(
            "host", "HOST_PENDING", "auctioneer", "admin", "superadmin"
    );

    /**
     * Grant a role to a user. Idempotent — no-op if already held.
     *
     * @param targetUserId  user to modify
     * @param role          role string to add
     * @param adminUserId   admin performing the action (audit trail)
     */
    @Transactional
    public User grantRole(String targetUserId, String role, String adminUserId) {
        validateRole(role);
        User user = findUser(targetUserId);

        boolean added = user.getRoles().add(role);
        User saved = userRepository.save(user);

        if (added) {
            publishEvent(saved, role, "GRANTED", adminUserId);
            log.info("[Roles] Granted '{}' to user {} by admin {}", role, targetUserId, adminUserId);
        } else {
            log.debug("[Roles] User {} already has role '{}' — no-op", targetUserId, role);
        }
        return saved;
    }

    /**
     * Revoke a role from a user. Idempotent — no-op if not held.
     * GUEST cannot be revoked (it is the minimum role).
     *
     * @param targetUserId  user to modify
     * @param role          role string to remove
     * @param adminUserId   admin performing the action
     */
    @Transactional
    public User revokeRole(String targetUserId, String role, String adminUserId) {
        validateRole(role);
        User user = findUser(targetUserId);

        boolean removed = user.getRoles().remove(role);

        // Ensure user always has at least GUEST
        if (user.getRoles().isEmpty()) {
            user.getRoles().add("GUEST");
        }

        User saved = userRepository.save(user);

        if (removed) {
            publishEvent(saved, role, "REVOKED", adminUserId);
            log.info("[Roles] Revoked '{}' from user {} by admin {}", role, targetUserId, adminUserId);
        } else {
            log.debug("[Roles] User {} did not have role '{}' — no-op", targetUserId, role);
        }
        return saved;
    }

    /**
     * Returns the current roles for a user. Read-only.
     */
    public Set<String> getRoles(String userId) {
        return findUser(userId).getRoles();
    }

    // ─── Private helpers ──────────────────────────────────────────────────

    private User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    private void validateRole(String role) {
        if (!ASSIGNABLE_ROLES.contains(role)) {
            throw new IllegalArgumentException(
                    "Role '" + role + "' is not assignable. Valid roles: " + ASSIGNABLE_ROLES);
        }
    }

    private void publishEvent(User user, String role, String action, String adminUserId) {
        kafkaTemplate.send(RoleAssignedEvent.TOPIC, RoleAssignedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .userId(user.getId())
                .email(user.getEmail())
                .role(role)
                .action(action)
                .assignedBy(adminUserId)
                .shopifyCustomerId(user.getShopifyCustomerId())
                .occurredAt(LocalDateTime.now())
                .build());
    }
}
