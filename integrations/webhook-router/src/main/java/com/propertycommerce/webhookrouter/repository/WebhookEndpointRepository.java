package com.propertycommerce.webhookrouter.repository;

import com.propertycommerce.webhookrouter.model.WebhookEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface WebhookEndpointRepository extends JpaRepository<WebhookEndpoint, String> {
    List<WebhookEndpoint> findByTenantIdAndActiveTrue(String tenantId);
    List<WebhookEndpoint> findByActiveTrue();

    @Modifying
    @Query("UPDATE WebhookEndpoint e SET e.failureCount = e.failureCount + 1 WHERE e.id = :id")
    void incrementFailureCount(String id);

    @Modifying
    @Query("UPDATE WebhookEndpoint e SET e.active = false, e.disabledAt = NOW() WHERE e.id = :id")
    void disableEndpoint(String id);
}
