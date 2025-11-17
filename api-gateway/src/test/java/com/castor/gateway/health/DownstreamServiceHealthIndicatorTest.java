package com.castor.gateway.health;

import com.castor.gateway.config.GatewayProperties;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for DownstreamServiceHealthIndicator.
 *
 * Tests health check functionality including:
 * - Successful health checks (all services UP)
 * - Health check with one service DOWN
 * - Timeout handling in health checks
 * - Response time measurement
 * - Service details in health response
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DownstreamServiceHealthIndicator Tests")
class DownstreamServiceHealthIndicatorTest {

    private MockWebServer mockWebServer;
    private GatewayProperties gatewayProperties;
    private WebClient.Builder webClientBuilder;
    private DownstreamServiceHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        gatewayProperties = new GatewayProperties();
        webClientBuilder = WebClient.builder();

        // Configure properties with mock server URL
        String baseUrl = mockWebServer.url("/").toString().replaceAll("/$", "");

        gatewayProperties.getServices().getClientes().setUrl(baseUrl);
        gatewayProperties.getServices().getClientes().setHealthCheckEnabled(true);

        gatewayProperties.getServices().getFacturacion().setUrl(baseUrl);
        gatewayProperties.getServices().getFacturacion().setHealthCheckEnabled(true);

        gatewayProperties.getServices().getTaxCalculator().setUrl(baseUrl);
        gatewayProperties.getServices().getTaxCalculator().setHealthCheckEnabled(true);

