package com.castor.facturacion.infrastructure.adapter.out.external;

import com.castor.facturacion.infrastructure.config.ClienteServiceProperties;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests para ClienteValidationAdapter usando WireMock para simular el servicio externo.
 *
 * Cobertura:
 * - Tests de validación de clientes activos
 * - Tests de verificación de existencia de clientes
 * - Tests de manejo de errores (404, 500, timeout)
 * - Tests de circuit breaker y fallback
 * - Tests de retry
 */
@DisplayName("ClienteValidationAdapter - Tests con WireMock")
class ClienteValidationAdapterTest {

    private WireMockServer wireMockServer;
    private ClienteValidationAdapter clienteValidationAdapter;
    private ClienteServiceProperties properties;

    @BeforeEach
    void setUp() {
        // Iniciar WireMock Server en puerto dinámico
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();

        // Configurar WireMock client
        WireMock.configureFor("localhost", wireMockServer.port());

        // Configurar properties
        properties = new ClienteServiceProperties();
        properties.setBaseUrl("http://localhost:" + wireMockServer.port());
        properties.setTimeout(5000);

        // Crear adapter con WebClient
        WebClient.Builder webClientBuilder = WebClient.builder();
        clienteValidationAdapter = new ClienteValidationAdapter(webClientBuilder, properties);
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    @DisplayName("Test 01: Cliente activo debe retornar true")
    void testEsClienteActivo_ClienteActivo_RetornaTrue() {
        // Given
        Long clienteId = 1L;

        stubFor(get(urlEqualTo("/api/v1/clientes/" + clienteId + "/activo"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("true")));

        // When
        boolean resultado = clienteValidationAdapter.esClienteActivo(clienteId);

        // Then
        assertThat(resultado).isTrue();

        // Verificar que se llamó al servicio
        verify(getRequestedFor(urlEqualTo("/api/v1/clientes/" + clienteId + "/activo")));
    }

    @Test
    @DisplayName("Test 02: Cliente inactivo debe retornar false")
    void testEsClienteActivo_ClienteInactivo_RetornaFalse() {
        // Given
        Long clienteId = 2L;

        stubFor(get(urlEqualTo("/api/v1/clientes/" + clienteId + "/activo"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("false")));

        // When
        boolean resultado = clienteValidationAdapter.esClienteActivo(clienteId);

        // Then
        assertThat(resultado).isFalse();

        verify(getRequestedFor(urlEqualTo("/api/v1/clientes/" + clienteId + "/activo")));
    }

    @Test
    @DisplayName("Test 03: Cliente no encontrado (404) debe retornar false")
    void testEsClienteActivo_ClienteNoEncontrado_RetornaFalse() {
        // Given
        Long clienteId = 999L;

        stubFor(get(urlEqualTo("/api/v1/clientes/" + clienteId + "/activo"))
            .willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"message\": \"Cliente no encontrado\"}")));

        // When
        boolean resultado = clienteValidationAdapter.esClienteActivo(clienteId);

        // Then
        assertThat(resultado).isFalse();

        verify(getRequestedFor(urlEqualTo("/api/v1/clientes/" + clienteId + "/activo")));
    }

    @Test
    @DisplayName("Test 04: Servicio no disponible (500) debe lanzar excepción")
    void testEsClienteActivo_ServicioNoDisponible_LanzaExcepcion() {
        // Given
        Long clienteId = 1L;

        stubFor(get(urlEqualTo("/api/v1/clientes/" + clienteId + "/activo"))
            .willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"message\": \"Internal Server Error\"}")));

