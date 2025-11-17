package com.castor.facturacion.infrastructure.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuración de Resilience4j (Circuit Breaker, Retry, etc.).
 *
 * Define estrategias de resiliencia personalizadas para llamadas a servicios externos.
 */
@Configuration
public class ResilienceConfig {

    private static final Logger log = LoggerFactory.getLogger(ResilienceConfig.class);

    /**
     * Configuración personalizada de Circuit Breaker para clienteService
     */
    @Bean
    public CircuitBreaker clienteServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        log.info("Configurando Circuit Breaker personalizado para clienteService");

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            // Umbral de fallos para abrir el circuito (50%)
            .failureRateThreshold(50)
            // Número mínimo de llamadas antes de calcular tasa de fallos
            .minimumNumberOfCalls(5)
            // Tiempo que el circuito permanece abierto antes de pasar a half-open
            .waitDurationInOpenState(Duration.ofSeconds(30))
            // Número de llamadas permitidas en estado half-open
            .permittedNumberOfCallsInHalfOpenState(3)
            // Tamaño de la ventana deslizante para contar llamadas
            .slidingWindowSize(10)
            // Tipo de ventana: COUNT_BASED o TIME_BASED
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            // Excepciones que NO abren el circuito
            .ignoreExceptions(IllegalArgumentException.class)
            .build();

        CircuitBreaker circuitBreaker = registry.circuitBreaker("clienteService", config);

        // Event listeners para logging
        circuitBreaker.getEventPublisher()
            .onStateTransition(event ->
                log.warn("Circuit Breaker clienteService cambió de estado: {} -> {}",
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState()))
            .onError(event ->
                log.error("Circuit Breaker clienteService error: {}",
                    event.getThrowable().getMessage()))
            .onSuccess(event ->
                log.debug("Circuit Breaker clienteService llamada exitosa"));

        return circuitBreaker;
    }

    /**
     * Configuración personalizada de Circuit Breaker para taxCalculator
     */
    @Bean
    public CircuitBreaker taxCalculatorCircuitBreaker(CircuitBreakerRegistry registry) {
        log.info("Configurando Circuit Breaker personalizado para taxCalculator");

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(60)  // Más tolerante que clienteService
            .minimumNumberOfCalls(5)
            .waitDurationInOpenState(Duration.ofSeconds(20))
            .permittedNumberOfCallsInHalfOpenState(3)
            .slidingWindowSize(10)
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .build();

        CircuitBreaker circuitBreaker = registry.circuitBreaker("taxCalculator", config);

        // Event listeners
        circuitBreaker.getEventPublisher()
            .onStateTransition(event ->
                log.warn("Circuit Breaker taxCalculator cambió de estado: {} -> {}",
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState()))
            .onError(event ->
                log.error("Circuit Breaker taxCalculator error: {}",
                    event.getThrowable().getMessage()));

        return circuitBreaker;
    }

    /**
     * Configuración personalizada de Retry para clienteService
     */
    @Bean
    public Retry clienteServiceRetry(RetryRegistry registry) {
        log.info("Configurando Retry personalizado para clienteService");

        RetryConfig config = RetryConfig.custom()
            // Número máximo de intentos (incluyendo el inicial)
            .maxAttempts(3)
            // Tiempo de espera entre intentos
            .waitDuration(Duration.ofMillis(500))
            // Excepciones que disparan retry
            .retryExceptions(Exception.class)
            // Excepciones que NO disparan retry
            .ignoreExceptions(IllegalArgumentException.class)
            .build();

        Retry retry = registry.retry("clienteService", config);

        // Event listeners
        retry.getEventPublisher()
            .onRetry(event ->
                log.warn("Retry clienteService intento {}: {}",
                    event.getNumberOfRetryAttempts(),
                    event.getLastThrowable().getMessage()));

        return retry;
    }

    /**
     * Configuración personalizada de Retry para taxCalculator
     */
    @Bean
    public Retry taxCalculatorRetry(RetryRegistry registry) {
        log.info("Configurando Retry personalizado para taxCalculator");

        RetryConfig config = RetryConfig.custom()
            .maxAttempts(2)  // Menos intentos que clienteService
            .waitDuration(Duration.ofMillis(300))
            .retryExceptions(Exception.class)
            .build();

        Retry retry = registry.retry("taxCalculator", config);

        retry.getEventPublisher()
            .onRetry(event ->
                log.warn("Retry taxCalculator intento {}: {}",
                    event.getNumberOfRetryAttempts(),
                    event.getLastThrowable().getMessage()));

        return retry;
    }
}
