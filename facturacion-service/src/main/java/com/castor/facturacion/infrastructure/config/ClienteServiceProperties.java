package com.castor.facturacion.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Configuración externalizada para el servicio de clientes.
 *
 * Usa @ConfigurationProperties para mapear propiedades desde application.yml
 */
@ConfigurationProperties(prefix = "cliente-service")
@Validated
public class ClienteServiceProperties {

    /**
     * URL base del microservicio de clientes
     */
    @NotBlank(message = "La URL del servicio de clientes es obligatoria")
    private String baseUrl = "http://clientes-service:8081";

    /**
     * Timeout para las peticiones HTTP
     */
    @NotNull
    private Duration timeout = Duration.ofSeconds(5);

    /**
     * Timeout para la conexión
     */
    @NotNull
    private Duration connectionTimeout = Duration.ofSeconds(3);

    /**
     * Número máximo de reintentos
     */
    private int maxRetries = 3;

    /**
     * Habilitar cache de validaciones
     */
    private boolean enableCache = true;

    /**
     * Duración del cache en minutos
     */
    @NotNull
    private Duration cacheDuration = Duration.ofMinutes(5);

    // Constructor por defecto
    public ClienteServiceProperties() {
    }

    // Getters y Setters

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public Duration getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(Duration connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public boolean isEnableCache() {
        return enableCache;
    }

    public void setEnableCache(boolean enableCache) {
        this.enableCache = enableCache;
    }

    public Duration getCacheDuration() {
        return cacheDuration;
    }

    public void setCacheDuration(Duration cacheDuration) {
        this.cacheDuration = cacheDuration;
    }

    @Override
    public String toString() {
        return "ClienteServiceProperties{" +
               "baseUrl='" + baseUrl + '\'' +
               ", timeout=" + timeout +
               ", connectionTimeout=" + connectionTimeout +
               ", maxRetries=" + maxRetries +
               ", enableCache=" + enableCache +
               ", cacheDuration=" + cacheDuration +
               '}';
    }
}
