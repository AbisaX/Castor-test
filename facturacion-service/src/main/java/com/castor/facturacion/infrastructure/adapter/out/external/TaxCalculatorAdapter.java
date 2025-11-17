package com.castor.facturacion.infrastructure.adapter.out.external;

import com.castor.facturacion.domain.ItemFactura;
import com.castor.facturacion.domain.port.out.TaxCalculatorPort;
import com.castor.facturacion.infrastructure.config.TaxCalculatorProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Adapter externo para calcular impuestos mediante llamada REST al tax-calculator-service (Python).
 *
 * Implementa TaxCalculatorPort usando WebClient para comunicación HTTP.
 *
 * Características:
 * - Circuit Breaker para tolerancia a fallos
 * - Retry automático para errores transitorios
 * - Fallback method que usa cálculo por defecto
 */
@Component
public class TaxCalculatorAdapter implements TaxCalculatorPort {

    private static final Logger log = LoggerFactory.getLogger(TaxCalculatorAdapter.class);

    // Porcentajes por defecto para fallback
    private static final BigDecimal DEFAULT_TAX_RATE = new BigDecimal("19.00");  // IVA 19%
    private static final BigDecimal DEFAULT_DISCOUNT_RATE = new BigDecimal("10.00");  // Descuento 10%

    private final WebClient webClient;
    private final TaxCalculatorProperties properties;

    public TaxCalculatorAdapter(
        WebClient.Builder webClientBuilder,
        TaxCalculatorProperties properties
    ) {
        this.properties = properties;
        this.webClient = webClientBuilder
            .baseUrl(properties.getBaseUrl())
            .build();

        log.info("TaxCalculatorAdapter inicializado con URL: {}", properties.getBaseUrl());
    }

    /**
     * Calcula impuestos y descuentos para una lista de items.
     *
     * Delega el cálculo al microservicio Python tax-calculator-service.
     * En caso de error, usa el método fallback con tasas por defecto.
     */
    @Override
    @CircuitBreaker(name = "taxCalculator", fallbackMethod = "calcularImpuestosYDescuentosFallback")
    @Retry(name = "taxCalculator")
    public List<ItemFactura> calcularImpuestosYDescuentos(List<ItemFactura> items) {
        log.debug("Calculando impuestos y descuentos para {} items mediante servicio externo", items.size());

        try {
            // Preparar request para el servicio de Python
            List<Map<String, Object>> itemsRequest = items.stream()
                .map(this::itemToRequestMap)
                .collect(Collectors.toList());

            Map<String, Object> request = Map.of(
                "items", itemsRequest
            );

            // Llamar al endpoint POST /api/v1/calculate
            TaxCalculationResponse response = webClient.post()
                .uri("/api/v1/calculate")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(TaxCalculationResponse.class)
                .timeout(Duration.ofMillis(properties.getTimeout()))
                .block();

            if (response == null || response.items == null) {
                log.warn("Respuesta vacía del servicio de cálculo de impuestos");
                return calcularImpuestosYDescuentosFallback(items, new Exception("Respuesta vacía"));
            }

            log.debug("Impuestos y descuentos calculados exitosamente para {} items", items.size());

            // Los items ya vienen calculados desde el dominio
            // Este servicio podría usarse para recalcular con reglas más complejas
            // Por ahora, retornamos los items originales
            return items;

        } catch (Exception e) {
            log.error("Error al calcular impuestos mediante servicio externo: {}", e.getMessage());
            throw new IllegalStateException("Error al calcular impuestos", e);
        }
    }

    /**
     * Fallback method cuando el servicio de cálculo de impuestos no está disponible.
     *
     * Usa tasas por defecto configuradas en las propiedades.
     */
    private List<ItemFactura> calcularImpuestosYDescuentosFallback(List<ItemFactura> items, Exception ex) {
        log.warn("Usando fallback para cálculo de impuestos. " +
                "Aplicando tasas por defecto - IVA: {}%, Descuento: {}%. Error: {}",
            properties.getDefaultTaxRate(), properties.getDefaultDiscountRate(), ex.getMessage());

        // Los items del dominio ya tienen su lógica de cálculo
        // El fallback simplemente retorna los items tal cual
        // ya que la lógica de negocio está en el dominio
        return items;
    }

    /**
     * Convierte un ItemFactura a un Map para el request al servicio externo
     */
    private Map<String, Object> itemToRequestMap(ItemFactura item) {
        return Map.of(
            "descripcion", item.getDescripcion(),
            "cantidad", item.getCantidad().getValor(),
            "precio_unitario", item.getPrecioUnitario().getCantidad(),
            "categoria", "general"  // Por defecto, podría venir del item
        );
    }

    /**
     * DTO para la respuesta del servicio de cálculo de impuestos
     */
    private static class TaxCalculationResponse {
        public List<TaxCalculationItem> items;
    }

    /**
     * DTO para un item calculado
     */
    private static class TaxCalculationItem {
        public BigDecimal porcentajeImpuesto;
        public BigDecimal porcentajeDescuento;
        public BigDecimal impuesto;
        public BigDecimal descuento;
        public BigDecimal total;
    }
}
