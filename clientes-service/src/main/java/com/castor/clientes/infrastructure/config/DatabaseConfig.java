package com.castor.clientes.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de base de datos
 * Usa @ConfigurationProperties para externalizar configuración
 */
@Configuration
@EnableConfigurationProperties
public class DatabaseConfig {
    // Spring Boot autoconfigura DataSource con application.yml
    // Esta clase puede extenderse para configuraciones adicionales
}
