package com.castor.facturacion.domain.valueobject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Value Object: Número de Factura
 * Identificador único de factura con formato FACT-YYYYMMDDHHMMSS
 */
public final class NumeroFactura {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String PREFIJO = "FACT-";

    private final String valor;

    private NumeroFactura(String valor) {
        this.valor = valor;
    }

    public static NumeroFactura generar() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        return new NumeroFactura(PREFIJO + timestamp);
    }

    public static NumeroFactura of(String valor) {
        validar(valor);
        return new NumeroFactura(valor);
    }

    private static void validar(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException("El número de factura no puede ser nulo o vacío");
        }

        if (!valor.startsWith(PREFIJO)) {
            throw new IllegalArgumentException("El número de factura debe comenzar con " + PREFIJO);
        }
    }

    public String getValor() {
        return valor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NumeroFactura that = (NumeroFactura) o;
        return Objects.equals(valor, that.valor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valor);
    }

    @Override
    public String toString() {
        return valor;
    }
}