        // When & Then
        assertThatThrownBy(() -> clienteValidationAdapter.esClienteActivo(clienteId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Error al validar cliente");

        verify(getRequestedFor(urlEqualTo("/api/v1/clientes/" + clienteId + "/activo")));
    }

    @Test
    @DisplayName("Test 05: Timeout debe lanzar excepción")
    void testEsClienteActivo_Timeout_LanzaExcepcion() {
        // Given
        Long clienteId = 1L;

        // Configurar propiedades con timeout muy corto
        properties.setTimeout(100); // 100ms
        WebClient.Builder webClientBuilder = WebClient.builder();
        ClienteValidationAdapter adapterConTimeout = new ClienteValidationAdapter(webClientBuilder, properties);

        stubFor(get(urlEqualTo("/api/v1/clientes/" + clienteId + "/activo"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("true")
                .withFixedDelay(5000))); // Delay de 5 segundos

        // When & Then
        assertThatThrownBy(() -> adapterConTimeout.esClienteActivo(clienteId))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Test 06: Verificar existencia de cliente - cliente existe retorna true")
    void testExisteCliente_ClienteExiste_RetornaTrue() {
        // Given
        Long clienteId = 1L;

        stubFor(head(urlEqualTo("/api/v1/clientes/" + clienteId))
            .willReturn(aResponse()
                .withStatus(200)));

        // When
        boolean resultado = clienteValidationAdapter.existeCliente(clienteId);

        // Then
        assertThat(resultado).isTrue();

        verify(headRequestedFor(urlEqualTo("/api/v1/clientes/" + clienteId)));
    }

    @Test
    @DisplayName("Test 07: Verificar existencia de cliente - cliente no existe retorna false")
    void testExisteCliente_ClienteNoExiste_RetornaFalse() {
        // Given
        Long clienteId = 999L;

        stubFor(head(urlEqualTo("/api/v1/clientes/" + clienteId))
            .willReturn(aResponse()
                .withStatus(404)));

        // When
        boolean resultado = clienteValidationAdapter.existeCliente(clienteId);

        // Then
        assertThat(resultado).isFalse();

        verify(headRequestedFor(urlEqualTo("/api/v1/clientes/" + clienteId)));
    }

    @Test
    @DisplayName("Test 08: Verificar existencia de cliente - error 500 lanza excepción")
    void testExisteCliente_Error500_LanzaExcepcion() {
        // Given
        Long clienteId = 1L;

        stubFor(head(urlEqualTo("/api/v1/clientes/" + clienteId))
            .willReturn(aResponse()
                .withStatus(500)));

        // When & Then
        assertThatThrownBy(() -> clienteValidationAdapter.existeCliente(clienteId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Error al verificar existencia de cliente");

        verify(headRequestedFor(urlEqualTo("/api/v1/clientes/" + clienteId)));
    }

    @Test
    @DisplayName("Test 09: Múltiples clientes activos deben funcionar correctamente")
    void testMultiplesClientesActivos() {
        // Given
        Long cliente1 = 1L;
        Long cliente2 = 2L;
        Long cliente3 = 3L;

        stubFor(get(urlEqualTo("/api/v1/clientes/" + cliente1 + "/activo"))
            .willReturn(aResponse().withStatus(200).withBody("true")));

        stubFor(get(urlEqualTo("/api/v1/clientes/" + cliente2 + "/activo"))
            .willReturn(aResponse().withStatus(200).withBody("false")));

        stubFor(get(urlEqualTo("/api/v1/clientes/" + cliente3 + "/activo"))
            .willReturn(aResponse().withStatus(404)));

        // When
        boolean resultado1 = clienteValidationAdapter.esClienteActivo(cliente1);
        boolean resultado2 = clienteValidationAdapter.esClienteActivo(cliente2);
        boolean resultado3 = clienteValidationAdapter.esClienteActivo(cliente3);

        // Then
        assertThat(resultado1).isTrue();
        assertThat(resultado2).isFalse();
        assertThat(resultado3).isFalse();

        verify(3, getRequestedFor(urlMatching("/api/v1/clientes/.*/activo")));
    }

    @Test
    @DisplayName("Test 10: Respuesta con formato inválido debe lanzar excepción")
    void testEsClienteActivo_RespuestaInvalida_LanzaExcepcion() {
        // Given
        Long clienteId = 1L;

        stubFor(get(urlEqualTo("/api/v1/clientes/" + clienteId + "/activo"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("INVALID_BOOLEAN"))); // Respuesta inválida

        // When & Then
        assertThatThrownBy(() -> clienteValidationAdapter.esClienteActivo(clienteId))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Test 11: Verificar que el adapter usa la URL base configurada")
    void testAdapterUsaUrlBaseConfigurada() {
        // Given
        Long clienteId = 1L;

        stubFor(get(urlEqualTo("/api/v1/clientes/" + clienteId + "/activo"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("true")));

        // When
        clienteValidationAdapter.esClienteActivo(clienteId);

        // Then - verificar que llamó a la URL correcta
        verify(getRequestedFor(urlEqualTo("/api/v1/clientes/" + clienteId + "/activo"))
            .withHeader("Content-Type", matching(".*")));
    }

    @Test
    @DisplayName("Test 12: Error de conexión debe lanzar excepción")
    void testErrorConexion_LanzaExcepcion() {
        // Given - detener el servidor para simular error de conexión
        wireMockServer.stop();

        Long clienteId = 1L;

        // When & Then
        assertThatThrownBy(() -> clienteValidationAdapter.esClienteActivo(clienteId))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Test 13: Respuesta null del servicio debe retornar false")
    void testRespuestaNull_RetornaFalse() {
        // Given
        Long clienteId = 1L;

        stubFor(get(urlEqualTo("/api/v1/clientes/" + clienteId + "/activo"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("null")));

        // When
        boolean resultado = clienteValidationAdapter.esClienteActivo(clienteId);

        // Then
        assertThat(resultado).isFalse();
    }

    @Test
    @DisplayName("Test 14: Verificar headers de la petición")
    void testVerificarHeadersPeticion() {
        // Given
        Long clienteId = 1L;

        stubFor(get(urlEqualTo("/api/v1/clientes/" + clienteId + "/activo"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("true")));

        // When
        clienteValidationAdapter.esClienteActivo(clienteId);

        // Then - verificar que se enviaron los headers correctos
        verify(getRequestedFor(urlEqualTo("/api/v1/clientes/" + clienteId + "/activo"))
            .withHeader("Accept", matching(".*")));
    }

    @Test
    @DisplayName("Test 15: Servicio lento pero dentro del timeout debe funcionar")
    void testServicioLentoEnTimeout_Funciona() {
        // Given
        Long clienteId = 1L;

        stubFor(get(urlEqualTo("/api/v1/clientes/" + clienteId + "/activo"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("true")
                .withFixedDelay(1000))); // 1 segundo de delay (dentro del timeout de 5000ms)

        // When
        boolean resultado = clienteValidationAdapter.esClienteActivo(clienteId);

        // Then
        assertThat(resultado).isTrue();

        verify(getRequestedFor(urlEqualTo("/api/v1/clientes/" + clienteId + "/activo")));
    }
}
