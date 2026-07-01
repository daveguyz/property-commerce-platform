package com.propertycommerce.apigateway.filter;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import javax.crypto.SecretKey;
import java.util.*;

@Component @Slf4j
public class JwtAuthGatewayFilter extends AbstractGatewayFilterFactory<JwtAuthGatewayFilter.Config> {
    @Value("${pcp.jwt.secret}") private String jwtSecret;

    private static final List<String> PUBLIC_PATHS = List.of(
        "/api/v1/auth/", "/api/v1/properties/search", "/api/v1/ai/concierge/public",
        "/api/v1/ai/area-intelligence", "/api/v1/ai/calendar-insights",
        "/api/v1/auth/api-keys/validate",  // internal — ApiKeyGatewayFilter calls this
        "/api/v1/auctions",                 // public lot listing
        "/api/v1/auctions/live",             // public live lots
        "/actuator/", "/webhook/", "/v3/api-docs", "/swagger-ui");

    public JwtAuthGatewayFilter() { super(Config.class); }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();
            if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) return chain.filter(exchange);

            String token = extractToken(exchange.getRequest());
            if (token == null) return unauthorized(exchange, "Missing Authorization header");

            try {
                SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret));
                Claims claims = Jwts.parser().verifyWith(key).build()
                        .parseSignedClaims(token).getPayload();
                ServerHttpRequest mutated = exchange.getRequest().mutate()
                        .header("X-User-Id", claims.getSubject())
                        .header("X-User-Email", claims.get("email", String.class) != null ? claims.get("email", String.class) : "")
                        .header("X-User-Roles", String.join(",", claims.get("roles", List.class) != null ? claims.get("roles", List.class) : List.of()))
                        .build();
                return chain.filter(exchange.mutate().request(mutated).build());
            } catch (ExpiredJwtException e) {
                return unauthorized(exchange, "Token expired");
            } catch (Exception e) {
                return unauthorized(exchange, "Invalid token");
            }
        };
    }

    private String extractToken(ServerHttpRequest req) {
        String auth = req.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        return (auth != null && auth.startsWith("Bearer ")) ? auth.substring(7) : null;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String reason) {
        ServerHttpResponse resp = exchange.getResponse();
        resp.setStatusCode(HttpStatus.UNAUTHORIZED);
        resp.getHeaders().add("X-Auth-Error", reason);
        return resp.setComplete();
    }

    public static class Config {}
}
