package com.castor.facturacion.domain.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Value Object: Dinero
 * Representa una cantidad monetaria con moneda
 */
public final class Dinero {
    private static final Currency COP = Currency.getInstance("COP");
    private static final int ESCALA = 2;

    private final BigDecimal cantidad;
    private final Currency moneda;

    private Dinero(BigDecimal cantidad, Currency moneda) {
        this.cantidad = cantidad.setScale(ESCALA, RoundingMode.HALF_UP);
        this.moneda = moneda;
    }

    public static Dinero cero() {
        return new Dinero(BigDecimal.ZERO, COP);
    }

    public static Dinero of(BigDecimal cantidad) {
        return of(cantidad, COP);
    }

    public static Dinero of(BigDecimal cantidad, Currency moneda) {
        validar(cantidad);
        return new Dinero(cantidad, moneda);
    }

    private static void validar(BigDecimal cantidad) {
        if (cantidad == null) {
            throw new IllegalArgumentException("La cantidad no puede ser nula");
        }
        if (cantidad.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("La cantidad no puede ser negativa");
        }
    }

    public Dinero sumar(Dinero otro) {
        validarMismaMoneda(otro);
        return new Dinero(this.cantidad.add(otro.cantidad), this.moneda);
    }

    public Dinero restar(Dinero otro) {
        validarMismaMoneda(otro);
        BigDecimal resultado = this.cantidad.subtract(otro.cantidad);
        if (resultado.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El resultado de la resta no puede ser negativo");
        }
        return new Dinero(resultado, this.moneda);
    }

    public Dinero multiplicar(BigDecimal factor) {
        if (factor.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El factor no puede ser negativo");
        }
        return new Dinero(this.cantidad.multiply(factor), this.moneda);
    }

    public Dinero aplicarPorcentaje(Porcentaje porcentaje) {
        return new Dinero(
            this.cantidad.multiply(porcentaje.getValor()).divide(
                new BigDecimal("100"), ESCALA, RoundingMode.HALF_UP
            ),
            this.moneda
        );
    }

    private void validarMismaMoneda(Dinero otro) {
        if (!this.moneda.equals(otro.moneda)) {
            throw new IllegalArgumentException("No se pueden operar cantidades con diferentes monedas");
        }
    }

    public boolean esMayorQue(Dinero otro) {
        validarMismaMoneda(otro);
        return this.cantidad.compareTo(otro.cantidad) > 0;
    }

    public boolean esMenorQue(Dinero otro) {
        validarMismaMoneda(otro);
        return this.cantidad.compareTo(otro.cantidad) < 0;
    }

    public boolean esCero() {
        return this.cantidad.compareTo(BigDecimal.ZERO) == 0;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public Currency getMoneda() {
        return moneda;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dinero dinero = (Dinero) o;
        return cantidad.compareTo(dinero.cantidad) == 0 && Objects.equals(moneda, dinero.moneda);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cantidad, moneda);
    }

    @Override
    public String toString() {
        return moneda.getSymbol() + " " + cantidad;
    }
}
