package com.castor.clientes.domain.valueobject;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object: Email
 * Representa una dirección de correo electrónico válida
 */
public final class Email {
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final String valor;

    private Email(String valor) {
        this.valor = valor;
    }

    public static Email of(String valor) {
        validar(valor);
        return new Email(valor.trim().toLowerCase());
    }

    private static void validar(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException("El email no puede ser nulo o vacío");
        }

        if (!EMAIL_PATTERN.matcher(valor.trim()).matches()) {
            throw new IllegalArgumentException("El email no tiene un formato válido: " + valor);
        }
    }

    public String getValor() {
        return valor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Email email = (Email) o;
        return Objects.equals(valor, email.valor);
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
