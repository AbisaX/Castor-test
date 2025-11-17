package com.castor.clientes.infrastructure.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

/**
 * Propiedades de configuración de Actuator
 * Mapea las configuraciones de monitoreo y métricas
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "management")
@Validated
public class ActuatorProperties {

    private EndpointsProperties endpoints = new EndpointsProperties();
    private MetricsProperties metrics = new MetricsProperties();
    private TracingProperties tracing = new TracingProperties();
    private ZipkinProperties zipkin = new ZipkinProperties();

    @Data
    public static class EndpointsProperties {
        private WebProperties web = new WebProperties();

        @Data
        public static class WebProperties {
            private ExposureProperties exposure = new ExposureProperties();

            @NotBlank
            private String basePath = "/actuator";

            @Data
            public static class ExposureProperties {
                @NotNull
                private Set<String> include = Set.of("health", "info", "metrics", "prometheus");
            }
        }
    }

    @Data
    public static class MetricsProperties {
        private TagsProperties tags = new TagsProperties();
        private ExportProperties export = new ExportProperties();

        @Data
        public static class TagsProperties {
            private String application;
        }

        @Data
        public static class ExportProperties {
            private PrometheusExportProperties prometheus = new PrometheusExportProperties();

            @Data
            public static class PrometheusExportProperties {
                private Boolean enabled = true;
            }
        }
    }

    @Data
    public static class TracingProperties {
        private SamplingProperties sampling = new SamplingProperties();

        @Data
        public static class SamplingProperties {
            @NotNull
            private Double probability = 1.0;
        }
    }

    @Data
    public static class ZipkinProperties {
        private TracingProperties tracing = new TracingProperties();

        @Data
        public static class TracingProperties {
            @NotBlank
            private String endpoint = "http://localhost:9411/api/v2/spans";
        }
    }
}
