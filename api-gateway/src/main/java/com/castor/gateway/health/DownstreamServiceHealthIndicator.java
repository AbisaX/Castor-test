package com.castor.gateway.health;

import com.castor.gateway.config.GatewayProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DownstreamServiceHealthIndicator implements HealthIndicator {

    private final GatewayProperties gatewayProperties;
    private final WebClient.Builder webClientBuilder;

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        boolean allServicesUp = true;

        // Check Clientes Service
        if (gatewayProperties.getServices().getClientes().isHealthCheckEnabled()) {
            ServiceHealthStatus clientesStatus = checkServiceHealth(
                gatewayProperties.getServices().getClientes().getUrl(),
                "clientes-service"
            );
            details.put("clientes-service", clientesStatus.toMap());
            allServicesUp = allServicesUp && clientesStatus.isUp();
        }

        // Check Facturacion Service
        if (gatewayProperties.getServices().getFacturacion().isHealthCheckEnabled()) {
            ServiceHealthStatus facturacionStatus = checkServiceHealth(
                gatewayProperties.getServices().getFacturacion().getUrl(),
                "facturacion-service"
            );
            details.put("facturacion-service", facturacionStatus.toMap());
            allServicesUp = allServicesUp && facturacionStatus.isUp();
        }

        // Check Tax Calculator Service
        if (gatewayProperties.getServices().getTaxCalculator().isHealthCheckEnabled()) {
            ServiceHealthStatus taxCalculatorStatus = checkServiceHealth(
                gatewayProperties.getServices().getTaxCalculator().getUrl(),
                "tax-calculator-service"
            );
            details.put("tax-calculator-service", taxCalculatorStatus.toMap());
            allServicesUp = allServicesUp && taxCalculatorStatus.isUp();
        }

        if (allServicesUp) {
            return Health.up()
                .withDetail("message", "All downstream services are healthy")
                .withDetails(details)
                .build();
        } else {
            return Health.down()
                .withDetail("message", "One or more downstream services are unhealthy")
                .withDetails(details)
                .build();
        }
    }

    private ServiceHealthStatus checkServiceHealth(String baseUrl, String serviceName) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            log.debug("Service URL not configured for: {}", serviceName);
            return new ServiceHealthStatus(false, "Not configured", 0);
        }

        String healthUrl = baseUrl + "/actuator/health";
        long startTime = System.currentTimeMillis();

        try {
            WebClient webClient = webClientBuilder.build();

            String response = webClient.get()
                .uri(healthUrl)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(3))
                .onErrorResume(e -> {
                    log.debug("Health check failed for {}: {}", serviceName, e.getMessage());
                    return Mono.just("DOWN");
                })
                .block();

            long responseTime = System.currentTimeMillis() - startTime;

            boolean isUp = response != null && (response.contains("UP") || response.contains("\"status\":\"UP\""));
            return new ServiceHealthStatus(isUp, isUp ? "UP" : "DOWN", responseTime);

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            log.debug("Health check exception for {}: {}", serviceName, e.getMessage());
            return new ServiceHealthStatus(false, "DOWN - " + e.getMessage(), responseTime);
        }
    }

    private static class ServiceHealthStatus {
        private final boolean up;
        private final String status;
        private final long responseTime;

        public ServiceHealthStatus(boolean up, String status, long responseTime) {
            this.up = up;
            this.status = status;
            this.responseTime = responseTime;
        }

        public boolean isUp() {
            return up;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("status", status);
            map.put("responseTime", responseTime + "ms");
            return map;
        }
    }
}
