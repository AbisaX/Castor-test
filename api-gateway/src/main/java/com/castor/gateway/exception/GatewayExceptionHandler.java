package com.castor.gateway.exception;

import com.castor.gateway.dto.ErrorResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@Order(-2)
@RequiredArgsConstructor
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;
    private final Tracer tracer;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        String path = exchange.getRequest().getPath().value();
        String traceId = getTraceId();

        log.error("[{}] Gateway Exception - Path: {}, Error: {}", traceId, path, ex.getMessage(), ex);

        ErrorResponse errorResponse = buildErrorResponse(ex, path, traceId);

        // Set response status
        exchange.getResponse().setStatusCode(HttpStatus.valueOf(errorResponse.getStatus()));
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // Add trace header
        if (traceId != null) {
            exchange.getResponse().getHeaders().add("X-Trace-Id", traceId);
        }

        try {
            String errorJson = objectMapper.writeValueAsString(errorResponse);
            DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(errorJson.getBytes(StandardCharsets.UTF_8));

            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("[{}] Error serializing error response", traceId, e);
            return exchange.getResponse().setComplete();
        }
    }

    private ErrorResponse buildErrorResponse(Throwable ex, String path, String traceId) {
        if (ex instanceof NotFoundException) {
            return ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                "Service Not Found",
                "The requested service is not available: " + ex.getMessage(),
                path,
                traceId
            );
        } else if (ex instanceof ResponseStatusException) {
            ResponseStatusException rse = (ResponseStatusException) ex;
            return ErrorResponse.of(
                rse.getStatusCode().value(),
                rse.getStatusCode().toString(),
                rse.getReason() != null ? rse.getReason() : "An error occurred",
                path,
                traceId
            );
        } else if (ex instanceof java.net.ConnectException) {
            return ErrorResponse.of(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Service Unavailable",
                "Unable to connect to downstream service",
                path,
                traceId
            );
        } else if (ex instanceof java.util.concurrent.TimeoutException) {
            return ErrorResponse.of(
                HttpStatus.GATEWAY_TIMEOUT.value(),
                "Gateway Timeout",
                "Request to downstream service timed out",
                path,
                traceId
            );
        } else if (ex.getCause() instanceof java.net.ConnectException) {
            return ErrorResponse.of(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Service Unavailable",
                "Unable to connect to downstream service: " + ex.getCause().getMessage(),
                path,
                traceId
            );
        }

        // Default error response
        return ErrorResponse.of(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "An unexpected error occurred: " + ex.getMessage(),
            path,
            traceId
        );
    }

    private String getTraceId() {
        if (tracer != null && tracer.currentSpan() != null) {
            return tracer.currentSpan().context().traceId();
        }
        return null;
    }
}
