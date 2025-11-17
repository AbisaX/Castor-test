package com.castor.clientes;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Aplicación principal del microservicio de Clientes
 *
 * Bounded Context: Gestión de Clientes
 * Arquitectura: Hexagonal (Ports & Adapters) + DDD
 */
@SpringBootApplication
@EnableJpaRepositories
@EnableTransactionManagement
@ConfigurationPropertiesScan
@OpenAPIDefinition(
    info = @Info(
        title = "Clientes Service API",
        version = "1.0.0",
        description = "Microservicio de gestión de clientes con arquitectura hexagonal y DDD",
        contact = @Contact(
            name = "Castor Team",
            email = "contacto@castor.com"
        ),
        license = @License(
            name = "MIT License",
            url = "https://opensource.org/licenses/MIT"
        )
    ),
    servers = {
        @Server(url = "http://localhost:8081", description = "Desarrollo"),
        @Server(url = "http://api-gateway:8080", description = "API Gateway")
    }
)
public class ClientesServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientesServiceApplication.class, args);
    }
}
