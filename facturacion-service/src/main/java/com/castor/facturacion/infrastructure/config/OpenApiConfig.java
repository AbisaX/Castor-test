package com.castor.facturacion.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuración de OpenAPI/Swagger para documentación de la API.
 *
 * La documentación estará disponible en:
 * - Swagger UI: http://localhost:8082/swagger-ui.html
 * - OpenAPI JSON: http://localhost:8082/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:facturacion-service}")
    private String applicationName;

    @Value("${server.port:8082}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Facturación Service API")
                .version("1.0.0")
                .description("""
                    API REST para la gestión de facturas con arquitectura hexagonal y DDD.

                    Características:
                    - Creación de facturas con validación de clientes
                    - Cálculo automático de impuestos y descuentos
                    - Consulta de facturas con paginación
                    - Integración con clientes-service
                    - Persistencia en Oracle Database

                    Tecnologías:
                    - Spring Boot 3.2.0
                    - Java 17
                    - Oracle Database
                    - Resilience4j (Circuit Breaker)
                    - Micrometer + Zipkin (Trazabilidad)
                    """)
                .contact(new Contact()
                    .name("Equipo Castor")
                    .email("soporte@castor.com")
                    .url("https://www.castor.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0.html"))
            )
            .servers(List.of(
                new Server()
                    .url("http://localhost:" + serverPort)
                    .description("Servidor de desarrollo"),
                new Server()
                    .url("http://localhost:8080")
                    .description("API Gateway")
            ));
    }
}
