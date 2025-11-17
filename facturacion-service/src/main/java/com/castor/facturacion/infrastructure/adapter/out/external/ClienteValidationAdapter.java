package com.castor.facturacion.infrastructure.adapter.out.external;

import com.castor.facturacion.domain.port.out.ClienteValidationPort;
import com.castor.facturacion.infrastructure.config.ClienteServiceProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

/**
 * Adapter externo para validar clientes mediante llamada REST al clientes-service.
 *
 * Implementa ClienteValidationPort usando WebClient para comunicación HTTP.
 *
 * Características:
 * - Circuit Breaker para tolerancia a fallos
 * - Retry automático para errores transitorios
 * - Cache para reducir llamadas externas
 * - Fallback methods para manejo de errores
 */
@Component
public class ClienteValidationAdapter implements ClienteValidationPort {

    private static final Logger log = LoggerFactory.getLogger(ClienteValidationAdapter.class);

    private final WebClient webClient;
    private final ClienteServiceProperties properties;

    public ClienteValidationAdapter(
        WebClient.Builder webClientBuilder,
        ClienteServiceProperties properties
    ) {
        this.properties = properties;
        this.webClient = webClientBuilder
            .baseUrl(properties.getBaseUrl())
            .build();

        log.info("ClienteValidationAdapter inicializado con URL: {}", properties.getBaseUrl());
    }

    /**
     * Verifica si un cliente existe y está activo.
     *
     * Implementa Circuit Breaker, Retry y Cache para alta disponibilidad.
     */
    @Override
    @CircuitBreaker(name = "clienteService", fallbackMethod = "esClienteActivoFallback")
    @Retry(name = "clienteService")
    @Cacheable(value = "clientesActivos", key = "#clienteId", unless = "#result == false")
    public boolean esClienteActivo(Long clienteId) {
        log.debug("Validando si cliente {} está activo mediante REST", clienteId);

        try {
            // Llamar al endpoint GET /api/v1/clientes/{id}/activo
            Boolean resultado = webClient.get()
                .uri("/api/v1/clientes/{id}/activo", clienteId)
                .retrieve()
                .bodyToMono(Boolean.class)
                .timeout(Duration.ofMillis(properties.getTimeout()))
                .block();

            log.debug("Cliente {} activo: {}", clienteId, resultado);

            return Boolean.TRUE.equals(resultado);

        } catch (WebClientResponseException.NotFound e) {
            log.warn("Cliente {} no encontrado: {}", clienteId, e.getMessage());
            return false;

        } catch (WebClientResponseException e) {
            log.error("Error HTTP al validar cliente {}: {} - {}",
                clienteId, e.getStatusCode(), e.getMessage());
            throw new IllegalStateException("Error al validar cliente: " + e.getMessage(), e);

        } catch (Exception e) {
            log.error("Error inesperado al validar cliente {}: {}", clienteId, e.getMessage());
            throw new IllegalStateException("Error al comunicarse con el servicio de clientes", e);
        }
    }

    /**
     * Verifica si un cliente existe (sin importar si está activo o no).
     */
    @Override
    @CircuitBreaker(name = "clienteService", fallbackMethod = "existeClienteFallback")
    @Retry(name = "clienteService")
    @Cacheable(value = "clientesExistentes", key = "#clienteId")
    public boolean existeCliente(Long clienteId) {
        log.debug("Verificando existencia de cliente {} mediante REST", clienteId);

        try {
            // Llamar al endpoint HEAD /api/v1/clientes/{id}
            webClient.head()
                .uri("/api/v1/clientes/{id}", clienteId)
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofMillis(properties.getTimeout()))
                .block();

            log.debug("Cliente {} existe", clienteId);
            return true;

        } catch (WebClientResponseException.NotFound e) {
            log.debug("Cliente {} no existe", clienteId);
            return false;

        } catch (WebClientResponseException e) {
            log.error("Error HTTP al verificar existencia de cliente {}: {} - {}",
                clienteId, e.getStatusCode(), e.getMessage());
            throw new IllegalStateException("Error al verificar existencia de cliente", e);

        } catch (Exception e) {
            log.error("Error inesperado al verificar existencia de cliente {}: {}",
                clienteId, e.getMessage());
            throw new IllegalStateException("Error al comunicarse con el servicio de clientes", e);
        }
    }

    /**
     * Fallback method para esClienteActivo cuando el Circuit Breaker está abierto.
     *
     * Estrategia conservadora: retorna false para evitar crear facturas con clientes inválidos.
     */
    private boolean esClienteActivoFallback(Long clienteId, Exception ex) {
        log.error("Circuit Breaker abierto o error al validar cliente {}. " +
                 "Usando fallback (retornando false). Error: {}",
            clienteId, ex.getMessage());

        // Estrategia conservadora: no permitir facturar si no podemos validar
        return false;
    }

    /**
     * Fallback method para existeCliente cuando el Circuit Breaker está abierto.
     */
    private boolean existeClienteFallback(Long clienteId, Exception ex) {
        log.error("Circuit Breaker abierto o error al verificar existencia de cliente {}. " +
                 "Usando fallback (retornando false). Error: {}",
            clienteId, ex.getMessage());

        return false;
    }
}
