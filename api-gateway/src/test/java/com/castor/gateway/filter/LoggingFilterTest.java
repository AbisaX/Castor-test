package com.castor.gateway.filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

/**
 * Test class for LoggingFilter.
 *
 * Tests logging functionality including:
 * - Logging of requests with all details
 * - Logging of responses with latency
 * - Detection of slow requests (>1000ms)
 * - Extraction of client IP from headers
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoggingFilter Tests")
class LoggingFilterTest {

    @Mock
    private Tracer tracer;

    @Mock
    private Span span;

    @Mock
    private TraceContext traceContext;

    @Mock
    private GatewayFilterChain chain;

    private LoggingFilter loggingFilter;
    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        loggingFilter = new LoggingFilter(tracer);

        // Setup log capture
        logger = (Logger) LoggerFactory.getLogger(LoggingFilter.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
    }

    @Test
    @DisplayName("Should log incoming request with all details")
    void shouldLogIncomingRequestWithAllDetails() {
        // Arrange
        String traceId = "trace-abc123";
        setupTracer(traceId);

        MockServerHttpRequest request = MockServerHttpRequest
            .method(HttpMethod.GET, "/api/v1/clientes?page=1&size=10")
            .remoteAddress(new InetSocketAddress("192.168.1.100", 8080))
            .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.OK);

        Route route = createRoute("clientes-route", "clientes-service");
        exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, route);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // Act
        loggingFilter.filter(exchange, chain).block();

        // Assert - Verify incoming request was logged
        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList).isNotEmpty();

        ILoggingEvent requestLog = logsList.stream()
            .filter(event -> event.getMessage().contains("Incoming Request"))
            .findFirst()
            .orElse(null);

        assertThat(requestLog).isNotNull();
        assertThat(requestLog.getLevel()).isEqualTo(Level.INFO);
        assertThat(requestLog.getFormattedMessage())
            .contains(traceId)
            .contains("clientes-route")
            .contains("GET")
            .contains("/api/v1/clientes")
            .contains("page=1&size=10");
    }

    @Test
    @DisplayName("Should log outgoing response with latency")
    void shouldLogOutgoingResponseWithLatency() {
        // Arrange
        String traceId = "trace-xyz789";
        setupTracer(traceId);

        MockServerHttpRequest request = MockServerHttpRequest
            .method(HttpMethod.POST, "/api/v1/facturacion")
            .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.CREATED);

        Route route = createRoute("facturacion-route", "facturacion-service");
        exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, route);

        // Simulate some processing time
        when(chain.filter(any(ServerWebExchange.class)))
            .thenReturn(Mono.delay(Duration.ofMillis(50)).then());

        // Act
        loggingFilter.filter(exchange, chain).block();

        // Assert - Verify outgoing response was logged
        List<ILoggingEvent> logsList = listAppender.list;

        ILoggingEvent responseLog = logsList.stream()
            .filter(event -> event.getMessage().contains("Outgoing Response"))
            .findFirst()
            .orElse(null);

        assertThat(responseLog).isNotNull();
        assertThat(responseLog.getLevel()).isEqualTo(Level.INFO);
        assertThat(responseLog.getFormattedMessage())
            .contains(traceId)
            .contains("POST")
            .contains("/api/v1/facturacion")
            .contains("201")
            .contains("Duration:");
    }

    @Test
    @DisplayName("Should detect and log slow requests over 1000ms")
    void shouldDetectAndLogSlowRequests() {
        // Arrange
        String traceId = "trace-slow123";
        setupTracer(traceId);

        MockServerHttpRequest request = MockServerHttpRequest
            .method(HttpMethod.GET, "/api/v1/slow-endpoint")
            .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.OK);

        Route route = createRoute("slow-route", "slow-service");
        exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, route);

        // Simulate slow request (>1000ms)
        when(chain.filter(any(ServerWebExchange.class)))
            .thenReturn(Mono.delay(Duration.ofMillis(1100)).then());

        // Act
        loggingFilter.filter(exchange, chain).block();

        // Assert - Verify slow request warning was logged
        List<ILoggingEvent> logsList = listAppender.list;

        ILoggingEvent slowRequestLog = logsList.stream()
            .filter(event -> event.getMessage().contains("Slow request detected"))
            .findFirst()
            .orElse(null);

        assertThat(slowRequestLog).isNotNull();
        assertThat(slowRequestLog.getLevel()).isEqualTo(Level.WARN);
        assertThat(slowRequestLog.getFormattedMessage())
            .contains(traceId)
            .contains("/api/v1/slow-endpoint")
            .contains("Duration:");
    }

    @Test
    @DisplayName("Should not log slow request warning for fast requests")
    void shouldNotLogSlowRequestWarningForFastRequests() {
        // Arrange
        String traceId = "trace-fast123";
        setupTracer(traceId);

        MockServerHttpRequest request = MockServerHttpRequest
            .method(HttpMethod.GET, "/api/v1/fast-endpoint")
            .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.OK);

        Route route = createRoute("fast-route", "fast-service");
        exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, route);

        // Simulate fast request (<1000ms)
        when(chain.filter(any(ServerWebExchange.class)))
            .thenReturn(Mono.delay(Duration.ofMillis(100)).then());

        // Act
        loggingFilter.filter(exchange, chain).block();

        // Assert - Verify no slow request warning
        List<ILoggingEvent> logsList = listAppender.list;

        long slowRequestWarnings = logsList.stream()
            .filter(event -> event.getMessage().contains("Slow request detected"))
            .count();

        assertThat(slowRequestWarnings).isZero();
    }

    @Test
    @DisplayName("Should extract client IP from X-Forwarded-For header")
    void shouldExtractClientIpFromXForwardedForHeader() {
        // Arrange
        String traceId = "trace-ip123";
        String expectedIp = "203.0.113.195";
        setupTracer(traceId);

        MockServerHttpRequest request = MockServerHttpRequest
            .method(HttpMethod.GET, "/api/v1/test")
            .header("X-Forwarded-For", expectedIp + ", 198.51.100.178")
            .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.OK);

        Route route = createRoute("test-route", "test-service");
        exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, route);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // Act
        loggingFilter.filter(exchange, chain).block();

        // Assert - Verify client IP was extracted from X-Forwarded-For
        List<ILoggingEvent> logsList = listAppender.list;

        ILoggingEvent requestLog = logsList.stream()
            .filter(event -> event.getMessage().contains("Incoming Request"))
            .findFirst()
            .orElse(null);

        assertThat(requestLog).isNotNull();
        assertThat(requestLog.getFormattedMessage()).contains(expectedIp);
    }

    @Test
    @DisplayName("Should extract client IP from X-Real-IP header when X-Forwarded-For is absent")
    void shouldExtractClientIpFromXRealIpHeader() {
        // Arrange
        String traceId = "trace-realip123";
        String expectedIp = "198.51.100.42";
        setupTracer(traceId);

        MockServerHttpRequest request = MockServerHttpRequest
            .method(HttpMethod.GET, "/api/v1/test")
            .header("X-Real-IP", expectedIp)
            .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.OK);

        Route route = createRoute("test-route", "test-service");
        exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, route);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // Act
        loggingFilter.filter(exchange, chain).block();

        // Assert - Verify client IP was extracted from X-Real-IP
        List<ILoggingEvent> logsList = listAppender.list;

        ILoggingEvent requestLog = logsList.stream()
            .filter(event -> event.getMessage().contains("Incoming Request"))
            .findFirst()
            .orElse(null);

        assertThat(requestLog).isNotNull();
        assertThat(requestLog.getFormattedMessage()).contains(expectedIp);
    }

    @Test
    @DisplayName("Should fall back to remote address when headers are absent")
    void shouldFallBackToRemoteAddressWhenHeadersAreAbsent() {
        // Arrange
        String traceId = "trace-remote123";
        setupTracer(traceId);

        MockServerHttpRequest request = MockServerHttpRequest
            .method(HttpMethod.GET, "/api/v1/test")
            .remoteAddress(new InetSocketAddress("192.168.1.50", 8080))
            .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.OK);

        Route route = createRoute("test-route", "test-service");
        exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, route);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // Act
        loggingFilter.filter(exchange, chain).block();

        // Assert - Verify remote address was used
        List<ILoggingEvent> logsList = listAppender.list;

        ILoggingEvent requestLog = logsList.stream()
            .filter(event -> event.getMessage().contains("Incoming Request"))
            .findFirst()
            .orElse(null);

        assertThat(requestLog).isNotNull();
        assertThat(requestLog.getFormattedMessage()).contains("192.168.1.50");
    }

    @Test
    @DisplayName("Should log error when request processing fails")
    void shouldLogErrorWhenRequestProcessingFails() {
        // Arrange
        String traceId = "trace-error123";
        setupTracer(traceId);

        MockServerHttpRequest request = MockServerHttpRequest
            .method(HttpMethod.POST, "/api/v1/error")
            .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);

        Route route = createRoute("error-route", "error-service");
        exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, route);

        RuntimeException testException = new RuntimeException("Test error");
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.error(testException));

        // Act
        Mono<Void> result = loggingFilter.filter(exchange, chain);

        // Assert - Verify error was logged
        StepVerifier.create(result)
            .expectError(RuntimeException.class)
            .verify();

        List<ILoggingEvent> logsList = listAppender.list;

        ILoggingEvent errorLog = logsList.stream()
            .filter(event -> event.getMessage().contains("Request Error"))
            .findFirst()
            .orElse(null);

        assertThat(errorLog).isNotNull();
        assertThat(errorLog.getLevel()).isEqualTo(Level.ERROR);
        assertThat(errorLog.getFormattedMessage())
            .contains(traceId)
            .contains("POST")
            .contains("/api/v1/error")
            .contains("Test error");
    }

    @Test
    @DisplayName("Should use 'no-trace' when tracer is null")
    void shouldUseNoTraceWhenTracerIsNull() {
        // Arrange
        loggingFilter = new LoggingFilter(null);

        MockServerHttpRequest request = MockServerHttpRequest
            .method(HttpMethod.GET, "/api/v1/test")
            .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.OK);

        Route route = createRoute("test-route", "test-service");
        exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, route);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // Act
        loggingFilter.filter(exchange, chain).block();

        // Assert - Verify 'no-trace' is used
        List<ILoggingEvent> logsList = listAppender.list;

        ILoggingEvent requestLog = logsList.stream()
            .filter(event -> event.getMessage().contains("Incoming Request"))
            .findFirst()
            .orElse(null);

        assertThat(requestLog).isNotNull();
        assertThat(requestLog.getFormattedMessage()).contains("no-trace");
    }

    @Test
    @DisplayName("Should use 'no-trace' when current span is null")
    void shouldUseNoTraceWhenCurrentSpanIsNull() {
        // Arrange
        when(tracer.currentSpan()).thenReturn(null);

        MockServerHttpRequest request = MockServerHttpRequest
            .method(HttpMethod.GET, "/api/v1/test")
            .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.OK);

        Route route = createRoute("test-route", "test-service");
        exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, route);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // Act
        loggingFilter.filter(exchange, chain).block();

        // Assert - Verify 'no-trace' is used
        List<ILoggingEvent> logsList = listAppender.list;

        ILoggingEvent requestLog = logsList.stream()
            .filter(event -> event.getMessage().contains("Incoming Request"))
            .findFirst()
            .orElse(null);

        assertThat(requestLog).isNotNull();
        assertThat(requestLog.getFormattedMessage()).contains("no-trace");
    }

    @Test
    @DisplayName("Should have highest precedence filter order")
    void shouldHaveHighestPrecedenceFilterOrder() {
        // Assert - Verify filter executes first
        assertThat(loggingFilter.getOrder()).isEqualTo(Integer.MIN_VALUE);
    }

    /**
     * Helper method to setup tracer with trace ID.
     *
     * @param traceId the trace ID to return
     */
    private void setupTracer(String traceId) {
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn(traceId);
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
