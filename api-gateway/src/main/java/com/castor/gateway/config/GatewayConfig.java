package com.castor.gateway.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class GatewayConfig {

    private final GatewayProperties gatewayProperties;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        log.info("Configuring Gateway Routes...");

        return builder.routes()
                // Clientes Service Routes
                .route("clientes-service", r -> r
                        .path("/api/v1/clientes/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .addRequestHeader("X-Gateway-Request", "API-Gateway")
                                .addResponseHeader("X-Gateway-Response", "API-Gateway")
                                .circuitBreaker(config -> config
                                        .setName("clientesCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/clientes"))
                                .retry(retryConfig -> retryConfig
                                        .setRetries(3)
                                        .setStatuses(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE)
                                        .setBackoff(Duration.ofMillis(100), Duration.ofMillis(1000), 2, false)))
                        .uri(gatewayProperties.getServices().getClientes().getUrl())
                        .metadata("rateLimit", gatewayProperties.getRateLimiting().getDefaultLimit())
                        .metadata("service", "clientes-service"))

                // Facturacion Service Routes
                .route("facturacion-service", r -> r
                        .path("/api/v1/facturas/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .addRequestHeader("X-Gateway-Request", "API-Gateway")
                                .addResponseHeader("X-Gateway-Response", "API-Gateway")
                                .circuitBreaker(config -> config
                                        .setName("facturacionCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/facturacion"))
                                .retry(retryConfig -> retryConfig
                                        .setRetries(3)
                                        .setStatuses(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE)
                                        .setBackoff(Duration.ofMillis(100), Duration.ofMillis(1000), 2, false)))
                        .uri(gatewayProperties.getServices().getFacturacion().getUrl())
                        .metadata("rateLimit", 50) // Lower limit for facturacion
                        .metadata("service", "facturacion-service"))

                // Tax Calculator Service Routes
                .route("tax-calculator-service", r -> r
                        .path("/api/v1/tax-calculator/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .addRequestHeader("X-Gateway-Request", "API-Gateway")
                                .addResponseHeader("X-Gateway-Response", "API-Gateway")
                                .circuitBreaker(config -> config
                                        .setName("taxCalculatorCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/tax-calculator"))
                                .retry(retryConfig -> retryConfig
                                        .setRetries(3)
                                        .setStatuses(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE)
                                        .setBackoff(Duration.ofMillis(100), Duration.ofMillis(1000), 2, false)))
                        .uri(gatewayProperties.getServices().getTaxCalculator().getUrl())
                        .metadata("rateLimit", gatewayProperties.getRateLimiting().getDefaultLimit())
                        .metadata("service", "tax-calculator-service"))

                // Actuator endpoint for gateway
                .route("actuator-route", r -> r
                        .path("/actuator/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://localhost:8080"))

                .build();
    }
}
