package com.castor.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
@SpringBootApplication
public class ApiGatewayApplication {

    private final Environment environment;

    public ApiGatewayApplication(Environment environment) {
        this.environment = environment;
    }

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        printBanner();
    }

    private void printBanner() {
        String port = environment.getProperty("server.port", "8080");
        String contextPath = environment.getProperty("server.servlet.context-path", "");
        String hostAddress = "localhost";

        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("Unable to determine host address", e);
        }

        String banner = String.format("""

            ╔═══════════════════════════════════════════════════════════════╗
            ║                                                               ║
            ║   ██████╗ █████╗ ███████╗████████╗ ██████╗ ██████╗          ║
            ║  ██╔════╝██╔══██╗██╔════╝╚══██╔══╝██╔═══██╗██╔══██╗         ║
            ║  ██║     ███████║███████╗   ██║   ██║   ██║██████╔╝         ║
            ║  ██║     ██╔══██║╚════██║   ██║   ██║   ██║██╔══██╗         ║
            ║  ╚██████╗██║  ██║███████║   ██║   ╚██████╔╝██║  ██║         ║
            ║   ╚═════╝╚═╝  ╚═╝╚══════╝   ╚═╝    ╚═════╝ ╚═╝  ╚═╝         ║
            ║                                                               ║
            ║              API GATEWAY - Microservices Platform            ║
            ║                         v1.0.0                                ║
            ║                                                               ║
            ╠═══════════════════════════════════════════════════════════════╣
            ║                                                               ║
            ║  Application:  Castor API Gateway                            ║
            ║  Port:         %s                                           ║
            ║  Profile:      %s                                    ║
            ║                                                               ║
            ║  Local:        http://localhost:%s%s                    ║
            ║  External:     http://%s:%s%s                ║
            ║  Actuator:     http://localhost:%s/actuator                ║
            ║  Health:       http://localhost:%s/actuator/health         ║
            ║  Metrics:      http://localhost:%s/actuator/prometheus     ║
            ║                                                               ║
            ║  Features:                                                    ║
            ║    - Spring Cloud Gateway                                    ║
            ║    - Rate Limiting (Resilience4j)                            ║
            ║    - Circuit Breaker                                         ║
            ║    - Distributed Tracing (Zipkin)                            ║
            ║    - Prometheus Metrics                                      ║
            ║    - Health Checks                                           ║
            ║    - CORS Support                                            ║
            ║                                                               ║
            ╚═══════════════════════════════════════════════════════════════╝

            """,
            port,
            String.join(", ", environment.getActiveProfiles().length > 0 ?
                environment.getActiveProfiles() : new String[]{"default"}),
            port, contextPath,
            hostAddress, port, contextPath,
            port, port, port
        );

        log.info(banner);
    }
}
