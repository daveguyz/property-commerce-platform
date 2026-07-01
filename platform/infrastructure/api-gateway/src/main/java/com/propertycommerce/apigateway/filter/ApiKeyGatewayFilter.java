package com.propertycommerce.apigateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Phase C — API key authentication for the API gateway.
 *
 * Applied to routes where a tenant may make server-to-server calls without
 * an end-user JWT. The WordPress plugin, VS Code extension, and the
 * webhook-router service all use this auth path.
 *
 * Flow:
 *   1. Request arrives with header:  X-Api-Key: pcp_live_abc123...
 *   2. This filter calls auth-service internally:
 *      POST /api/v1/auth/api-keys/validate { "key": "pcp_live_abc123..." }
 *   3. On valid response: inject X-Tenant-Id and X-User-Roles headers,
 *      then pass to the downstream service.
 *   4. On invalid: return 401.
 *
 * This filter is only applied to routes that opt in (see application.yml).
 * Routes that use JwtAuthGatewayFilter do not use this filter — a request
 * must have either a Bearer JWT or an X-Api-Key, not both.
 *
 * Note: the auth-service validation endpoint is on PUBLIC_PATHS in
 * JwtAuthGatewayFilter, so this filter can call it without a JWT.
 */
@Component
@Slf4j
public class ApiKeyGatewayFilter
        extends AbstractGatewayFilterFactory<ApiKeyGatewayFilter.Config> {

    private static final String API_KEY_HEADER = "X-Api-Key";
    private final WebClient webClient;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${pcp.auth-service.internal-url:http://auth-service}")
    private String authServiceUrl;

    public ApiKeyGatewayFilter() {
        super(Config.class);
        this.webClient = WebClient.builder().build();
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String apiKey = exchange.getRequest().getHeaders().getFirst(API_KEY_HEADER);

            if (apiKey == null || apiKey.isBlank()) {
                return unauthorized(exchange, "Missing X-Api-Key header");
            }

            return webClient.post()
                    .uri(authServiceUrl + "/api/v1/auth/api-keys/validate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"key\":\"" + apiKey.replace("\"", "") + "\"}")
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(),
                            resp -> Mono.error(new RuntimeException("Invalid key")))
                    .bodyToMono(String.class)
                    .flatMap(body -> {
                        try {
                            JsonNode root = mapper.readTree(body);
                            String tenantId = root.at("/data/tenantId").asText("");
                            String roles = root.at("/data/roles").toString()
                                    .replace("[\"", "").replace("\"]", "")
                                    .replace("\",\"", ",");

                            var mutated = exchange.getRequest().mutate()
                                    .header("X-Tenant-Id", tenantId)
                                    .header("X-User-Roles", roles)
                                    .header("X-Auth-Method", "API_KEY")
                                    .build();
                            return chain.filter(exchange.mutate().request(mutated).build());
                        } catch (Exception e) {
                            log.warn("[ApiKeyFilter] Failed to parse auth response: {}", e.getMessage());
                            return unauthorized(exchange, "API key validation error");
                        }
                    })
                    .onErrorResume(e -> unauthorized(exchange, "Invalid or revoked API key"));
        };
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String reason) {
        var resp = exchange.getResponse();
        resp.setStatusCode(HttpStatus.UNAUTHORIZED);
        resp.getHeaders().add("X-Auth-Error", reason);
        return resp.setComplete();
    }

    public static class Config {}
}
