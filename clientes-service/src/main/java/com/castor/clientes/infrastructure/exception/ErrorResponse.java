package com.castor.clientes.infrastructure.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO para respuestas de error estandarizadas
 * Incluye información detallada para debugging y trazabilidad
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta de error estándar del API")
public class ErrorResponse {

    @Schema(description = "Timestamp del error", example = "2024-01-15T10:30:00")
    private LocalDateTime timestamp;

    @Schema(description = "Código HTTP del error", example = "400")
    private int status;

    @Schema(description = "Tipo de error", example = "Bad Request")
    private String error;

    @Schema(description = "Mensaje descriptivo del error", example = "El NIT es obligatorio")
    private String message;

    @Schema(description = "Path del endpoint donde ocurrió el error", example = "/api/v1/clientes")
    private String path;

    @Schema(description = "ID de rastreo para correlación de logs (Zipkin/Sleuth)")
    @JsonProperty("trace_id")
    private String traceId;

    @Schema(description = "Nombre de la excepción Java")
    @JsonProperty("exception_type")
    private String exceptionType;

    @Schema(description = "Errores de validación por campo")
    @JsonProperty("validation_errors")
    private Map<String, String> validationErrors;

    @Schema(description = "Detalles adicionales del error")
    private Map<String, Object> details;

    @Schema(description = "Indica si el error es recuperable")
    @JsonProperty("is_recoverable")
    private Boolean isRecoverable;
}
