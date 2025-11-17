package com.castor.gateway.filter;

import com.castor.gateway.config.GatewayProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    @Mock
    private GatewayFilterChain chain;

    private GatewayProperties gatewayProperties;
    private RateLimitingFilter rateLimitingFilter;

    @BeforeEach
    void setUp() {
        gatewayProperties = new GatewayProperties();
        gatewayProperties.getRateLimiting().setEnabled(true);
        gatewayProperties.getRateLimiting().setDefaultLimit(5); // Small limit for testing
        gatewayProperties.getRateLimiting().setDefaultRefreshPeriod(60);

        rateLimitingFilter = new RateLimitingFilter(gatewayProperties);
    }

    @Test
    void shouldAllowRequestWhenUnderLimit() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/clientes").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        Route route = Route.async()
                .id("test-route")
                .uri(URI.create("http://localhost:8081"))
                .predicate(serverWebExchange -> true)
                .metadata(createMetadata(10))
                .build();

        exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, route);

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = rateLimitingFilter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(chain).filter(exchange);
        assertNotNull(exchange.getResponse().getHeaders().get("X-RateLimit-Remaining"));
    }

    @Test
    void shouldRejectRequestWhenRateLimitExceeded() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/clientes").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        Route route = Route.async()
                .id("test-route")
                .uri(URI.create("http://localhost:8081"))
                .predicate(serverWebExchange -> true)
                .metadata(createMetadata(2)) // Very low limit
                .build();

        exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, route);

        when(chain.filter(any())).thenReturn(Mono.empty());

        // Make requests to exceed limit
        rateLimitingFilter.filter(exchange, chain).block();
        rateLimitingFilter.filter(exchange, chain).block();

        // This should be rejected
        Mono<Void> result = rateLimitingFilter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldBypassWhenRateLimitingDisabled() {
        // Arrange
        gatewayProperties.getRateLimiting().setEnabled(false);
        rateLimitingFilter = new RateLimitingFilter(gatewayProperties);

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/clientes").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = rateLimitingFilter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(chain).filter(exchange);
    }

    private Map<String, Object> createMetadata(int rateLimit) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("rateLimit", rateLimit);
        metadata.put("service", "test-service");
        return metadata;
    }
}
