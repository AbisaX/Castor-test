package com.castor.gateway.filter;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

/**
 * Test class for MetricsFilter.
 *
 * Tests the collection of metrics including:
 * - Request counter increments (gateway.requests.total)
 * - Request duration measurements (gateway.requests.duration)
 * - Status code categorization (2xx, 3xx, 4xx, 5xx)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MetricsFilter Tests")
class MetricsFilterTest {

    private MeterRegistry meterRegistry;
    private MetricsFilter metricsFilter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        // Use SimpleMeterRegistry for testing
        meterRegistry = new SimpleMeterRegistry();
        metricsFilter = new MetricsFilter(meterRegistry);
        chain = mock(GatewayFilterChain.class);
    }

    @Test
    @DisplayName("Should increment request counter when processing request")
    void shouldIncrementRequestCounter() {
        // Arrange - Create a mock request and exchange
        MockServerHttpRequest request = MockServerHttpRequest
            .method(HttpMethod.GET, "/api/v1/clientes/123")
            .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.OK);

        Route route = createRoute("clientes-route", "clientes-service");
        exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, route);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // Act - Execute the filter
        Mono<Void> result = metricsFilter.filter(exchange, chain);

        // Assert - Verify the request completed successfully
        StepVerifier.create(result)
            .verifyComplete();

        // Verify counter was incremented
        Counter counter = meterRegistry.find("gateway.requests.total")
            .tag("route", "clientes-route")
            .tag("method", "GET")
            .tag("service", "clientes-service")
            .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should measure request duration with timer")
    void shouldMeasureRequestDuration() {
        // Arrange - Create request with delay to measure duration
        MockServerHttpRequest request = MockServerHttpRequest
            .method(HttpMethod.POST, "/api/v1/facturacion")
            .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.CREATED);

        Route route = createRoute("facturacion-route", "facturacion-service");
        exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, route);

        // Simulate processing delay
        when(chain.filter(any(ServerWebExchange.class)))
            .thenReturn(Mono.delay(java.time.Duration.ofMillis(10)).then());

        // Act - Execute the filter
        Mono<Void> result = metricsFilter.filter(exchange, chain);

        // Assert - Verify completion
        StepVerifier.create(result)
            .verifyComplete();

        // Verify timer was recorded
        Timer timer = meterRegistry.find("gateway.requests.duration")
            .tag("route", "facturacion-route")
            .tag("method", "POST")
            .tag("service", "facturacion-service")
            .tag("status", "201")
            .timer();

        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1L);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should categorize 2xx status codes correctly")
    void shouldCategorize2xxStatusCodes() {
        // Arrange - Create request that returns 200 OK
        MockServerHttpRequest request = MockServerHttpRequest
            .method(HttpMethod.GET, "/api/v1/clientes")
            .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.OK);

        Route route = createRoute("test-route", "test-service");
        exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, route);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // Act
        metricsFilter.filter(exchange, chain).block();

        // Assert - Verify status category is 2xx_success
        Counter statusCounter = meterRegistry.find("gateway.requests.status")
            .tag("route", "test-route")
            .tag("method", "GET")
            .tag("service", "test-service")
            .tag("status", "200")
            .tag("status_category", "2xx_success")
            .counter();

        assertThat(statusCounter).isNotNull();
        assertThat(statusCounter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should categorize 3xx status codes correctly")
    void shouldCategorize3xxStatusCodes() {
        // Arrange - Create request that returns 302 FOUND
        MockServerHttpRequest request = MockServerHttpRequest
            .method(HttpMethod.GET, "/api/v1/redirect")
            .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.FOUND);

        Route route = createRoute("test-route", "test-service");
        exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, route);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // Act
        metricsFilter.filter(exchange, chain).block();

        // Assert - Verify status category is 3xx_redirect
        Counter statusCounter = meterRegistry.find("gateway.requests.status")
            .tag("status_category", "3xx_redirect")
            .counter();

        assertThat(statusCounter).isNotNull();
        assertThat(statusCounter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should categorize 4xx status codes correctly")
    void shouldCategorize4xxStatusCodes() {
        // Arrange - Create request that returns 404 NOT FOUND
        MockServerHttpRequest request = MockServerHttpRequest
            .method(HttpMethod.GET, "/api/v1/notfound")
            .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);

        Route route = createRoute("test-route", "test-service");
        exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, route);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // Act
        metricsFilter.filter(exchange, chain).block();

        // Assert - Verify status category is 4xx_client_error
        Counter statusCounter = meterRegistry.find("gateway.requests.status")
            .tag("status", "404")
            .tag("status_category", "4xx_client_error")
            .counter();

        assertThat(statusCounter).isNotNull();
        assertThat(statusCounter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should categorize 5xx status codes correctly")
    void shouldCategorize5xxStatusCodes() {
        // Arrange - Create request that returns 503 SERVICE UNAVAILABLE
        MockServerHttpRequest request = MockServerHttpRequest
            .method(HttpMethod.POST, "/api/v1/error")
            .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);

        Route route = createRoute("test-route", "test-service");
        exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, route);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // Act
        metricsFilter.filter(exchange, chain).block();

        // Assert - Verify status category is 5xx_server_error
        Counter statusCounter = meterRegistry.find("gateway.requests.status")
            .tag("status", "503")
            .tag("status_category", "5xx_server_error")
            .counter();

        assertThat(statusCounter).isNotNull();
        assertThat(statusCounter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should handle multiple requests and accumulate metrics")
    void shouldHandleMultipleRequestsAndAccumulateMetrics() {
        // Arrange
        Route route = createRoute("multi-route", "multi-service");
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // Act - Send multiple requests
        for (int i = 0; i < 5; i++) {
            MockServerHttpRequest request = MockServerHttpRequest
                .method(HttpMethod.GET, "/api/v1/test/" + i)
                .build();

            ServerWebExchange exchange = MockServerWebExchange.from(request);
            exchange.getResponse().setStatusCode(HttpStatus.OK);
            exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, route);

            metricsFilter.filter(exchange, chain).block();
        }

        // Assert - Verify accumulated metrics
        Counter counter = meterRegistry.find("gateway.requests.total")
            .tag("route", "multi-route")
            .tag("method", "GET")
            .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("Should handle request with no route gracefully")
    void shouldHandleRequestWithNoRoute() {
        // Arrange - Create exchange without route attribute
        MockServerHttpRequest request = MockServerHttpRequest
            .method(HttpMethod.GET, "/api/v1/unknown")
            .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.OK);
        // Don't set GATEWAY_ROUTE_ATTR

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = metricsFilter.filter(exchange, chain);

        // Assert - Should complete without errors
        StepVerifier.create(result)
            .verifyComplete();

        // Verify metrics were recorded with "unknown" route
        Counter counter = meterRegistry.find("gateway.requests.total")
            .tag("route", "unknown")
            .tag("service", "unknown")
            .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should have correct filter order")
    void shouldHaveCorrectFilterOrder() {
        // Assert - Verify filter executes after rate limiting
        assertThat(metricsFilter.getOrder()).isEqualTo(-50);
    }

    /**
     * Helper method to create a Route for testing.
     *
     * @param routeId the route identifier
     * @param serviceName the service name
     * @return configured Route instance
     */
    private Route createRoute(String routeId, String serviceName) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("service", serviceName);

        return Route.async()
            .id(routeId)
            .uri(URI.create("http://localhost:8080"))
            .predicate(exchange -> true)
            .metadata(metadata)
            .build();
    }
}
