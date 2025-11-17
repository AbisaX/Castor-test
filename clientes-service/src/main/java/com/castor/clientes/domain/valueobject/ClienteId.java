package com.castor.clientes.domain.valueobject;

import java.util.Objects;

/**
 * Value Object: ID de Cliente
 * Identidad única del agregado Cliente
 */
public final class ClienteId {
    private final Long valor;

    private ClienteId(Long valor) {
        this.valor = valor;
    }

    public static ClienteId of(Long valor) {
        if (valor == null || valor <= 0) {
            throw new IllegalArgumentException("El ID del cliente debe ser un número positivo");
        }
        return new ClienteId(valor);
    }

    public static ClienteId generate() {
        // En producción, usar un generador de IDs distribuido (UUID, Snowflake, etc.)
        return new ClienteId(null);
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
        ClienteId clienteId = (ClienteId) o;
        return Objects.equals(valor, clienteId.valor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valor);
    }

    @Override
    public String toString() {
        return valor != null ? valor.toString() : "NUEVO";
    }
}
