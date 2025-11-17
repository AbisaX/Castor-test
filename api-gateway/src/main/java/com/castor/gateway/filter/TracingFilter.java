package com.castor.gateway.filter;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

@Slf4j
@Component
@RequiredArgsConstructor
public class TracingFilter implements GlobalFilter, Ordered {

    private final Tracer tracer;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Span currentSpan = tracer.currentSpan();

        if (currentSpan != null) {
            Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
            String routeId = route != null ? route.getId() : "unknown";
            String requestPath = exchange.getRequest().getPath().value();
            String method = exchange.getRequest().getMethod().name();

            // Add span tags
            currentSpan.tag("gateway.route", routeId);
            currentSpan.tag("http.method", method);
            currentSpan.tag("http.path", requestPath);
            currentSpan.tag("component", "api-gateway");

            if (route != null) {
                String serviceName = (String) route.getMetadata().getOrDefault("service", "unknown");
                currentSpan.tag("downstream.service", serviceName);
                currentSpan.tag("downstream.uri", route.getUri().toString());
            }

            // Extract trace information
            String traceId = currentSpan.context().traceId();
            String spanId = currentSpan.context().spanId();

            // Add trace headers to response
            exchange.getResponse().getHeaders().add("X-Trace-Id", traceId);
            exchange.getResponse().getHeaders().add("X-Span-Id", spanId);

            // Add trace headers to downstream request
            exchange.getRequest().mutate()
                .header("X-Trace-Id", traceId)
                .header("X-Span-Id", spanId)
                .build();

            log.debug("Trace information - TraceId: {}, SpanId: {}, Route: {}, Path: {}",
                traceId, spanId, routeId, requestPath);

            // Create a new span for the gateway operation
            Span gatewaySpan = tracer.nextSpan(currentSpan).name("gateway-request");
            gatewaySpan.tag("request.path", requestPath);
            gatewaySpan.tag("request.method", method);
            gatewaySpan.start();

            return chain.filter(exchange)
                .doFinally(signalType -> {
                    // Add response information to span
                    if (exchange.getResponse().getStatusCode() != null) {
                        gatewaySpan.tag("http.status_code",
                            String.valueOf(exchange.getResponse().getStatusCode().value()));
                    }
                    gatewaySpan.end();
                })
                .contextWrite(context -> context.put(Span.class, gatewaySpan));
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -25; // Execute after metrics filter
    }
}
