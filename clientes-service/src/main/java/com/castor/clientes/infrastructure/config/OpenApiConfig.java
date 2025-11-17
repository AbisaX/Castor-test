package com.castor.clientes.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuración completa de OpenAPI/Swagger
 * Incluye múltiples servidores, seguridad y metadata detallada
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8081}")
    private int serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Servidor local de desarrollo"),
                        new Server()
                                .url("http://localhost:8080")
                                .description("API Gateway (desarrollo)"),
                        new Server()
                                .url("https://api.castor.com")
                                .description("Servidor de producción")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT token para autenticación. " +
                                                "Obtener token del servicio de autenticación.")))
                .info(new Info()
                        .title("Clientes Service API")
                        .version("1.0.0")
                        .description("""
                                ## Microservicio de Gestión de Clientes

                                Este microservicio implementa la gestión completa de clientes usando:
                                - **Domain-Driven Design (DDD)**
                                - **Arquitectura Hexagonal (Puertos y Adaptadores)**
                                - **Resilience4j** para tolerancia a fallos
                                - **Observabilidad** con Micrometer y Zipkin

                                ### Características principales:
                                - CRUD completo de clientes
                                - Paginación defensiva (máximo 100 elementos por página)
                                - Validaciones exhaustivas
                                - Circuit Breaker y Retry patterns
                                - Health checks personalizados
                                - Métricas con Prometheus

                                ### Reglas de negocio:
                                - El NIT debe ser único
                                - Nombre entre 3 y 200 caracteres
                                - Email válido obligatorio
                                """)
                        .contact(new Contact()
                                .name("Equipo Castor")
                                .email("contacto@castor.com")
                                .url("https://castor.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}
