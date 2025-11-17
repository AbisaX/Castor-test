package com.castor.clientes.infrastructure.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Propiedades de configuración de la base de datos
 * Mapea las configuraciones desde application.yml con validaciones
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "spring.datasource")
@Validated
public class DatabaseProperties {

    @NotBlank(message = "La URL de la base de datos es obligatoria")
    private String url;

    @NotBlank(message = "El username de la base de datos es obligatorio")
    private String username;

    @NotBlank(message = "El password de la base de datos es obligatorio")
    private String password;

    @NotBlank(message = "El driver class name es obligatorio")
    private String driverClassName;

    /**
     * Configuraciones del pool de conexiones HikariCP
     */
    private HikariProperties hikari = new HikariProperties();

    @Data
    public static class HikariProperties {

        @NotNull
        @Min(value = 1, message = "El pool debe tener al menos 1 conexión")
        private Integer maximumPoolSize = 10;

        @NotNull
        @Min(value = 1, message = "Debe haber al menos 1 conexión idle")
        private Integer minimumIdle = 2;

        @NotNull
        @Min(value = 1000, message = "El timeout debe ser al menos 1000ms")
        private Long connectionTimeout = 30000L;

        @NotNull
        @Min(value = 10000, message = "El idle timeout debe ser al menos 10000ms")
        private Long idleTimeout = 600000L;

        @NotNull
        @Min(value = 30000, message = "El max lifetime debe ser al menos 30000ms")
        private Long maxLifetime = 1800000L;

        private String poolName = "ClientesHikariPool";

        private Boolean autoCommit = true;

        private Integer leakDetectionThreshold = 0;
    }
}
