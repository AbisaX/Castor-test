package com.castor.gateway.filter;

import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoggingFilter implements GlobalFilter, Ordered {

    private final Tracer tracer;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Instant startTime = Instant.now();
        ServerHttpRequest request = exchange.getRequest();

        // Log incoming request
        logRequest(exchange);

        return chain.filter(exchange)
            .doOnSuccess(aVoid -> logResponse(exchange, startTime))
            .doOnError(throwable -> logError(exchange, startTime, throwable));
    }

    private void logRequest(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);

        String traceId = getTraceId();
        String routeId = route != null ? route.getId() : "unknown";
        String method = request.getMethod().name();
        String path = request.getPath().value();
        String queryParams = request.getURI().getQuery();
        String clientIp = getClientIp(request);

        log.info("[{}] Incoming Request - Route: {}, Method: {}, Path: {}, Query: {}, Client: {}",
            traceId, routeId, method, path, queryParams != null ? queryParams : "none", clientIp);

        // Log request headers (only in debug mode)
        if (log.isDebugEnabled()) {
            HttpHeaders headers = request.getHeaders();
            log.debug("[{}] Request Headers: {}", traceId, headers);
        }
    }

    private void logResponse(ServerWebExchange exchange, Instant startTime) {
        ServerHttpResponse response = exchange.getResponse();
        Duration duration = Duration.between(startTime, Instant.now());

        String traceId = getTraceId();
        int statusCode = response.getStatusCode() != null ? response.getStatusCode().value() : 0;
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();

        log.info("[{}] Outgoing Response - Method: {}, Path: {}, Status: {}, Duration: {}ms",
            traceId, method, path, statusCode, duration.toMillis());

        // Log slow requests
        if (duration.toMillis() > 1000) {
            log.warn("[{}] Slow request detected - Path: {}, Duration: {}ms",
                traceId, path, duration.toMillis());
        }

        // Log response headers (only in debug mode)
        if (log.isDebugEnabled()) {
            HttpHeaders headers = response.getHeaders();
            log.debug("[{}] Response Headers: {}", traceId, headers);
        }
    }

    private void logError(ServerWebExchange exchange, Instant startTime, Throwable throwable) {
        Duration duration = Duration.between(startTime, Instant.now());

        String traceId = getTraceId();
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();

        log.error("[{}] Request Error - Method: {}, Path: {}, Duration: {}ms, Error: {}",
            traceId, method, path, duration.toMillis(), throwable.getMessage(), throwable);
    }

    private String getTraceId() {
        if (tracer != null && tracer.currentSpan() != null) {
            return tracer.currentSpan().context().traceId();
        }
        return "no-trace";
    }

    private String getClientIp(ServerHttpRequest request) {
        // Check X-Forwarded-For header first
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        // Check X-Real-IP header
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        // Fall back to remote address
        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }

        return "unknown";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE; // Execute first
    }
}
