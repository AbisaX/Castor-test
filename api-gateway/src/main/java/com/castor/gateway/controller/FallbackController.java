package com.castor.gateway.controller;

import com.castor.gateway.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/clientes")
    public ResponseEntity<ErrorResponse> clientesFallback() {
        log.warn("Circuit breaker activated for clientes-service");
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.SERVICE_UNAVAILABLE.value(),
            "Service Unavailable",
            "Clientes service is currently unavailable. Please try again later.",
            "/fallback/clientes"
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    @GetMapping("/facturacion")
    public ResponseEntity<ErrorResponse> facturacionFallback() {
        log.warn("Circuit breaker activated for facturacion-service");
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.SERVICE_UNAVAILABLE.value(),
            "Service Unavailable",
            "Facturacion service is currently unavailable. Please try again later.",
            "/fallback/facturacion"
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    @GetMapping("/tax-calculator")
    public ResponseEntity<ErrorResponse> taxCalculatorFallback() {
        log.warn("Circuit breaker activated for tax-calculator-service");
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.SERVICE_UNAVAILABLE.value(),
            "Service Unavailable",
            "Tax Calculator service is currently unavailable. Please try again later.",
            "/fallback/tax-calculator"
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }
}