        healthIndicator = new DownstreamServiceHealthIndicator(gatewayProperties, webClientBuilder);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("Should return UP when all services are healthy")
    void shouldReturnUpWhenAllServicesAreHealthy() {
        // Arrange - Mock successful health responses for all services
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"status\":\"UP\"}")
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"status\":\"UP\"}")
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"status\":\"UP\"}")
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

        // Act
        Health health = healthIndicator.health();

        // Assert
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("message");
        assertThat(health.getDetails().get("message")).isEqualTo("All downstream services are healthy");
        assertThat(health.getDetails()).containsKeys("clientes-service", "facturacion-service", "tax-calculator-service");
    }

    @Test
    @DisplayName("Should return DOWN when one service is unhealthy")
    void shouldReturnDownWhenOneServiceIsUnhealthy() {
        // Arrange - Mock responses with one service down
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"status\":\"UP\"}")
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"status\":\"DOWN\"}")
            .setHeader("Content-Type", "application/json")
            .setResponseCode(503));

        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"status\":\"UP\"}")
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

        // Act
        Health health = healthIndicator.health();

        // Assert
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("message");
        assertThat(health.getDetails().get("message")).isEqualTo("One or more downstream services are unhealthy");
    }

    @Test
    @DisplayName("Should return DOWN when all services are unreachable")
    void shouldReturnDownWhenAllServicesAreUnreachable() {
        // Arrange - Mock connection failures for all services
        mockWebServer.enqueue(new MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AT_START));
        mockWebServer.enqueue(new MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AT_START));
        mockWebServer.enqueue(new MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AT_START));

        // Act
        Health health = healthIndicator.health();

        // Assert
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("message");
    }

    @Test
    @DisplayName("Should handle timeout in health check")
    void shouldHandleTimeoutInHealthCheck() {
        // Arrange - Mock delayed responses that will timeout (>3 seconds)
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"status\":\"UP\"}")
            .setBodyDelay(5, TimeUnit.SECONDS));

        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"status\":\"UP\"}")
            .setBodyDelay(5, TimeUnit.SECONDS));

        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"status\":\"UP\"}")
            .setBodyDelay(5, TimeUnit.SECONDS));

        // Act
        Health health = healthIndicator.health();

        // Assert - Should return DOWN due to timeouts
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }

    @Test
    @DisplayName("Should include response time in service details")
    void shouldIncludeResponseTimeInServiceDetails() {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"status\":\"UP\"}")
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"status\":\"UP\"}")
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"status\":\"UP\"}")
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

        // Act
        Health health = healthIndicator.health();

        // Assert - Verify response time is included in details
        @SuppressWarnings("unchecked")
        Map<String, Object> clientesDetails = (Map<String, Object>) health.getDetails().get("clientes-service");
        assertThat(clientesDetails).containsKey("responseTime");
        assertThat(clientesDetails.get("responseTime").toString()).endsWith("ms");

        @SuppressWarnings("unchecked")
        Map<String, Object> facturacionDetails = (Map<String, Object>) health.getDetails().get("facturacion-service");
        assertThat(facturacionDetails).containsKey("responseTime");

        @SuppressWarnings("unchecked")
        Map<String, Object> taxCalculatorDetails = (Map<String, Object>) health.getDetails().get("tax-calculator-service");
        assertThat(taxCalculatorDetails).containsKey("responseTime");
    }

    @Test
    @DisplayName("Should include status in service details")
    void shouldIncludeStatusInServiceDetails() {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"status\":\"UP\"}")
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"status\":\"DOWN\"}")
            .setHeader("Content-Type", "application/json")
            .setResponseCode(503));

        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"status\":\"UP\"}")
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

        // Act
        Health health = healthIndicator.health();

        // Assert
        @SuppressWarnings("unchecked")
        Map<String, Object> clientesDetails = (Map<String, Object>) health.getDetails().get("clientes-service");
        assertThat(clientesDetails.get("status")).isEqualTo("UP");

        @SuppressWarnings("unchecked")
        Map<String, Object> facturacionDetails = (Map<String, Object>) health.getDetails().get("facturacion-service");
        assertThat(facturacionDetails.get("status")).isEqualTo("DOWN");

        @SuppressWarnings("unchecked")
        Map<String, Object> taxCalculatorDetails = (Map<String, Object>) health.getDetails().get("tax-calculator-service");
        assertThat(taxCalculatorDetails.get("status")).isEqualTo("UP");
    }

    @Test
    @DisplayName("Should handle service with health check disabled")
    void shouldHandleServiceWithHealthCheckDisabled() {
        // Arrange - Disable health check for clientes service
        gatewayProperties.getServices().getClientes().setHealthCheckEnabled(false);

        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"status\":\"UP\"}")
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"status\":\"UP\"}")
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

        // Act
        Health health = healthIndicator.health();

        // Assert - Should not include clientes-service in details
        assertThat(health.getDetails()).doesNotContainKey("clientes-service");
        assertThat(health.getDetails()).containsKeys("facturacion-service", "tax-calculator-service");
    }

    @Test
    @DisplayName("Should handle service with null URL")
    void shouldHandleServiceWithNullUrl() {
        // Arrange - Set null URL for one service
        gatewayProperties.getServices().getClientes().setUrl(null);

        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"status\":\"UP\"}")
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"status\":\"UP\"}")
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

        // Act
        Health health = healthIndicator.health();

        // Assert - Should handle gracefully and not crash
        assertThat(health.getDetails()).containsKey("clientes-service");

        @SuppressWarnings("unchecked")
        Map<String, Object> clientesDetails = (Map<String, Object>) health.getDetails().get("clientes-service");
        assertThat(clientesDetails.get("status")).isEqualTo("Not configured");
    }

    @Test
    @DisplayName("Should handle service with empty URL")
    void shouldHandleServiceWithEmptyUrl() {
        // Arrange - Set empty URL for one service
        gatewayProperties.getServices().getTaxCalculator().setUrl("");

        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"status\":\"UP\"}")
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"status\":\"UP\"}")
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

        // Act
        Health health = healthIndicator.health();

        // Assert - Should handle gracefully
        assertThat(health.getDetails()).containsKey("tax-calculator-service");

        @SuppressWarnings("unchecked")
        Map<String, Object> taxCalculatorDetails = (Map<String, Object>) health.getDetails().get("tax-calculator-service");
        assertThat(taxCalculatorDetails.get("status")).isEqualTo("Not configured");
    }

    @Test
    @DisplayName("Should parse health response with UP status")
    void shouldParseHealthResponseWithUpStatus() {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
            .setBody("UP")
            .setHeader("Content-Type", "text/plain")
            .setResponseCode(200));

        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"status\":\"UP\"}")
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"status\":\"UP\"}")
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

        // Act
        Health health = healthIndicator.health();

        // Assert - Should parse both plain text and JSON responses
        assertThat(health.getStatus()).isEqualTo(Status.UP);
    }

    @Test
    @DisplayName("Should handle mixed service states correctly")
    void shouldHandleMixedServiceStatesCorrectly() {
        // Arrange - Mix of UP, DOWN, and error responses
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"status\":\"UP\"}")
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(500)
            .setBody("Internal Server Error"));

        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"status\":\"UP\"}")
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

        // Act
        Health health = healthIndicator.health();

        // Assert - Overall health should be DOWN
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);

        @SuppressWarnings("unchecked")
        Map<String, Object> clientesDetails = (Map<String, Object>) health.getDetails().get("clientes-service");
        assertThat(clientesDetails.get("status")).isEqualTo("UP");

        @SuppressWarnings("unchecked")
        Map<String, Object> facturacionDetails = (Map<String, Object>) health.getDetails().get("facturacion-service");
        assertThat(facturacionDetails.get("status")).isEqualTo("DOWN");

        @SuppressWarnings("unchecked")
        Map<String, Object> taxCalculatorDetails = (Map<String, Object>) health.getDetails().get("tax-calculator-service");
        assertThat(taxCalculatorDetails.get("status")).isEqualTo("UP");
    }

    @Test
    @DisplayName("Should return UP when all health checks are disabled")
    void shouldReturnUpWhenAllHealthChecksAreDisabled() {
        // Arrange - Disable all health checks
        gatewayProperties.getServices().getClientes().setHealthCheckEnabled(false);
        gatewayProperties.getServices().getFacturacion().setHealthCheckEnabled(false);
        gatewayProperties.getServices().getTaxCalculator().setHealthCheckEnabled(false);

        // Act
        Health health = healthIndicator.health();

        // Assert - Should return UP with no service details
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("message");
    }
}
