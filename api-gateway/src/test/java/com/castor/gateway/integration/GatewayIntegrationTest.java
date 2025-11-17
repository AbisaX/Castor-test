package com.castor.gateway.integration;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for API Gateway.
 *
 * Tests end-to-end functionality including:
 * - Routing to downstream services
 * - Circuit breaker activation after multiple failures
 * - Rate limiting and rejection after exceeding limits
 * - Request/response header propagation
 * - Retry mechanism
 * - Tracing header propagation
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "gateway.services.clientes.url=http://localhost:${mock.server.port}",
    "gateway.services.facturacion.url=http://localhost:${mock.server.port}",
    "gateway.services.tax-calculator.url=http://localhost:${mock.server.port}",
    "gateway.rate-limiting.default-limit=5",
    "gateway.rate-limiting.default-refresh-period=60",
    "resilience4j.circuitbreaker.instances.clientesCircuitBreaker.minimumNumberOfCalls=3",
    "resilience4j.circuitbreaker.instances.clientesCircuitBreaker.failureRateThreshold=50",
    "resilience4j.circuitbreaker.instances.clientesCircuitBreaker.waitDurationInOpenState=5s",
    "logging.level.com.castor.gateway=DEBUG"
})
@DisplayName("Gateway Integration Tests")
class GatewayIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private WebTestClient webTestClient;

    private MockWebServer mockBackendServer;

    @BeforeEach
    void setUp() throws IOException {
        mockBackendServer = new MockWebServer();
        mockBackendServer.start();

        // Update system property for mock server port
        System.setProperty("mock.server.port", String.valueOf(mockBackendServer.getPort()));

        // Configure WebTestClient with longer timeout for integration tests
        webTestClient = webTestClient.mutate()
            .responseTimeout(Duration.ofSeconds(10))
            .build();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (mockBackendServer != null) {
            mockBackendServer.shutdown();
        }
    }

    @Test
    @DisplayName("Should route request to clientes service successfully")
    void shouldRouteRequestToClientesServiceSuccessfully() throws InterruptedException {
        // Arrange - Mock successful response from downstream service
        mockBackendServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"id\":1,\"nombre\":\"Test Cliente\",\"email\":\"test@example.com\"}"));

        // Act - Send request through gateway
        webTestClient.get()
            .uri("/api/v1/clientes/1")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectHeader().exists("X-Gateway-Response")
            .expectBody()
            .jsonPath("$.id").isEqualTo(1)
            .jsonPath("$.nombre").isEqualTo("Test Cliente");

        // Assert - Verify request was forwarded to downstream service
        RecordedRequest recordedRequest = mockBackendServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(recordedRequest).isNotNull();
        assertThat(recordedRequest.getPath()).contains("/api/v1/clientes/1");
        assertThat(recordedRequest.getHeader("X-Gateway-Request")).isEqualTo("API-Gateway");
    }

    @Test
    @DisplayName("Should route request to facturacion service successfully")
    void shouldRouteRequestToFacturacionServiceSuccessfully() throws InterruptedException {
        // Arrange
        mockBackendServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"id\":100,\"numero\":\"FAC-001\",\"total\":1500.00}"));

        // Act
        webTestClient.get()
            .uri("/api/v1/facturas/100")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectHeader().exists("X-Gateway-Response")
            .expectBody()
            .jsonPath("$.id").isEqualTo(100)
            .jsonPath("$.numero").isEqualTo("FAC-001");

        // Assert
        RecordedRequest recordedRequest = mockBackendServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(recordedRequest).isNotNull();
        assertThat(recordedRequest.getPath()).contains("/api/v1/facturas/100");
    }

    @Test
    @DisplayName("Should route request to tax-calculator service successfully")
    void shouldRouteRequestToTaxCalculatorServiceSuccessfully() throws InterruptedException {
        // Arrange
        mockBackendServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"baseAmount\":1000.00,\"taxAmount\":190.00,\"totalAmount\":1190.00}"));

        // Act
        webTestClient.get()
            .uri("/api/v1/tax-calculator/calculate?amount=1000")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.baseAmount").isEqualTo(1000.00)
            .jsonPath("$.taxAmount").isEqualTo(190.00);

        // Assert
        RecordedRequest recordedRequest = mockBackendServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(recordedRequest).isNotNull();
        assertThat(recordedRequest.getPath()).contains("/api/v1/tax-calculator/calculate");
    }

    @Test
    @DisplayName("Should activate circuit breaker after multiple failures")
    void shouldActivateCircuitBreakerAfterMultipleFailures() {
        // Arrange - Mock failures from downstream service
        // Need at least 3 failures to open circuit (minimumNumberOfCalls=3)
        for (int i = 0; i < 5; i++) {
            mockBackendServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));
        }

        // Act - Make multiple failing requests to trigger circuit breaker
        for (int i = 0; i < 3; i++) {
            webTestClient.get()
                .uri("/api/v1/clientes/" + i)
                .exchange()
                .expectStatus().is5xxServerError();
        }

        // Wait a bit for circuit breaker to evaluate
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Assert - Next request should hit fallback due to open circuit
        webTestClient.get()
            .uri("/api/v1/clientes/999")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
            .expectBody()
            .jsonPath("$.error").isEqualTo("Service Unavailable")
            .jsonPath("$.message").value(message ->
                assertThat(message.toString()).contains("Clientes service"));
    }

    @Test
    @DisplayName("Should reject requests after exceeding rate limit")
    void shouldRejectRequestsAfterExceedingRateLimit() {
        // Arrange - Mock successful responses
        for (int i = 0; i < 10; i++) {
            mockBackendServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"id\":" + i + "}"));
        }

        // Act - Make requests up to the limit (5 requests per route)
        for (int i = 0; i < 5; i++) {
            webTestClient.get()
                .uri("/api/v1/clientes/" + i)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("X-RateLimit-Remaining");
        }

        // Assert - Next request should be rate limited
        webTestClient.get()
            .uri("/api/v1/clientes/999")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
            .expectHeader().exists("X-RateLimit-Remaining");
    }

    @Test
    @DisplayName("Should add trace headers to requests and responses")
    void shouldAddTraceHeadersToRequestsAndResponses() throws InterruptedException {
        // Arrange
        mockBackendServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"id\":1}"));

        // Act
        webTestClient.get()
            .uri("/api/v1/clientes/1")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().exists("X-Trace-Id")
            .expectHeader().exists("X-Span-Id");

        // Assert - Verify trace headers were forwarded to downstream
        RecordedRequest recordedRequest = mockBackendServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(recordedRequest).isNotNull();
        assertThat(recordedRequest.getHeader("X-Trace-Id")).isNotNull();
        assertThat(recordedRequest.getHeader("X-Span-Id")).isNotNull();
    }

    @Test
    @DisplayName("Should retry failed requests")
    void shouldRetryFailedRequests() throws InterruptedException {
        // Arrange - First two requests fail, third succeeds
        mockBackendServer.enqueue(new MockResponse()
            .setResponseCode(503)
            .setBody("Service Unavailable"));

        mockBackendServer.enqueue(new MockResponse()
            .setResponseCode(503)
            .setBody("Service Unavailable"));

        mockBackendServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"id\":1,\"status\":\"success\"}"));

        // Act - Make single request that should be retried
        webTestClient.get()
            .uri("/api/v1/clientes/1")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("success");

        // Assert - Verify 3 requests were made (1 original + 2 retries)
        assertThat(mockBackendServer.getRequestCount()).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Should handle 404 Not Found from downstream service")
    void shouldHandle404NotFoundFromDownstreamService() {
        // Arrange
        mockBackendServer.enqueue(new MockResponse()
            .setResponseCode(404)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"error\":\"Not Found\"}"));

        // Act & Assert
        webTestClient.get()
            .uri("/api/v1/clientes/99999")
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("Should handle timeout from downstream service")
    void shouldHandleTimeoutFromDownstreamService() {
        // Arrange - Mock delayed response that exceeds gateway timeout
        mockBackendServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBodyDelay(15, TimeUnit.SECONDS) // Exceeds 10s gateway timeout
            .setBody("{\"id\":1}"));

        // Act & Assert - Should timeout and trigger circuit breaker fallback
        webTestClient.get()
            .uri("/api/v1/clientes/1")
            .exchange()
            .expectStatus().is5xxServerError();
    }

    @Test
    @DisplayName("Should propagate custom headers to downstream service")
    void shouldPropagateCustomHeadersToDownstreamService() throws InterruptedException {
        // Arrange
        mockBackendServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"id\":1}"));

        String customHeaderValue = "test-correlation-id-123";

        // Act
        webTestClient.get()
            .uri("/api/v1/clientes/1")
            .header("X-Correlation-Id", customHeaderValue)
            .exchange()
            .expectStatus().isOk();

        // Assert - Verify custom header was forwarded
        RecordedRequest recordedRequest = mockBackendServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(recordedRequest).isNotNull();
        assertThat(recordedRequest.getHeader("X-Correlation-Id")).isEqualTo(customHeaderValue);
    }

    @Test
    @DisplayName("Should return gateway response header in all responses")
    void shouldReturnGatewayResponseHeaderInAllResponses() {
        // Arrange
        mockBackendServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"id\":1}"));

        // Act & Assert
        webTestClient.get()
            .uri("/api/v1/clientes/1")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().valueEquals("X-Gateway-Response", "API-Gateway");
    }

    @Test
    @DisplayName("Should handle POST request with body")
    void shouldHandlePostRequestWithBody() throws InterruptedException {
        // Arrange
        mockBackendServer.enqueue(new MockResponse()
            .setResponseCode(201)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"id\":10,\"nombre\":\"New Cliente\"}"));

        String requestBody = "{\"nombre\":\"New Cliente\",\"email\":\"new@example.com\"}";

        // Act
        webTestClient.post()
            .uri("/api/v1/clientes")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.id").isEqualTo(10)
            .jsonPath("$.nombre").isEqualTo("New Cliente");

        // Assert - Verify POST body was forwarded
        RecordedRequest recordedRequest = mockBackendServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(recordedRequest).isNotNull();
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getBody().readUtf8()).contains("New Cliente");
    }

    @Test
    @DisplayName("Should handle PUT request")
    void shouldHandlePutRequest() throws InterruptedException {
        // Arrange
        mockBackendServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"id\":1,\"nombre\":\"Updated Cliente\"}"));

        String requestBody = "{\"nombre\":\"Updated Cliente\"}";

        // Act
        webTestClient.put()
            .uri("/api/v1/clientes/1")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .exchange()
            .expectStatus().isOk();

        // Assert
        RecordedRequest recordedRequest = mockBackendServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(recordedRequest).isNotNull();
        assertThat(recordedRequest.getMethod()).isEqualTo("PUT");
    }

    @Test
    @DisplayName("Should handle DELETE request")
    void shouldHandleDeleteRequest() throws InterruptedException {
        // Arrange
        mockBackendServer.enqueue(new MockResponse()
            .setResponseCode(204));

        // Act
        webTestClient.delete()
            .uri("/api/v1/clientes/1")
            .exchange()
            .expectStatus().isNoContent();

        // Assert
        RecordedRequest recordedRequest = mockBackendServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(recordedRequest).isNotNull();
        assertThat(recordedRequest.getMethod()).isEqualTo("DELETE");
    }

    @Test
    @DisplayName("Should access actuator health endpoint")
    void shouldAccessActuatorHealthEndpoint() {
        // Act & Assert
        webTestClient.get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").exists();
    }

    @Test
    @DisplayName("Should access actuator metrics endpoint")
    void shouldAccessActuatorMetricsEndpoint() {
        // Act & Assert
        webTestClient.get()
            .uri("/actuator/metrics")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.names").isArray();
    }

    @Test
    @DisplayName("Should include rate limit headers in successful responses")
    void shouldIncludeRateLimitHeadersInSuccessfulResponses() {
        // Arrange
        mockBackendServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"id\":1}"));

        // Act & Assert
        webTestClient.get()
            .uri("/api/v1/clientes/1")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().exists("X-RateLimit-Remaining")
            .expectHeader().exists("X-RateLimit-Reset");
    }
}
