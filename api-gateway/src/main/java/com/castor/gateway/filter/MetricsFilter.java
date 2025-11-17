package com.castor.gateway.filter;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

@Slf4j
@Component
@RequiredArgsConstructor
public class MetricsFilter implements GlobalFilter, Ordered {

    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, Counter> requestCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Timer> requestTimers = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Instant startTime = Instant.now();
        String requestPath = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();

        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        String routeId = route != null ? route.getId() : "unknown";
        String serviceName = route != null ?
            (String) route.getMetadata().getOrDefault("service", "unknown") : "unknown";

        // Increment request counter
        incrementRequestCounter(routeId, method, serviceName);

        return chain.filter(exchange).doFinally(signalType -> {
            Duration duration = Duration.between(startTime, Instant.now());
            HttpStatus statusCode = exchange.getResponse().getStatusCode();
            int status = statusCode != null ? statusCode.value() : 0;

            // Record request duration
            recordRequestDuration(routeId, method, serviceName, status, duration);

            // Log metrics
            log.debug("Request metrics - Route: {}, Method: {}, Status: {}, Duration: {}ms",
                routeId, method, status, duration.toMillis());

            // Additional metrics
            recordStatusMetrics(routeId, method, serviceName, status);
        });
    }

    private void incrementRequestCounter(String routeId, String method, String serviceName) {
        String counterKey = String.format("gateway.requests.total.%s.%s", routeId, method);

        Counter counter = requestCounters.computeIfAbsent(counterKey, k ->
            Counter.builder("gateway.requests.total")
                .description("Total number of requests through the gateway")
                .tag("route", routeId)
                .tag("method", method)
                .tag("service", serviceName)
                .register(meterRegistry)
        );

        counter.increment();
    }

    private void recordRequestDuration(String routeId, String method, String serviceName,
                                       int status, Duration duration) {
        String timerKey = String.format("gateway.requests.duration.%s.%s.%d", routeId, method, status);

        Timer timer = requestTimers.computeIfAbsent(timerKey, k ->
            Timer.builder("gateway.requests.duration")
                .description("Request duration through the gateway")
                .tag("route", routeId)
                .tag("method", method)
                .tag("service", serviceName)
                .tag("status", String.valueOf(status))
                .register(meterRegistry)
        );

        timer.record(duration);
    }

    private void recordStatusMetrics(String routeId, String method, String serviceName, int status) {
        String statusCategory = getStatusCategory(status);

        Counter.builder("gateway.requests.status")
            .description("Request status codes")
            .tag("route", routeId)
            .tag("method", method)
            .tag("service", serviceName)
            .tag("status", String.valueOf(status))
            .tag("status_category", statusCategory)
            .register(meterRegistry)
            .increment();
    }

    private String getStatusCategory(int status) {
        if (status >= 200 && status < 300) return "2xx_success";
        if (status >= 300 && status < 400) return "3xx_redirect";
        if (status >= 400 && status < 500) return "4xx_client_error";
        if (status >= 500 && status < 600) return "5xx_server_error";
        return "unknown";
    }

    @Override
    public int getOrder() {
        return -50; // Execute after rate limiting but before other filters
    }
}
