package com.castor.clientes.infrastructure.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de Resilience4j con event listeners
 * Proporciona Circuit Breaker, Retry y TimeLimiter para tolerancia a fallos
 */
@Slf4j
@Configuration
public class ResilienceConfig {

    /**
     * Event listener para Circuit Breaker
     * Registra todos los eventos del circuit breaker para debugging y monitoreo
     */
    @Bean
    public RegistryEventConsumer<CircuitBreaker> circuitBreakerEventConsumer() {
        return new RegistryEventConsumer<>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<CircuitBreaker> entryAddedEvent) {
                CircuitBreaker circuitBreaker = entryAddedEvent.getAddedEntry();
                log.info("Circuit Breaker '{}' agregado", circuitBreaker.getName());

                // Registrar listeners para eventos del circuit breaker
                circuitBreaker.getEventPublisher()
                        .onSuccess(event -> log.debug("Circuit Breaker '{}' - Llamada exitosa", event.getCircuitBreakerName()))
                        .onError(event -> log.warn("Circuit Breaker '{}' - Error: {}",
                                event.getCircuitBreakerName(),
                                event.getThrowable().getMessage()))
                        .onStateTransition(event -> log.warn("Circuit Breaker '{}' - Transición de estado: {} -> {}",
                                event.getCircuitBreakerName(),
                                event.getStateTransition().getFromState(),
                                event.getStateTransition().getToState()))
                        .onSlowCallRateExceeded(event -> log.warn("Circuit Breaker '{}' - Tasa de llamadas lentas excedida: {}%",
                                event.getCircuitBreakerName(),
                                event.getSlowCallRate()))
                        .onFailureRateExceeded(event -> log.error("Circuit Breaker '{}' - Tasa de fallos excedida: {}%",
                                event.getCircuitBreakerName(),
                                event.getFailureRate()));
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<CircuitBreaker> entryRemoveEvent) {
                log.info("Circuit Breaker '{}' removido", entryRemoveEvent.getRemovedEntry().getName());
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<CircuitBreaker> entryReplacedEvent) {
                log.info("Circuit Breaker '{}' reemplazado", entryReplacedEvent.getNewEntry().getName());
            }
        };
    }

    /**
     * Event listener para Retry
     * Registra todos los eventos de retry para debugging
     */
    @Bean
    public RegistryEventConsumer<Retry> retryEventConsumer() {
        return new RegistryEventConsumer<>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<Retry> entryAddedEvent) {
                Retry retry = entryAddedEvent.getAddedEntry();
                log.info("Retry '{}' agregado", retry.getName());

                // Registrar listeners para eventos de retry
                retry.getEventPublisher()
                        .onRetry(event -> log.warn("Retry '{}' - Intento #{} después de error: {}",
                                event.getName(),
                                event.getNumberOfRetryAttempts(),
                                event.getLastThrowable().getMessage()))
                        .onSuccess(event -> log.debug("Retry '{}' - Operación exitosa después de {} intentos",
                                event.getName(),
                                event.getNumberOfRetryAttempts()))
                        .onError(event -> log.error("Retry '{}' - Todos los reintentos fallaron. Error final: {}",
                                event.getName(),
                                event.getLastThrowable().getMessage()));
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<Retry> entryRemoveEvent) {
                log.info("Retry '{}' removido", entryRemoveEvent.getRemovedEntry().getName());
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<Retry> entryReplacedEvent) {
                log.info("Retry '{}' reemplazado", entryReplacedEvent.getNewEntry().getName());
            }
        };
    }

    /**
     * Configura el CircuitBreakerRegistry con event consumer
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(
            RegistryEventConsumer<CircuitBreaker> circuitBreakerEventConsumer) {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        registry.getEventPublisher().onEntryAdded(circuitBreakerEventConsumer);
        log.info("CircuitBreakerRegistry configurado con event listeners");
        return registry;
    }

    /**
     * Configura el RetryRegistry con event consumer
     */
    @Bean
    public RetryRegistry retryRegistry(RegistryEventConsumer<Retry> retryEventConsumer) {
        RetryRegistry registry = RetryRegistry.ofDefaults();
        registry.getEventPublisher().onEntryAdded(retryEventConsumer);
        log.info("RetryRegistry configurado con event listeners");
        return registry;
    }
}
