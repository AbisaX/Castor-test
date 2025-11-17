package com.castor.facturacion.infrastructure.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para respuestas de error estandarizadas.
 *
 * Usado por GlobalExceptionHandler para retornar errores en formato consistente.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta de error estándar de la API")
public class ErrorResponse {

    @Schema(description = "Timestamp del error", example = "2025-01-16T12:30:45")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    @Schema(description = "Código de estado HTTP", example = "400")
    private int status;

    @Schema(description = "Nombre del error HTTP", example = "Bad Request")
    private String error;

    @Schema(description = "Mensaje descriptivo del error", example = "El ID del cliente es obligatorio")
    private String message;

    @Schema(description = "Detalles adicionales del error", example = "clienteId: no debe ser nulo")
    private String details;

    @Schema(description = "Path del endpoint que generó el error", example = "/api/v1/facturas")
    private String path;

    @Schema(description = "Lista de errores de validación (para errores 400)")
    private List<ValidationError> validationErrors;

    /**
     * DTO para errores de validación individuales
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Error de validación de campo")
    public static class ValidationError {

        @Schema(description = "Nombre del campo con error", example = "clienteId")
        private String field;

        @Schema(description = "Valor rechazado", example = "null")
        private Object rejectedValue;

        @Schema(description = "Mensaje de error", example = "El ID del cliente es obligatorio")
        private String message;
    }

    /**
     * Constructor simplificado para errores básicos
     */
    public ErrorResponse(int status, String error, String message, String path) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }
}
