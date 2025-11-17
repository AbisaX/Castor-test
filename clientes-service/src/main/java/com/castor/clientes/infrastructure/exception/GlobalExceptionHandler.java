package com.castor.clientes.infrastructure.exception;

import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones mejorado
 * Incluye trazabilidad, logging detallado y múltiples tipos de excepciones
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final Tracer tracer;

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.error("Error de validación en {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse error = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                ex.getMessage(),
                request.getRequestURI(),
                ex.getClass().getSimpleName(),
                true
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex, HttpServletRequest request) {
        log.error("Error de estado en {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ErrorResponse error = buildErrorResponse(
                HttpStatus.CONFLICT,
                "Conflict",
                ex.getMessage(),
                request.getRequestURI(),
                ex.getClass().getSimpleName(),
                false
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.error("Errores de validación en {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse error = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Validation Failed",
                "Error en la validación de datos",
                request.getRequestURI(),
                ex.getClass().getSimpleName(),
                true
        );
        error.setValidationErrors(errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        log.error("Violación de constraints en {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (existing, replacement) -> existing
                ));

        ErrorResponse error = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Constraint Violation",
                "Violación de restricciones de validación",
                request.getRequestURI(),
                ex.getClass().getSimpleName(),
                true
        );
        error.setValidationErrors(errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        log.error("Violación de integridad de datos en {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        String message = "Error de integridad de datos";
        if (ex.getMessage() != null) {
            if (ex.getMessage().contains("unique") || ex.getMessage().contains("duplicate")) {
                message = "Ya existe un registro con esos datos";
            } else if (ex.getMessage().contains("foreign key") || ex.getMessage().contains("constraint")) {
                message = "No se puede realizar la operación debido a relaciones existentes";
            }
        }

        ErrorResponse error = buildErrorResponse(
                HttpStatus.CONFLICT,
                "Data Integrity Violation",
                message,
                request.getRequestURI(),
                ex.getClass().getSimpleName(),
                true
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.error("Error al leer el cuerpo de la petición en {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse error = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Malformed JSON Request",
                "El formato del cuerpo de la petición es inválido",
                request.getRequestURI(),
                ex.getClass().getSimpleName(),
                true
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.error("Error de tipo de argumento en {}: {}", request.getRequestURI(), ex.getMessage());

        String message = String.format("El parámetro '%s' debe ser de tipo %s",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "desconocido");

        ErrorResponse error = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Type Mismatch",
                message,
                request.getRequestURI(),
                ex.getClass().getSimpleName(),
                true
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        log.error("Parámetro requerido faltante en {}: {}", request.getRequestURI(), ex.getMessage());

        String message = String.format("El parámetro '%s' es requerido", ex.getParameterName());

        ErrorResponse error = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Missing Parameter",
                message,
                request.getRequestURI(),
                ex.getClass().getSimpleName(),
                true
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Error inesperado en {}: ", request.getRequestURI(), ex);

        ErrorResponse error = buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "Ha ocurrido un error inesperado. Por favor contacte al administrador.",
                request.getRequestURI(),
                ex.getClass().getSimpleName(),
                false
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Builder helper para crear ErrorResponse con todos los campos
     */
    private ErrorResponse buildErrorResponse(
            HttpStatus status,
            String error,
            String message,
            String path,
            String exceptionType,
            boolean isRecoverable) {

        String traceId = getTraceId();

        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(error)
                .message(message)
                .path(path)
                .traceId(traceId)
                .exceptionType(exceptionType)
                .isRecoverable(isRecoverable)
                .build();
    }

    /**
     * Obtiene el trace ID de Micrometer/Zipkin si está disponible
     */
    private String getTraceId() {
        try {
            if (tracer != null && tracer.currentSpan() != null) {
                return tracer.currentSpan().context().traceId();
            }
        } catch (Exception e) {
            log.debug("No se pudo obtener trace ID: {}", e.getMessage());
        }
        return null;
    }
}
