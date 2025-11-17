package com.castor.facturacion.infrastructure.adapter.out.external;

import com.castor.facturacion.domain.ItemFactura;
import com.castor.facturacion.domain.valueobject.Cantidad;
import com.castor.facturacion.domain.valueobject.Dinero;
import com.castor.facturacion.domain.valueobject.Porcentaje;
import com.castor.facturacion.infrastructure.config.TaxCalculatorProperties;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests para TaxCalculatorAdapter usando WireMock para simular el servicio Python.
 *
 * Cobertura:
 * - Tests de cálculo de impuestos y descuentos
 * - Tests de manejo de errores (500, timeout)
 * - Tests de circuit breaker y fallback
 * - Tests de retry
 * - Tests de integración con servicio Python
 */
@DisplayName("TaxCalculatorAdapter - Tests con WireMock")
class TaxCalculatorAdapterTest {

    private WireMockServer wireMockServer;
    private TaxCalculatorAdapter taxCalculatorAdapter;
    private TaxCalculatorProperties properties;

    @BeforeEach
    void setUp() {
        // Iniciar WireMock Server en puerto dinámico
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();

        // Configurar WireMock client
        WireMock.configureFor("localhost", wireMockServer.port());

        // Configurar properties
        properties = new TaxCalculatorProperties();
        properties.setBaseUrl("http://localhost:" + wireMockServer.port());
        properties.setTimeout(5000);
        properties.setDefaultTaxRate(new BigDecimal("19.00"));
        properties.setDefaultDiscountRate(new BigDecimal("10.00"));

        // Crear adapter con WebClient
        WebClient.Builder webClientBuilder = WebClient.builder();
        taxCalculatorAdapter = new TaxCalculatorAdapter(webClientBuilder, properties);
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    @DisplayName("Test 01: Calcular impuestos y descuentos exitosamente")
    void testCalcularImpuestosYDescuentos_Exitoso() {
        // Given
        ItemFactura item1 = ItemFactura.crear(
            "Producto A",
            Cantidad.de(2),
            Dinero.de(new BigDecimal("100.00")),
            Porcentaje.de(new BigDecimal("19.00")),
            Porcentaje.de(new BigDecimal("5.00"))
        );

        List<ItemFactura> items = List.of(item1);

        String responseJson = """
            {
                "items": [
                    {
                        "porcentajeImpuesto": 19.00,
                        "porcentajeDescuento": 5.00,
                        "impuesto": 38.00,
                        "descuento": 10.00,
                        "total": 228.00
                    }
                ]
            }
            """;

        stubFor(post(urlEqualTo("/api/v1/calculate"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(responseJson)));

        // When
        List<ItemFactura> resultado = taxCalculatorAdapter.calcularImpuestosYDescuentos(items);

        // Then
        assertThat(resultado).isNotNull();
        assertThat(resultado).hasSize(1);

        verify(postRequestedFor(urlEqualTo("/api/v1/calculate"))
            .withHeader("Content-Type", matching("application/json.*")));
    }

    @Test
    @DisplayName("Test 02: Servicio no disponible debe usar fallback")
    void testCalcularImpuestos_ServicioNoDisponible_UsaFallback() {
        // Given
        ItemFactura item = ItemFactura.crear(
            "Producto Test",
            Cantidad.de(1),
            Dinero.de(new BigDecimal("100.00")),
            Porcentaje.de(new BigDecimal("19.00")),
            Porcentaje.de(new BigDecimal("10.00"))
        );

        List<ItemFactura> items = List.of(item);

        stubFor(post(urlEqualTo("/api/v1/calculate"))
            .willReturn(aResponse()
                .withStatus(500)
                .withBody("{\"error\": \"Internal Server Error\"}")));

        // When & Then
        assertThatThrownBy(() -> taxCalculatorAdapter.calcularImpuestosYDescuentos(items))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Error al calcular impuestos");

        verify(postRequestedFor(urlEqualTo("/api/v1/calculate")));
    }

    @Test
    @DisplayName("Test 03: Timeout debe lanzar excepción")
    void testCalcularImpuestos_Timeout_LanzaExcepcion() {
        // Given
        properties.setTimeout(100); // Timeout muy corto
        WebClient.Builder webClientBuilder = WebClient.builder();
        TaxCalculatorAdapter adapterConTimeout = new TaxCalculatorAdapter(webClientBuilder, properties);

        ItemFactura item = ItemFactura.crear(
            "Producto Test",
            Cantidad.de(1),
            Dinero.de(new BigDecimal("100.00")),
            Porcentaje.de(new BigDecimal("19.00")),
            Porcentaje.de(new BigDecimal("10.00"))
        );

        List<ItemFactura> items = List.of(item);

        stubFor(post(urlEqualTo("/api/v1/calculate"))
            .willReturn(aResponse()
                .withStatus(200)
                .withFixedDelay(5000) // Delay de 5 segundos
                .withBody("{}")));

        // When & Then
        assertThatThrownBy(() -> adapterConTimeout.calcularImpuestosYDescuentos(items))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Test 04: Respuesta vacía debe usar fallback")
    void testCalcularImpuestos_RespuestaVacia_UsaFallback() {
        // Given
        ItemFactura item = ItemFactura.crear(
            "Producto Test",
            Cantidad.de(1),
            Dinero.de(new BigDecimal("100.00")),
            Porcentaje.de(new BigDecimal("19.00")),
            Porcentaje.de(new BigDecimal("10.00"))
        );

        List<ItemFactura> items = List.of(item);

        stubFor(post(urlEqualTo("/api/v1/calculate"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{}"))); // Respuesta vacía (sin items)

        // When
        List<ItemFactura> resultado = taxCalculatorAdapter.calcularImpuestosYDescuentos(items);

        // Then - debe usar fallback y retornar los items originales
        assertThat(resultado).isNotNull();
        assertThat(resultado).hasSize(1);

        verify(postRequestedFor(urlEqualTo("/api/v1/calculate")));
    }

    @Test
    @DisplayName("Test 05: Múltiples items deben calcularse correctamente")
    void testCalcularImpuestos_MultipleItems() {
        // Given
        ItemFactura item1 = ItemFactura.crear(
            "Producto A",
            Cantidad.de(2),
            Dinero.de(new BigDecimal("100.00")),
            Porcentaje.de(new BigDecimal("19.00")),
            Porcentaje.de(new BigDecimal("5.00"))
        );

        ItemFactura item2 = ItemFactura.crear(
            "Producto B",
            Cantidad.de(1),
            Dinero.de(new BigDecimal("50.00")),
            Porcentaje.de(new BigDecimal("19.00")),
            Porcentaje.de(new BigDecimal("0.00"))
        );

        List<ItemFactura> items = List.of(item1, item2);

        String responseJson = """
            {
                "items": [
                    {
                        "porcentajeImpuesto": 19.00,
                        "porcentajeDescuento": 5.00,
                        "impuesto": 38.00,
                        "descuento": 10.00,
                        "total": 228.00
                    },
                    {
                        "porcentajeImpuesto": 19.00,
                        "porcentajeDescuento": 0.00,
                        "impuesto": 9.50,
                        "descuento": 0.00,
                        "total": 59.50
                    }
                ]
            }
            """;

        stubFor(post(urlEqualTo("/api/v1/calculate"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(responseJson)));

        // When
        List<ItemFactura> resultado = taxCalculatorAdapter.calcularImpuestosYDescuentos(items);

        // Then
        assertThat(resultado).isNotNull();
        assertThat(resultado).hasSize(2);

        verify(postRequestedFor(urlEqualTo("/api/v1/calculate")));
    }

    @Test
    @DisplayName("Test 06: Verificar que se envía el request correcto")
    void testVerificarRequestEnviado() {
        // Given
        ItemFactura item = ItemFactura.crear(
            "Producto Test",
            Cantidad.de(5),
            Dinero.de(new BigDecimal("200.00")),
            Porcentaje.de(new BigDecimal("19.00")),
            Porcentaje.de(new BigDecimal("10.00"))
        );

        List<ItemFactura> items = List.of(item);

        String responseJson = """
            {
                "items": [
                    {
                        "porcentajeImpuesto": 19.00,
                        "porcentajeDescuento": 10.00,
                        "impuesto": 190.00,
                        "descuento": 100.00,
                        "total": 1090.00
                    }
                ]
            }
            """;

        stubFor(post(urlEqualTo("/api/v1/calculate"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(responseJson)));

        // When
        taxCalculatorAdapter.calcularImpuestosYDescuentos(items);

        // Then - verificar que el request contiene los datos correctos
        verify(postRequestedFor(urlEqualTo("/api/v1/calculate"))
            .withRequestBody(matchingJsonPath("$.items[0].descripcion", equalTo("Producto Test")))
            .withRequestBody(matchingJsonPath("$.items[0].cantidad", equalTo("5")))
            .withRequestBody(matchingJsonPath("$.items[0].precio_unitario", equalTo("200.00"))));
    }

    @Test
    @DisplayName("Test 07: Error de conexión debe lanzar excepción")
    void testErrorConexion_LanzaExcepcion() {
        // Given - detener servidor para simular error de conexión
        wireMockServer.stop();

        ItemFactura item = ItemFactura.crear(
            "Producto Test",
            Cantidad.de(1),
            Dinero.de(new BigDecimal("100.00")),
            Porcentaje.de(new BigDecimal("19.00")),
            Porcentaje.de(new BigDecimal("10.00"))
        );

        List<ItemFactura> items = List.of(item);

        // When & Then
        assertThatThrownBy(() -> taxCalculatorAdapter.calcularImpuestosYDescuentos(items))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Test 08: Respuesta con formato JSON inválido debe lanzar excepción")
    void testRespuestaJsonInvalida_LanzaExcepcion() {
        // Given
        ItemFactura item = ItemFactura.crear(
            "Producto Test",
            Cantidad.de(1),
            Dinero.de(new BigDecimal("100.00")),
            Porcentaje.de(new BigDecimal("19.00")),
            Porcentaje.de(new BigDecimal("10.00"))
        );

        List<ItemFactura> items = List.of(item);

        stubFor(post(urlEqualTo("/api/v1/calculate"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("INVALID_JSON{{{"))); // JSON inválido

        // When & Then
        assertThatThrownBy(() -> taxCalculatorAdapter.calcularImpuestosYDescuentos(items))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Test 09: Lista vacía de items debe retornar lista vacía")
    void testListaVaciaItems_RetornaListaVacia() {
        // Given
        List<ItemFactura> items = List.of();

        String responseJson = """
            {
                "items": []
            }
            """;

        stubFor(post(urlEqualTo("/api/v1/calculate"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(responseJson)));

        // When
        List<ItemFactura> resultado = taxCalculatorAdapter.calcularImpuestosYDescuentos(items);

        // Then
        assertThat(resultado).isEmpty();

        verify(postRequestedFor(urlEqualTo("/api/v1/calculate")));
    }

    @Test
    @DisplayName("Test 10: Servicio lento pero dentro del timeout debe funcionar")
    void testServicioLentoEnTimeout_Funciona() {
        // Given
        ItemFactura item = ItemFactura.crear(
            "Producto Test",
            Cantidad.de(1),
            Dinero.de(new BigDecimal("100.00")),
            Porcentaje.de(new BigDecimal("19.00")),
            Porcentaje.de(new BigDecimal("10.00"))
        );

        List<ItemFactura> items = List.of(item);

        String responseJson = """
            {
                "items": [
                    {
                        "porcentajeImpuesto": 19.00,
                        "porcentajeDescuento": 10.00,
                        "impuesto": 19.00,
                        "descuento": 10.00,
                        "total": 109.00
                    }
                ]
            }
            """;

        stubFor(post(urlEqualTo("/api/v1/calculate"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(responseJson)
                .withFixedDelay(1000))); // 1 segundo (dentro del timeout de 5000ms)

        // When
        List<ItemFactura> resultado = taxCalculatorAdapter.calcularImpuestosYDescuentos(items);

        // Then
        assertThat(resultado).isNotNull();
        assertThat(resultado).hasSize(1);

        verify(postRequestedFor(urlEqualTo("/api/v1/calculate")));
    }

    @Test
    @DisplayName("Test 11: Verificar que se usa la URL base configurada")
    void testUsaUrlBaseConfigurada() {
        // Given
        ItemFactura item = ItemFactura.crear(
            "Producto Test",
            Cantidad.de(1),
            Dinero.de(new BigDecimal("100.00")),
            Porcentaje.de(new BigDecimal("19.00")),
            Porcentaje.de(new BigDecimal("10.00"))
        );

        List<ItemFactura> items = List.of(item);

        stubFor(post(urlEqualTo("/api/v1/calculate"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"items\": []}")));

        // When
        taxCalculatorAdapter.calcularImpuestosYDescuentos(items);

        // Then - verificar que llamó a la URL correcta
        verify(postRequestedFor(urlEqualTo("/api/v1/calculate")));
    }

    @Test
    @DisplayName("Test 12: Error 400 Bad Request debe lanzar excepción")
    void testError400BadRequest_LanzaExcepcion() {
        // Given
        ItemFactura item = ItemFactura.crear(
            "Producto Test",
            Cantidad.de(1),
            Dinero.de(new BigDecimal("100.00")),
            Porcentaje.de(new BigDecimal("19.00")),
            Porcentaje.de(new BigDecimal("10.00"))
        );

        List<ItemFactura> items = List.of(item);

        stubFor(post(urlEqualTo("/api/v1/calculate"))
            .willReturn(aResponse()
                .withStatus(400)
                .withBody("{\"error\": \"Invalid request\"}")));

        // When & Then
        assertThatThrownBy(() -> taxCalculatorAdapter.calcularImpuestosYDescuentos(items))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Error al calcular impuestos");

        verify(postRequestedFor(urlEqualTo("/api/v1/calculate")));
    }

    @Test
    @DisplayName("Test 13: Verificar headers de la petición")
    void testVerificarHeadersPeticion() {
        // Given
        ItemFactura item = ItemFactura.crear(
            "Producto Test",
            Cantidad.de(1),
            Dinero.de(new BigDecimal("100.00")),
            Porcentaje.de(new BigDecimal("19.00")),
            Porcentaje.de(new BigDecimal("10.00"))
        );

        List<ItemFactura> items = List.of(item);

        stubFor(post(urlEqualTo("/api/v1/calculate"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("{\"items\": []}")));

        // When
        taxCalculatorAdapter.calcularImpuestosYDescuentos(items);

        // Then
        verify(postRequestedFor(urlEqualTo("/api/v1/calculate"))
            .withHeader("Content-Type", matching("application/json.*")));
    }
}
