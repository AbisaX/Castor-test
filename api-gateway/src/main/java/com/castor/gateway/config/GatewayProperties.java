package com.castor.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {

    private RateLimiting rateLimiting = new RateLimiting();
    private Timeouts timeouts = new Timeouts();
    private CircuitBreaker circuitBreaker = new CircuitBreaker();
    private Services services = new Services();

    @Data
    public static class RateLimiting {
        private boolean enabled = true;
        private int defaultLimit = 100;
        private int defaultRefreshPeriod = 60; // seconds
        private Map<String, RouteRateLimit> routes = new HashMap<>();
    }

    @Data
    public static class RouteRateLimit {
        private int limit;
        private int refreshPeriod; // seconds
    }

    @Data
    public static class Timeouts {
        private int connect = 3000; // milliseconds
        private int response = 10000; // milliseconds
    }

    @Data
    public static class CircuitBreaker {
        private boolean enabled = true;
        private int failureRateThreshold = 50; // percentage
        private int waitDurationInOpenState = 60000; // milliseconds
        private int slidingWindowSize = 10;
        private int minimumNumberOfCalls = 5;
    }

    @Data
    public static class Services {
        private ServiceConfig clientes = new ServiceConfig();
        private ServiceConfig facturacion = new ServiceConfig();
        private ServiceConfig taxCalculator = new ServiceConfig();
    }

    @Data
    public static class ServiceConfig {
        private String url;
        private boolean healthCheckEnabled = true;
    }
}
