package com.castor.facturacion;

import com.castor.facturacion.infrastructure.config.ClienteServiceProperties;
import com.castor.facturacion.infrastructure.config.TaxCalculatorProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Aplicación principal del microservicio de facturación.
 *
 * Arquitectura Hexagonal (Ports & Adapters):
 * - domain/: Lógica de negocio pura (sin dependencias de frameworks)
 * - application/: Casos de uso y servicios de aplicación
 * - infrastructure/: Adaptadores (REST, JPA, WebClient, etc.)
 *
 * Características:
 * - Domain-Driven Design (DDD)
 * - Arquitectura Hexagonal
 * - Circuit Breaker con Resilience4j
 * - Cache con Caffeine
 * - Trazabilidad con Micrometer + Zipkin
 * - Documentación con OpenAPI/Swagger
 */
@SpringBootApplication
@EnableConfigurationProperties({
    ClienteServiceProperties.class,
    TaxCalculatorProperties.class
})
public class FacturacionServiceApplication {

    private static final Logger log = LoggerFactory.getLogger(FacturacionServiceApplication.class);

    public static void main(String[] args) {
        log.info("========================================");
        log.info("Iniciando Facturacion Service");
        log.info("========================================");

        SpringApplication.run(FacturacionServiceApplication.class, args);

        log.info("========================================");
        log.info("Facturacion Service iniciado exitosamente");
        log.info("Swagger UI: http://localhost:8082/swagger-ui.html");
        log.info("API Docs: http://localhost:8082/v3/api-docs");
        log.info("Actuator: http://localhost:8082/actuator");
        log.info("Health: http://localhost:8082/actuator/health");
        log.info("========================================");
    }
}
