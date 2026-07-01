package com.propertycommerce.webhookrouter.repository;

import com.propertycommerce.webhookrouter.model.WebhookDelivery;
import com.propertycommerce.webhookrouter.model.WebhookDelivery.DeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, String> {

    Page<WebhookDelivery> findByEndpointIdOrderByCreatedAtDesc(String endpointId, Pageable pageable);

    /** All FAILED deliveries that are due for retry. */
    @Query("""
            SELECT d FROM WebhookDelivery d
            WHERE d.status = 'FAILED'
              AND d.attemptCount < 4
              AND d.nextRetryAt <= :now
            ORDER BY d.nextRetryAt
            """)
    List<WebhookDelivery> findDueForRetry(@org.springframework.data.repository.query.Param("now") LocalDateTime now);
}
