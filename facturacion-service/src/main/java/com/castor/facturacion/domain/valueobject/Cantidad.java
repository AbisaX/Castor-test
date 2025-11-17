package com.castor.facturacion.domain.valueobject;

import java.util.Objects;

/**
 * Value Object: Cantidad
 * Representa una cantidad entera positiva
 */
public final class Cantidad {
    private final Integer valor;

    private Cantidad(Integer valor) {
        this.valor = valor;
    }

    public static Cantidad of(Integer valor) {
        validar(valor);
        return new Cantidad(valor);
    }

    private static void validar(Integer valor) {
        if (valor == null || valor <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser un número entero positivo");
        }
    }

    public Integer getValor() {
        return valor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cantidad cantidad = (Cantidad) o;
        return Objects.equals(valor, cantidad.valor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valor);
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
