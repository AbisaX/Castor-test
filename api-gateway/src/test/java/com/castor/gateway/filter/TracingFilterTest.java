package com.castor.gateway.filter;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
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
import static org.mockito.Mockito.*;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

/**
 * Test class for TracingFilter.
 *
 * Tests distributed tracing functionality including:
 * - Propagation of trace IDs in headers
 * - Creation of spans with correct names
 * - Addition of tags to spans (route, method, path, service)
 * - Addition of trace IDs to responses
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TracingFilter Tests")
class TracingFilterTest {

    @Mock
    private Tracer tracer;

    @Mock
    private Span currentSpan;

    @Mock
    private Span gatewaySpan;

    @Mock
    private TraceContext traceContext;

    @Mock
    private GatewayFilterChain chain;

    private TracingFilter tracingFilter;

    @BeforeEach
    void setUp() {
        tracingFilter = new TracingFilter(tracer);
    }

    @Test
    @DisplayName("Should propagate trace ID in response headers")
    void shouldPropagateTraceIdInResponseHeaders() {
        // Arrange - Setup trace context and span
        String expectedTraceId = "abc123trace";
        String expectedSpanId = "xyz789span";

        when(tracer.currentSpan()).thenReturn(currentSpan);
        when(currentSpan.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn(expectedTraceId);
        when(traceContext.spanId()).thenReturn(expectedSpanId);
        when(tracer.nextSpan(any(Span.class))).thenReturn(gatewaySpan);
        when(gatewaySpan.name(anyString())).thenReturn(gatewaySpan);
        when(gatewaySpan.tag(anyString(), anyString())).thenReturn(gatewaySpan);
        when(gatewaySpan.start()).thenReturn(gatewaySpan);

        MockServerHttpRequest request = MockServerHttpRequest
            .method(HttpMethod.GET, "/api/v1/clientes")
            .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.OK);

        Route route = createRoute("clientes-route", "clientes-service");
        exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, route);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = tracingFilter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
            .verifyComplete();

        // Verify trace headers were added to response
        assertThat(exchange.getResponse().getHeaders().getFirst("X-Trace-Id"))
            .isEqualTo(expectedTraceId);
        assertThat(exchange.getResponse().getHeaders().getFirst("X-Span-Id"))
            .isEqualTo(expectedSpanId);
    }

    @Test
    @DisplayName("Should create gateway span with correct name")
    void shouldCreateGatewaySpanWithCorrectName() {
        // Arrange
        when(tracer.currentSpan()).thenReturn(currentSpan);
        when(currentSpan.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn("trace-123");
        when(traceContext.spanId()).thenReturn("span-456");
        when(tracer.nextSpan(any(Span.class))).thenReturn(gatewaySpan);
        when(gatewaySpan.name(anyString())).thenReturn(gatewaySpan);
        when(gatewaySpan.tag(anyString(), anyString())).thenReturn(gatewaySpan);
        when(gatewaySpan.start()).thenReturn(gatewaySpan);

        MockServerHttpRequest request = MockServerHttpRequest
            .method(HttpMethod.POST, "/api/v1/facturacion")
            .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.CREATED);

        Route route = createRoute("facturacion-route", "facturacion-service");
        exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, route);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // Act
        tracingFilter.filter(exchange, chain).block();

        // Assert - Verify span was created with correct name
        verify(gatewaySpan).name("gateway-request");
        verify(gatewaySpan).start();
        verify(gatewaySpan).end();
    }

    @Test
    @DisplayName("Should add route tag to current span")
    void shouldAddRouteTagToCurrentSpan() {
        // Arrange
        String routeId = "tax-calculator-route";

        when(tracer.currentSpan()).thenReturn(currentSpan);
        when(currentSpan.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn("trace-123");
        when(traceContext.spanId()).thenReturn("span-456");
        when(tracer.nextSpan(any(Span.class))).thenReturn(gatewaySpan);
        when(gatewaySpan.name(anyString())).thenReturn(gatewaySpan);
        when(gatewaySpan.tag(anyString(), anyString())).thenReturn(gatewaySpan);
        when(gatewaySpan.start()).thenReturn(gatewaySpan);

        MockServerHttpRequest request = MockServerHttpRequest
            .method(HttpMethod.GET, "/api/v1/tax-calculator/calculate")
            .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);
        Route route = createRoute(routeId, "tax-calculator-service");
        exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, route);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // Act
        tracingFilter.filter(exchange, chain).block();

        // Assert - Verify route tag was added
        verify(currentSpan).tag("gateway.route", routeId);
    }

    @Test
    @DisplayName("Should add HTTP method and path tags to current span")
    void shouldAddHttpMethodAndPathTagsToCurrentSpan() {
        // Arrange
        String expectedPath = "/api/v1/clientes/123";
        String expectedMethod = "GET";

        when(tracer.currentSpan()).thenReturn(currentSpan);
        when(currentSpan.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn("trace-123");
        when(traceContext.spanId()).thenReturn("span-456");
        when(tracer.nextSpan(any(Span.class))).thenReturn(gatewaySpan);
        when(gatewaySpan.name(anyString())).thenReturn(gatewaySpan);
        when(gatewaySpan.tag(anyString(), anyString())).thenReturn(gatewaySpan);
        when(gatewaySpan.start()).thenReturn(gatewaySpan);

        MockServerHttpRequest request = MockServerHttpRequest
            .method(HttpMethod.GET, expectedPath)
            .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);
        Route route = createRoute("test-route", "test-service");
        exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, route);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // Act
        tracingFilter.filter(exchange, chain).block();

        // Assert - Verify HTTP tags were added
        verify(currentSpan).tag("http.method", expectedMethod);
        verify(currentSpan).tag("http.path", expectedPath);
    }

    @Test
    @DisplayName("Should add service tag to current span")
    void shouldAddServiceTagToCurrentSpan() {
        // Arrange
        String serviceName = "facturacion-service";

        when(tracer.currentSpan()).thenReturn(currentSpan);
        when(currentSpan.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn("trace-123");
        when(traceContext.spanId()).thenReturn("span-456");
        when(tracer.nextSpan(any(Span.class))).thenReturn(gatewaySpan);
        when(gatewaySpan.name(anyString())).thenReturn(gatewaySpan);
        when(gatewaySpan.tag(anyString(), anyString())).thenReturn(gatewaySpan);
        when(gatewaySpan.start()).thenReturn(gatewaySpan);

        MockServerHttpRequest request = MockServerHttpRequest
            .method(HttpMethod.POST, "/api/v1/facturacion")
            .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);
        Route route = createRoute("facturacion-route", serviceName);
        exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, route);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // Act
        tracingFilter.filter(exchange, chain).block();

        // Assert - Verify service tag was added
        verify(currentSpan).tag("downstream.service", serviceName);
    }

    @Test
    @DisplayName("Should add component tag to current span")
    void shouldAddComponentTagToCurrentSpan() {
        // Arrange
        when(tracer.currentSpan()).thenReturn(currentSpan);
        when(currentSpan.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn("trace-123");
        when(traceContext.spanId()).thenReturn("span-456");
        when(tracer.nextSpan(any(Span.class))).thenReturn(gatewaySpan);
        when(gatewaySpan.name(anyString())).thenReturn(gatewaySpan);
        when(gatewaySpan.tag(anyString(), anyString())).thenReturn(gatewaySpan);
        when(gatewaySpan.start()).thenReturn(gatewaySpan);

        MockServerHttpRequest request = MockServerHttpRequest
            .method(HttpMethod.GET, "/api/v1/test")
            .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);
        Route route = createRoute("test-route", "test-service");
        exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, route);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // Act
        tracingFilter.filter(exchange, chain).block();

        // Assert - Verify component tag was added
        verify(currentSpan).tag("component", "api-gateway");
    }

    @Test
    @DisplayName("Should add downstream URI tag to current span")
    void shouldAddDownstreamUriTagToCurrentSpan() {
        // Arrange
        String downstreamUri = "http://localhost:8081";

        when(tracer.currentSpan()).thenReturn(currentSpan);
        when(currentSpan.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn("trace-123");
        when(traceContext.spanId()).thenReturn("span-456");
        when(tracer.nextSpan(any(Span.class))).thenReturn(gatewaySpan);
        when(gatewaySpan.name(anyString())).thenReturn(gatewaySpan);
        when(gatewaySpan.tag(anyString(), anyString())).thenReturn(gatewaySpan);
        when(gatewaySpan.start()).thenReturn(gatewaySpan);

        MockServerHttpRequest request = MockServerHttpRequest
            .method(HttpMethod.GET, "/api/v1/clientes")
            .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);
        Route route = createRouteWithUri("clientes-route", "clientes-service", downstreamUri);
        exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, route);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // Act
        tracingFilter.filter(exchange, chain).block();

        // Assert - Verify downstream URI tag was added
        verify(currentSpan).tag("downstream.uri", downstreamUri);
    }

    @Test
    @DisplayName("Should add HTTP status code tag to gateway span")
    void shouldAddHttpStatusCodeTagToGatewaySpan() {
        // Arrange
        when(tracer.currentSpan()).thenReturn(currentSpan);
        when(currentSpan.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn("trace-123");
        when(traceContext.spanId()).thenReturn("span-456");
        when(tracer.nextSpan(any(Span.class))).thenReturn(gatewaySpan);
        when(gatewaySpan.name(anyString())).thenReturn(gatewaySpan);
        when(gatewaySpan.tag(anyString(), anyString())).thenReturn(gatewaySpan);
        when(gatewaySpan.start()).thenReturn(gatewaySpan);

        MockServerHttpRequest request = MockServerHttpRequest
            .method(HttpMethod.GET, "/api/v1/test")
            .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.OK);

        Route route = createRoute("test-route", "test-service");
        exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, route);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // Act
        tracingFilter.filter(exchange, chain).block();

        // Assert - Verify status code tag was added to gateway span
        verify(gatewaySpan).tag("http.status_code", "200");
    }

    @Test
    @DisplayName("Should handle request when current span is null")
    void shouldHandleRequestWhenCurrentSpanIsNull() {
        // Arrange - No current span available
        when(tracer.currentSpan()).thenReturn(null);

        MockServerHttpRequest request = MockServerHttpRequest
            .method(HttpMethod.GET, "/api/v1/test")
            .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = tracingFilter.filter(exchange, chain);

        // Assert - Should complete without errors
        StepVerifier.create(result)
            .verifyComplete();

        // Verify no span operations were attempted
        verify(tracer, never()).nextSpan(any());
        verify(chain).filter(exchange);
    }

    @Test
    @DisplayName("Should add request path and method tags to gateway span")
    void shouldAddRequestPathAndMethodTagsToGatewaySpan() {
        // Arrange
        String requestPath = "/api/v1/facturacion/invoices";
        String requestMethod = "POST";

        when(tracer.currentSpan()).thenReturn(currentSpan);
        when(currentSpan.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn("trace-123");
        when(traceContext.spanId()).thenReturn("span-456");
        when(tracer.nextSpan(any(Span.class))).thenReturn(gatewaySpan);
        when(gatewaySpan.name(anyString())).thenReturn(gatewaySpan);
        when(gatewaySpan.tag(anyString(), anyString())).thenReturn(gatewaySpan);
        when(gatewaySpan.start()).thenReturn(gatewaySpan);

        MockServerHttpRequest request = MockServerHttpRequest
            .method(HttpMethod.POST, requestPath)
            .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);
        Route route = createRoute("test-route", "test-service");
        exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, route);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // Act
        tracingFilter.filter(exchange, chain).block();

        // Assert - Verify request tags were added to gateway span
        verify(gatewaySpan).tag("request.path", requestPath);
        verify(gatewaySpan).tag("request.method", requestMethod);
    }

    @Test
    @DisplayName("Should have correct filter order")
    void shouldHaveCorrectFilterOrder() {
        // Assert - Verify filter executes after metrics filter
        assertThat(tracingFilter.getOrder()).isEqualTo(-25);
    }

    /**
     * Helper method to create a Route for testing.
     *
     * @param routeId the route identifier
     * @param serviceName the service name
     * @return configured Route instance
     */
    private Route createRoute(String routeId, String serviceName) {
        return createRouteWithUri(routeId, serviceName, "http://localhost:8080");
    }

    /**
     * Helper method to create a Route with specific URI for testing.
     *
     * @param routeId the route identifier
     * @param serviceName the service name
     * @param uri the downstream URI
     * @return configured Route instance
     */
    private Route createRouteWithUri(String routeId, String serviceName, String uri) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("service", serviceName);

        return Route.async()
            .id(routeId)
            .uri(URI.create(uri))
            .predicate(exchange -> true)
            .metadata(metadata)
            .build();
    }
}
