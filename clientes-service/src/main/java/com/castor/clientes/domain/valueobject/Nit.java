package com.castor.clientes.domain.valueobject;

import java.util.Objects;

/**
 * Value Object: NIT (Número de Identificación Tributaria)
 * Representa el identificador único tributario de un cliente
 */
public final class Nit {
    private final String valor;

    private Nit(String valor) {
        this.valor = valor;
    }

    public static Nit of(String valor) {
        validar(valor);
        return new Nit(valor.trim());
    }

    private static void validar(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException("El NIT no puede ser nulo o vacío");
        }

        String nitLimpio = valor.trim();
        if (nitLimpio.length() < 9 || nitLimpio.length() > 15) {
            throw new IllegalArgumentException("El NIT debe tener entre 9 y 15 caracteres");
        }

        // Validar formato: solo números, guiones y espacios
        if (!nitLimpio.matches("^[0-9\\-\\s]+$")) {
            throw new IllegalArgumentException("El NIT solo puede contener números, guiones y espacios");
        }
    }

    public String getValor() {
        return valor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Nit nit = (Nit) o;
        return Objects.equals(valor, nit.valor);
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
