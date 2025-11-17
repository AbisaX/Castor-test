package com.castor.gateway.controller;

import com.castor.gateway.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for FallbackController.
 *
 * Tests fallback endpoints for circuit breaker including:
 * - GET /fallback/clientes returns 503 with error message
 * - GET /fallback/facturacion returns 503 with error message
 * - GET /fallback/tax-calculator returns 503 with error message
 * - Proper error response format
 * - Service unavailable messages
 */
@DisplayName("FallbackController Tests")
class FallbackControllerTest {

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        FallbackController fallbackController = new FallbackController();
        webTestClient = WebTestClient.bindToController(fallbackController).build();
    }

    @Test
    @DisplayName("Should return 503 for clientes service fallback")
    void shouldReturn503ForClientesFallback() {
        // Act & Assert
        webTestClient.get()
            .uri("/fallback/clientes")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(ErrorResponse.class)
            .value(errorResponse -> {
                assertThat(errorResponse.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
                assertThat(errorResponse.getError()).isEqualTo("Service Unavailable");
                assertThat(errorResponse.getMessage()).contains("Clientes service");
                assertThat(errorResponse.getMessage()).contains("unavailable");
                assertThat(errorResponse.getMessage()).contains("try again later");
                assertThat(errorResponse.getPath()).isEqualTo("/fallback/clientes");
                assertThat(errorResponse.getTimestamp()).isNotNull();
            });
    }

    @Test
    @DisplayName("Should return 503 for facturacion service fallback")
    void shouldReturn503ForFacturacionFallback() {
        // Act & Assert
        webTestClient.get()
            .uri("/fallback/facturacion")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(ErrorResponse.class)
            .value(errorResponse -> {
                assertThat(errorResponse.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
                assertThat(errorResponse.getError()).isEqualTo("Service Unavailable");
                assertThat(errorResponse.getMessage()).contains("Facturacion service");
                assertThat(errorResponse.getMessage()).contains("unavailable");
                assertThat(errorResponse.getMessage()).contains("try again later");
                assertThat(errorResponse.getPath()).isEqualTo("/fallback/facturacion");
                assertThat(errorResponse.getTimestamp()).isNotNull();
            });
    }

    @Test
    @DisplayName("Should return 503 for tax-calculator service fallback")
    void shouldReturn503ForTaxCalculatorFallback() {
        // Act & Assert
        webTestClient.get()
            .uri("/fallback/tax-calculator")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(ErrorResponse.class)
            .value(errorResponse -> {
                assertThat(errorResponse.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
                assertThat(errorResponse.getError()).isEqualTo("Service Unavailable");
                assertThat(errorResponse.getMessage()).contains("Tax Calculator service");
                assertThat(errorResponse.getMessage()).contains("unavailable");
                assertThat(errorResponse.getMessage()).contains("try again later");
                assertThat(errorResponse.getPath()).isEqualTo("/fallback/tax-calculator");
                assertThat(errorResponse.getTimestamp()).isNotNull();
            });
    }

    @Test
    @DisplayName("Should include error details in clientes fallback response")
    void shouldIncludeErrorDetailsInClientesFallbackResponse() {
        // Act & Assert
        webTestClient.get()
            .uri("/fallback/clientes")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
            .expectBody()
            .jsonPath("$.status").isEqualTo(503)
            .jsonPath("$.error").isEqualTo("Service Unavailable")
            .jsonPath("$.message").isNotEmpty()
            .jsonPath("$.path").isEqualTo("/fallback/clientes")
            .jsonPath("$.timestamp").isNotEmpty();
    }

    @Test
    @DisplayName("Should include error details in facturacion fallback response")
    void shouldIncludeErrorDetailsInFacturacionFallbackResponse() {
        // Act & Assert
        webTestClient.get()
            .uri("/fallback/facturacion")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
            .expectBody()
            .jsonPath("$.status").isEqualTo(503)
            .jsonPath("$.error").isEqualTo("Service Unavailable")
            .jsonPath("$.message").isNotEmpty()
            .jsonPath("$.path").isEqualTo("/fallback/facturacion")
            .jsonPath("$.timestamp").isNotEmpty();
    }

    @Test
    @DisplayName("Should include error details in tax-calculator fallback response")
    void shouldIncludeErrorDetailsInTaxCalculatorFallbackResponse() {
        // Act & Assert
        webTestClient.get()
            .uri("/fallback/tax-calculator")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
            .expectBody()
            .jsonPath("$.status").isEqualTo(503)
            .jsonPath("$.error").isEqualTo("Service Unavailable")
            .jsonPath("$.message").isNotEmpty()
            .jsonPath("$.path").isEqualTo("/fallback/tax-calculator")
            .jsonPath("$.timestamp").isNotEmpty();
    }

    @Test
    @DisplayName("Should return correct content type for all fallback endpoints")
    void shouldReturnCorrectContentTypeForAllFallbackEndpoints() {
        // Act & Assert - Test all endpoints
        webTestClient.get()
            .uri("/fallback/clientes")
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON);

        webTestClient.get()
            .uri("/fallback/facturacion")
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON);

        webTestClient.get()
            .uri("/fallback/tax-calculator")
            .exchange()
            .expectHeader().contentType(MediaType.APPLICATION_JSON);
    }

    @Test
    @DisplayName("Should have different messages for each service")
    void shouldHaveDifferentMessagesForEachService() {
        // Arrange & Act - Get responses from all endpoints
        ErrorResponse clientesResponse = webTestClient.get()
            .uri("/fallback/clientes")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
            .expectBody(ErrorResponse.class)
            .returnResult()
            .getResponseBody();

        ErrorResponse facturacionResponse = webTestClient.get()
            .uri("/fallback/facturacion")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
            .expectBody(ErrorResponse.class)
            .returnResult()
            .getResponseBody();

        ErrorResponse taxCalculatorResponse = webTestClient.get()
            .uri("/fallback/tax-calculator")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
            .expectBody(ErrorResponse.class)
            .returnResult()
            .getResponseBody();

        // Assert - Verify each endpoint has a unique message
        assertThat(clientesResponse.getMessage()).isNotEqualTo(facturacionResponse.getMessage());
        assertThat(clientesResponse.getMessage()).isNotEqualTo(taxCalculatorResponse.getMessage());
        assertThat(facturacionResponse.getMessage()).isNotEqualTo(taxCalculatorResponse.getMessage());
    }

    @Test
    @DisplayName("Should have correct paths for all fallback endpoints")
    void shouldHaveCorrectPathsForAllFallbackEndpoints() {
        // Act & Assert
        webTestClient.get()
            .uri("/fallback/clientes")
            .exchange()
            .expectBody(ErrorResponse.class)
            .value(response -> assertThat(response.getPath()).isEqualTo("/fallback/clientes"));

        webTestClient.get()
            .uri("/fallback/facturacion")
            .exchange()
            .expectBody(ErrorResponse.class)
            .value(response -> assertThat(response.getPath()).isEqualTo("/fallback/facturacion"));

        webTestClient.get()
            .uri("/fallback/tax-calculator")
            .exchange()
            .expectBody(ErrorResponse.class)
            .value(response -> assertThat(response.getPath()).isEqualTo("/fallback/tax-calculator"));
    }

    @Test
    @DisplayName("Should return 503 status code in response body for clientes")
    void shouldReturn503StatusCodeInResponseBodyForClientes() {
        // Act & Assert
        webTestClient.get()
            .uri("/fallback/clientes")
            .exchange()
            .expectBody(ErrorResponse.class)
            .value(response -> assertThat(response.getStatus()).isEqualTo(503));
    }

    @Test
    @DisplayName("Should return 503 status code in response body for facturacion")
    void shouldReturn503StatusCodeInResponseBodyForFacturacion() {
        // Act & Assert
        webTestClient.get()
            .uri("/fallback/facturacion")
            .exchange()
            .expectBody(ErrorResponse.class)
            .value(response -> assertThat(response.getStatus()).isEqualTo(503));
    }

    @Test
    @DisplayName("Should return 503 status code in response body for tax-calculator")
    void shouldReturn503StatusCodeInResponseBodyForTaxCalculator() {
        // Act & Assert
        webTestClient.get()
            .uri("/fallback/tax-calculator")
            .exchange()
            .expectBody(ErrorResponse.class)
            .value(response -> assertThat(response.getStatus()).isEqualTo(503));
    }

    @Test
    @DisplayName("Should have Service Unavailable error type for all endpoints")
    void shouldHaveServiceUnavailableErrorTypeForAllEndpoints() {
        // Act & Assert
        webTestClient.get()
            .uri("/fallback/clientes")
            .exchange()
            .expectBody(ErrorResponse.class)
            .value(response -> assertThat(response.getError()).isEqualTo("Service Unavailable"));

        webTestClient.get()
            .uri("/fallback/facturacion")
            .exchange()
            .expectBody(ErrorResponse.class)
            .value(response -> assertThat(response.getError()).isEqualTo("Service Unavailable"));

        webTestClient.get()
            .uri("/fallback/tax-calculator")
            .exchange()
            .expectBody(ErrorResponse.class)
            .value(response -> assertThat(response.getError()).isEqualTo("Service Unavailable"));
    }

    @Test
    @DisplayName("Should include timestamp in all fallback responses")
    void shouldIncludeTimestampInAllFallbackResponses() {
        // Act & Assert
        webTestClient.get()
            .uri("/fallback/clientes")
            .exchange()
            .expectBody(ErrorResponse.class)
            .value(response -> assertThat(response.getTimestamp()).isNotNull());

        webTestClient.get()
            .uri("/fallback/facturacion")
            .exchange()
            .expectBody(ErrorResponse.class)
            .value(response -> assertThat(response.getTimestamp()).isNotNull());

        webTestClient.get()
            .uri("/fallback/tax-calculator")
            .exchange()
            .expectBody(ErrorResponse.class)
            .value(response -> assertThat(response.getTimestamp()).isNotNull());
    }
}
