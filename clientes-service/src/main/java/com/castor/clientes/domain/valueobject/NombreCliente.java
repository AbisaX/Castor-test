package com.castor.clientes.domain.valueobject;

import java.util.Objects;

/**
 * Value Object: Nombre de Cliente
 * Representa el nombre o razón social de un cliente corporativo
 */
public final class NombreCliente {
    private static final int LONGITUD_MINIMA = 3;
    private static final int LONGITUD_MAXIMA = 200;

    private final String valor;

    private NombreCliente(String valor) {
        this.valor = valor;
    }

    public static NombreCliente of(String valor) {
        validar(valor);
        return new NombreCliente(valor.trim());
    }

    private static void validar(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del cliente no puede ser nulo o vacío");
        }

        String nombreLimpio = valor.trim();
        if (nombreLimpio.length() < LONGITUD_MINIMA) {
            throw new IllegalArgumentException(
                String.format("El nombre debe tener al menos %d caracteres", LONGITUD_MINIMA)
            );
        }

        if (nombreLimpio.length() > LONGITUD_MAXIMA) {
            throw new IllegalArgumentException(
                String.format("El nombre no puede exceder %d caracteres", LONGITUD_MAXIMA)
            );
        }
    }

    public String getValor() {
        return valor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NombreCliente that = (NombreCliente) o;
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
