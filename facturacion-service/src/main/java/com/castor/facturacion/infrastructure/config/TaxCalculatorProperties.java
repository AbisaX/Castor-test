package com.castor.facturacion.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Configuración externalizada para el servicio de cálculo de impuestos.
 *
 * Usa @ConfigurationProperties para mapear propiedades desde application.yml
 * siguiendo las mejores prácticas de Spring Boot.
 */
@ConfigurationProperties(prefix = "tax-calculator")
@Validated
public class TaxCalculatorProperties {

    /**
     * URL base del microservicio de cálculo de impuestos
     */
    @NotBlank(message = "La URL del servicio de cálculo de impuestos es obligatoria")
    private String baseUrl = "http://tax-calculator-service:5000";

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
     * Delay entre reintentos
     */
    @NotNull
    private Duration retryDelay = Duration.ofMillis(500);

    /**
     * Habilitar logs detallados
     */
    private boolean enableLogging = true;

    /**
     * Tasa de impuesto por defecto (IVA)
     */
    @NotNull
    private java.math.BigDecimal defaultTaxRate = new java.math.BigDecimal("19.00");

    /**
     * Tasa de descuento por defecto
     */
    @NotNull
    private java.math.BigDecimal defaultDiscountRate = new java.math.BigDecimal("10.00");

    // Constructor por defecto
    public TaxCalculatorProperties() {
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

    public Duration getRetryDelay() {
        return retryDelay;
    }

    public void setRetryDelay(Duration retryDelay) {
        this.retryDelay = retryDelay;
    }

    public boolean isEnableLogging() {
        return enableLogging;
    }

    public void setEnableLogging(boolean enableLogging) {
        this.enableLogging = enableLogging;
    }

    public java.math.BigDecimal getDefaultTaxRate() {
        return defaultTaxRate;
    }

    public void setDefaultTaxRate(java.math.BigDecimal defaultTaxRate) {
        this.defaultTaxRate = defaultTaxRate;
    }

    public java.math.BigDecimal getDefaultDiscountRate() {
        return defaultDiscountRate;
    }

    public void setDefaultDiscountRate(java.math.BigDecimal defaultDiscountRate) {
        this.defaultDiscountRate = defaultDiscountRate;
    }

    @Override
    public String toString() {
        return "TaxCalculatorProperties{" +
               "baseUrl='" + baseUrl + '\'' +
               ", timeout=" + timeout +
               ", connectionTimeout=" + connectionTimeout +
               ", maxRetries=" + maxRetries +
               ", retryDelay=" + retryDelay +
               ", enableLogging=" + enableLogging +
               ", defaultTaxRate=" + defaultTaxRate +
               ", defaultDiscountRate=" + defaultDiscountRate +
               '}';
    }
}
