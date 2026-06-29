package com.propertycommerce.apigateway.filter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.time.Duration;

@Component @Slf4j
public class RateLimitFilter implements GlobalFilter, Ordered {
    private final ReactiveRedisTemplate<String, Long> redisTemplate;
    private static final int MAX_REQUESTS = 120;
    private static final int AI_MAX_REQUESTS = 20;

    public RateLimitFilter(ReactiveRedisTemplate<String, Long> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String ip = getClientIp(exchange);
        boolean isAi = exchange.getRequest().getURI().getPath().startsWith("/api/v1/ai/");
        int limit = isAi ? AI_MAX_REQUESTS : MAX_REQUESTS;
        String key = "ratelimit:" + ip + ":" + (isAi ? "ai" : "general");

        return redisTemplate.opsForValue().increment(key)
                .flatMap(count -> {
                    if (count == 1) redisTemplate.expire(key, Duration.ofMinutes(1)).subscribe();
                    if (count > limit) {
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        exchange.getResponse().getHeaders().add("Retry-After", "60");
                        return exchange.getResponse().setComplete();
                    }
                    exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", String.valueOf(limit - count));
                    return chain.filter(exchange);
                })
                .onErrorResume(e -> { log.warn("Rate limit Redis error: {}", e.getMessage()); return chain.filter(exchange); });
    }

    private String getClientIp(ServerWebExchange e) {
        String xff = e.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xff != null) return xff.split(",")[0].trim();
        return e.getRequest().getRemoteAddress() != null ? e.getRequest().getRemoteAddress().getHostString() : "unknown";
    }

    @Override public int getOrder() { return -1; }
}
