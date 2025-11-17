package com.castor.gateway.filter;

import com.castor.gateway.config.GatewayProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter implements GlobalFilter, Ordered {

    private final GatewayProperties gatewayProperties;
    private final Map<String, RateLimitBucket> rateLimitBuckets = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!gatewayProperties.getRateLimiting().isEnabled()) {
            return chain.filter(exchange);
        }

        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        if (route == null) {
            return chain.filter(exchange);
        }

        String routeId = route.getId();
        String clientKey = getClientKey(exchange, routeId);

        // Get rate limit for this route
        int rateLimit = getRateLimitForRoute(route);
        int refreshPeriod = gatewayProperties.getRateLimiting().getDefaultRefreshPeriod();

        RateLimitBucket bucket = rateLimitBuckets.computeIfAbsent(clientKey,
            k -> new RateLimitBucket(rateLimit, refreshPeriod));

        if (!bucket.tryConsume()) {
            log.warn("Rate limit exceeded for client: {} on route: {}", clientKey, routeId);
            return handleRateLimitExceeded(exchange, bucket);
        }

        // Add rate limit headers
        exchange.getResponse().getHeaders().add("X-RateLimit-Remaining",
            String.valueOf(bucket.getTokensRemaining()));
        exchange.getResponse().getHeaders().add("X-RateLimit-Reset",
            String.valueOf(bucket.getResetTime()));
        exchange.getResponse().getHeaders().add("X-RateLimit-Limit",
            String.valueOf(rateLimit));

        log.debug("Request allowed for client: {} on route: {}. Tokens remaining: {}",
            clientKey, routeId, bucket.getTokensRemaining());

        return chain.filter(exchange);
    }

    private String getClientKey(ServerWebExchange exchange, String routeId) {
        String clientIp = exchange.getRequest().getRemoteAddress() != null ?
                exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "unknown";
        return routeId + ":" + clientIp;
    }

    private int getRateLimitForRoute(Route route) {
        Object rateLimitMetadata = route.getMetadata().get("rateLimit");
        if (rateLimitMetadata instanceof Integer) {
            return (Integer) rateLimitMetadata;
        }
        return gatewayProperties.getRateLimiting().getDefaultLimit();
    }

    private Mono<Void> handleRateLimitExceeded(ServerWebExchange exchange, RateLimitBucket bucket) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", "0");
        exchange.getResponse().getHeaders().add("X-RateLimit-Reset",
            String.valueOf(bucket.getResetTime()));
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String errorMessage = String.format(
            "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Please try again after %d seconds.\",\"status\":429}",
            bucket.getSecondsUntilReset()
        );

        return exchange.getResponse().writeWith(
            Mono.just(exchange.getResponse().bufferFactory().wrap(errorMessage.getBytes()))
        );
    }

    @Override
    public int getOrder() {
        return -100; // Execute before other filters
    }

    /**
     * Token bucket implementation for rate limiting
     */
    private static class RateLimitBucket {
        private final int capacity;
        private final int refillRate;
        private final long refillPeriodSeconds;
        private int tokens;
        private long lastRefillTimestamp;

        public RateLimitBucket(int capacity, long refillPeriodSeconds) {
            this.capacity = capacity;
            this.refillPeriodSeconds = refillPeriodSeconds;
            this.refillRate = capacity;
            this.tokens = capacity;
            this.lastRefillTimestamp = Instant.now().getEpochSecond();
        }

        public synchronized boolean tryConsume() {
            refill();
            if (tokens > 0) {
                tokens--;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = Instant.now().getEpochSecond();
            long timePassed = now - lastRefillTimestamp;

            if (timePassed >= refillPeriodSeconds) {
                tokens = capacity;
                lastRefillTimestamp = now;
            }
        }

        public synchronized int getTokensRemaining() {
            refill();
            return tokens;
        }

        public long getResetTime() {
            return lastRefillTimestamp + refillPeriodSeconds;
        }

        public long getSecondsUntilReset() {
            long now = Instant.now().getEpochSecond();
            long resetTime = getResetTime();
            return Math.max(0, resetTime - now);
        }
    }
}
