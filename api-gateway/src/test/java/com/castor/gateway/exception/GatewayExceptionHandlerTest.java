package com.castor.gateway.exception;

import com.castor.gateway.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Test class for GatewayExceptionHandler.
 *
 * Tests exception handling including:
 * - NotFoundException → 404 response
 * - ConnectException → 503 response
 * - TimeoutException → 504 response
 * - ResponseStatusException with custom code
 * - ErrorResponse format validation
 * - Trace ID inclusion in error responses
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GatewayExceptionHandler Tests")
class GatewayExceptionHandlerTest {

    @Mock
    private Tracer tracer;

    @Mock
    private Span span;

    @Mock
    private TraceContext traceContext;

    private ObjectMapper objectMapper;
    private GatewayExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        exceptionHandler = new GatewayExceptionHandler(objectMapper, tracer);
    }

    @Test
    @DisplayName("Should handle NotFoundException and return 404")
    void shouldHandleNotFoundExceptionAndReturn404() {
        // Arrange
        String traceId = "trace-notfound123";
        setupTracer(traceId);

        String path = "/api/v1/unknown-service";
        MockServerHttpRequest request = MockServerHttpRequest.get(path).build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        NotFoundException exception = new NotFoundException("Service not found: unknown-service");

        // Act
        Mono<Void> result = exceptionHandler.handle(exchange, exception);

        // Assert
        StepVerifier.create(result)
            .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(exchange.getResponse().getHeaders().getFirst("X-Trace-Id")).isEqualTo(traceId);
    }

    @Test
    @DisplayName("Should handle ConnectException and return 503")
    void shouldHandleConnectExceptionAndReturn503() {
        // Arrange
        String traceId = "trace-connect123";
        setupTracer(traceId);

        String path = "/api/v1/clientes";
        MockServerHttpRequest request = MockServerHttpRequest.get(path).build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        ConnectException exception = new ConnectException("Connection refused: localhost:8081");

        // Act
        Mono<Void> result = exceptionHandler.handle(exchange, exception);

        // Assert
        StepVerifier.create(result)
            .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(exchange.getResponse().getHeaders().getFirst("X-Trace-Id")).isEqualTo(traceId);
    }

    @Test
    @DisplayName("Should handle TimeoutException and return 504")
    void shouldHandleTimeoutExceptionAndReturn504() {
        // Arrange
        String traceId = "trace-timeout123";
        setupTracer(traceId);

        String path = "/api/v1/facturacion";
        MockServerHttpRequest request = MockServerHttpRequest.post(path).build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        TimeoutException exception = new TimeoutException("Request timeout after 10000ms");

        // Act
        Mono<Void> result = exceptionHandler.handle(exchange, exception);

        // Assert
        StepVerifier.create(result)
            .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(exchange.getResponse().getHeaders().getFirst("X-Trace-Id")).isEqualTo(traceId);
    }

    @Test
    @DisplayName("Should handle ResponseStatusException with custom status code")
    void shouldHandleResponseStatusExceptionWithCustomStatusCode() {
        // Arrange
        String traceId = "trace-custom123";
        setupTracer(traceId);

        String path = "/api/v1/bad-request";
        MockServerHttpRequest request = MockServerHttpRequest.get(path).build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        ResponseStatusException exception = new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Invalid request parameters"
        );

        // Act
        Mono<Void> result = exceptionHandler.handle(exchange, exception);

        // Assert
        StepVerifier.create(result)
            .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    }

    @Test
    @DisplayName("Should handle ResponseStatusException with 401 Unauthorized")
    void shouldHandleResponseStatusExceptionWith401() {
        // Arrange
        String traceId = "trace-auth123";
        setupTracer(traceId);

        String path = "/api/v1/secure";
        MockServerHttpRequest request = MockServerHttpRequest.get(path).build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        ResponseStatusException exception = new ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            "Authentication required"
        );

        // Act
        Mono<Void> result = exceptionHandler.handle(exchange, exception);

        // Assert
        StepVerifier.create(result)
            .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Should handle nested ConnectException and return 503")
    void shouldHandleNestedConnectExceptionAndReturn503() {
        // Arrange
        String traceId = "trace-nested123";
        setupTracer(traceId);

        String path = "/api/v1/service";
        MockServerHttpRequest request = MockServerHttpRequest.get(path).build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        ConnectException cause = new ConnectException("Connection refused");
        RuntimeException exception = new RuntimeException("Failed to connect", cause);

        // Act
        Mono<Void> result = exceptionHandler.handle(exchange, exception);

        // Assert
        StepVerifier.create(result)
            .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("Should return 500 for unknown exception types")
    void shouldReturn500ForUnknownExceptionTypes() {
        // Arrange
        String traceId = "trace-unknown123";
        setupTracer(traceId);

        String path = "/api/v1/error";
        MockServerHttpRequest request = MockServerHttpRequest.get(path).build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");

        // Act
        Mono<Void> result = exceptionHandler.handle(exchange, exception);

        // Assert
        StepVerifier.create(result)
            .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    }

    @Test
    @DisplayName("Should include trace ID in error response body")
    void shouldIncludeTraceIdInErrorResponseBody() throws Exception {
        // Arrange
        String traceId = "trace-body123";
        setupTracer(traceId);

        String path = "/api/v1/test";
        MockServerHttpRequest request = MockServerHttpRequest.get(path).build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        RuntimeException exception = new RuntimeException("Test exception");

        // Act
        exceptionHandler.handle(exchange, exception).block();

        // Assert - Extract and verify response body
        byte[] responseBody = exchange.getResponse().getBody().blockFirst().asByteBuffer().array();
        ErrorResponse errorResponse = objectMapper.readValue(responseBody, ErrorResponse.class);

        assertThat(errorResponse.getTraceId()).isEqualTo(traceId);
        assertThat(errorResponse.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(errorResponse.getPath()).isEqualTo(path);
        assertThat(errorResponse.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should format ErrorResponse correctly for NotFoundException")
    void shouldFormatErrorResponseCorrectlyForNotFoundException() throws Exception {
        // Arrange
        String traceId = "trace-format123";
        setupTracer(traceId);

        String path = "/api/v1/missing";
        MockServerHttpRequest request = MockServerHttpRequest.get(path).build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        NotFoundException exception = new NotFoundException("Service missing-service not found");

        // Act
        exceptionHandler.handle(exchange, exception).block();

        // Assert
        byte[] responseBody = exchange.getResponse().getBody().blockFirst().asByteBuffer().array();
        ErrorResponse errorResponse = objectMapper.readValue(responseBody, ErrorResponse.class);

        assertThat(errorResponse.getStatus()).isEqualTo(404);
        assertThat(errorResponse.getError()).isEqualTo("Service Not Found");
        assertThat(errorResponse.getMessage()).contains("not available");
        assertThat(errorResponse.getPath()).isEqualTo(path);
        assertThat(errorResponse.getTraceId()).isEqualTo(traceId);
    }

    @Test
    @DisplayName("Should format ErrorResponse correctly for ConnectException")
    void shouldFormatErrorResponseCorrectlyForConnectException() throws Exception {
        // Arrange
        String traceId = "trace-connect-format123";
        setupTracer(traceId);

        String path = "/api/v1/unavailable";
        MockServerHttpRequest request = MockServerHttpRequest.get(path).build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        ConnectException exception = new ConnectException("Connection refused");

        // Act
        exceptionHandler.handle(exchange, exception).block();

        // Assert
        byte[] responseBody = exchange.getResponse().getBody().blockFirst().asByteBuffer().array();
        ErrorResponse errorResponse = objectMapper.readValue(responseBody, ErrorResponse.class);

        assertThat(errorResponse.getStatus()).isEqualTo(503);
        assertThat(errorResponse.getError()).isEqualTo("Service Unavailable");
        assertThat(errorResponse.getMessage()).contains("Unable to connect");
        assertThat(errorResponse.getPath()).isEqualTo(path);
    }

    @Test
    @DisplayName("Should format ErrorResponse correctly for TimeoutException")
    void shouldFormatErrorResponseCorrectlyForTimeoutException() throws Exception {
        // Arrange
        String traceId = "trace-timeout-format123";
        setupTracer(traceId);

        String path = "/api/v1/slow";
        MockServerHttpRequest request = MockServerHttpRequest.get(path).build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        TimeoutException exception = new TimeoutException("Request timed out");

        // Act
        exceptionHandler.handle(exchange, exception).block();

        // Assert
        byte[] responseBody = exchange.getResponse().getBody().blockFirst().asByteBuffer().array();
        ErrorResponse errorResponse = objectMapper.readValue(responseBody, ErrorResponse.class);

        assertThat(errorResponse.getStatus()).isEqualTo(504);
        assertThat(errorResponse.getError()).isEqualTo("Gateway Timeout");
        assertThat(errorResponse.getMessage()).contains("timed out");
        assertThat(errorResponse.getPath()).isEqualTo(path);
    }

    @Test
    @DisplayName("Should handle exception when tracer is null")
    void shouldHandleExceptionWhenTracerIsNull() {
        // Arrange
        exceptionHandler = new GatewayExceptionHandler(objectMapper, null);

        String path = "/api/v1/test";
        MockServerHttpRequest request = MockServerHttpRequest.get(path).build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        RuntimeException exception = new RuntimeException("Test error");

        // Act
        Mono<Void> result = exceptionHandler.handle(exchange, exception);

        // Assert - Should complete without errors even with null tracer
        StepVerifier.create(result)
            .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        // X-Trace-Id header should not be present
        assertThat(exchange.getResponse().getHeaders().getFirst("X-Trace-Id")).isNull();
    }

    @Test
    @DisplayName("Should handle exception when current span is null")
    void shouldHandleExceptionWhenCurrentSpanIsNull() {
        // Arrange
        when(tracer.currentSpan()).thenReturn(null);

        String path = "/api/v1/test";
        MockServerHttpRequest request = MockServerHttpRequest.get(path).build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        RuntimeException exception = new RuntimeException("Test error");

        // Act
        Mono<Void> result = exceptionHandler.handle(exchange, exception);

        // Assert - Should complete without errors even with null span
        StepVerifier.create(result)
            .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        // X-Trace-Id header should not be present
        assertThat(exchange.getResponse().getHeaders().getFirst("X-Trace-Id")).isNull();
    }

    @Test
    @DisplayName("Should handle ResponseStatusException without reason")
    void shouldHandleResponseStatusExceptionWithoutReason() throws Exception {
        // Arrange
        String traceId = "trace-noreason123";
        setupTracer(traceId);

        String path = "/api/v1/conflict";
        MockServerHttpRequest request = MockServerHttpRequest.get(path).build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        ResponseStatusException exception = new ResponseStatusException(HttpStatus.CONFLICT);

        // Act
        exceptionHandler.handle(exchange, exception).block();

        // Assert
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        byte[] responseBody = exchange.getResponse().getBody().blockFirst().asByteBuffer().array();
        ErrorResponse errorResponse = objectMapper.readValue(responseBody, ErrorResponse.class);

        assertThat(errorResponse.getMessage()).isEqualTo("An error occurred");
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
}
