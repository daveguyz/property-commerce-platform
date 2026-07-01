package com.propertycommerce.webhookrouter.service;

import com.propertycommerce.webhookrouter.model.WebhookEndpoint;
import com.propertycommerce.webhookrouter.repository.WebhookEndpointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WebhookEndpointService {

    private final WebhookEndpointRepository endpointRepo;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public WebhookEndpoint register(String tenantId, String url, String eventFilter) {
        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        String signingSecret = HexFormat.of().formatHex(raw);

        WebhookEndpoint endpoint = WebhookEndpoint.builder()
                .tenantId(tenantId)
                .url(url)
                .signingSecret(signingSecret)
                .eventFilter(eventFilter)
                .active(true)
                .build();
        return endpointRepo.save(endpoint);
    }

    public List<WebhookEndpoint> listForTenant(String tenantId) {
        return endpointRepo.findByTenantIdAndActiveTrue(tenantId);
    }

    @Transactional
    public void delete(String tenantId, String endpointId) {
        endpointRepo.findById(endpointId).ifPresent(e -> {
            if (e.getTenantId().equals(tenantId)) {
                endpointRepo.disableEndpoint(endpointId);
            }
        });
    }
}
