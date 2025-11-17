package com.castor.facturacion.domain.valueobject;

import java.util.Objects;

/**
 * Value Object: ID de Factura
 * Identidad única del agregado Factura
 */
public final class FacturaId {
    private final Long valor;

    private FacturaId(Long valor) {
        this.valor = valor;
    }

    public static FacturaId of(Long valor) {
        if (valor == null || valor <= 0) {
            throw new IllegalArgumentException("El ID de la factura debe ser un número positivo");
        }
        return new FacturaId(valor);
    }

    public static FacturaId generate() {
        return new FacturaId(null);
    }

    public Long getValor() {
        return valor;
    }

    public boolean esNuevo() {
        return valor == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FacturaId that = (FacturaId) o;
        return Objects.equals(valor, that.valor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valor);
    }

    @Override
    public String toString() {
        return valor != null ? valor.toString() : "NUEVA";
    }
}
