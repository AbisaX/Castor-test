package com.castor.facturacion.domain.valueobject;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value Object: Porcentaje
 * Representa un porcentaje entre 0 y 100
 */
public final class Porcentaje {
    private static final BigDecimal MIN_VALOR = BigDecimal.ZERO;
    private static final BigDecimal MAX_VALOR = new BigDecimal("100");

    private final BigDecimal valor;

    private Porcentaje(BigDecimal valor) {
        this.valor = valor;
    }

    public static Porcentaje cero() {
        return new Porcentaje(BigDecimal.ZERO);
    }

    public static Porcentaje of(BigDecimal valor) {
        validar(valor);
        return new Porcentaje(valor);
    }

    private static void validar(BigDecimal valor) {
        if (valor == null) {
            throw new IllegalArgumentException("El porcentaje no puede ser nulo");
        }
        if (valor.compareTo(MIN_VALOR) < 0 || valor.compareTo(MAX_VALOR) > 0) {
            throw new IllegalArgumentException("El porcentaje debe estar entre 0 y 100");
        }
    }

    public BigDecimal getValor() {
        return valor;
    }

    public boolean esCero() {
        return valor.compareTo(BigDecimal.ZERO) == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Porcentaje that = (Porcentaje) o;
        return valor.compareTo(that.valor) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(valor);
    }

    @Override
    public String toString() {
        return valor + "%";
    }
}
