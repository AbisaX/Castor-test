package com.castor.facturacion.domain;

import com.castor.facturacion.domain.valueobject.Cantidad;
import com.castor.facturacion.domain.valueobject.Dinero;
import com.castor.facturacion.domain.valueobject.Porcentaje;

import java.util.Objects;

/**
 * Entity: Item de Factura
 * Parte del agregado Factura
 *
 * Representa un ítem individual dentro de una factura con sus cálculos de impuestos y descuentos.
 */
public class ItemFactura {
    private String descripcion;
    private Cantidad cantidad;
    private Dinero precioUnitario;
    private Porcentaje porcentajeImpuesto;
    private Porcentaje porcentajeDescuento;
    private Dinero subtotal;
    private Dinero impuesto;
    private Dinero descuento;
    private Dinero total;

    // Constructor privado
    private ItemFactura() {
    }

    private ItemFactura(String descripcion, Cantidad cantidad, Dinero precioUnitario,
                       Porcentaje porcentajeImpuesto, Porcentaje porcentajeDescuento) {
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.porcentajeImpuesto = porcentajeImpuesto;
        this.porcentajeDescuento = porcentajeDescuento;
        calcularTotales();
    }

    /**
     * Factory Method: Crear item de factura
     */
    public static ItemFactura crear(String descripcion, Cantidad cantidad, Dinero precioUnitario,
                                   Porcentaje porcentajeImpuesto, Porcentaje porcentajeDescuento) {
        validarDatosCreacion(descripcion, cantidad, precioUnitario);
        return new ItemFactura(descripcion, cantidad, precioUnitario,
                              porcentajeImpuesto != null ? porcentajeImpuesto : Porcentaje.cero(),
                              porcentajeDescuento != null ? porcentajeDescuento : Porcentaje.cero());
    }

    /**
     * Factory Method: Reconstruir desde persistencia
     */
    public static ItemFactura reconstituir(String descripcion, Cantidad cantidad, Dinero precioUnitario,
                                          Porcentaje porcentajeImpuesto, Porcentaje porcentajeDescuento,
                                          Dinero subtotal, Dinero impuesto, Dinero descuento, Dinero total) {
        ItemFactura item = new ItemFactura();
        item.descripcion = descripcion;
        item.cantidad = cantidad;
        item.precioUnitario = precioUnitario;
        item.porcentajeImpuesto = porcentajeImpuesto;
        item.porcentajeDescuento = porcentajeDescuento;
        item.subtotal = subtotal;
        item.impuesto = impuesto;
        item.descuento = descuento;
        item.total = total;
        return item;
    }

    private static void validarDatosCreacion(String descripcion, Cantidad cantidad, Dinero precioUnitario) {
        if (descripcion == null || descripcion.trim().isEmpty()) {
            throw new IllegalArgumentException("La descripción del ítem es obligatoria");
        }
        if (descripcion.trim().length() > 500) {
            throw new IllegalArgumentException("La descripción no puede exceder 500 caracteres");
        }
        if (cantidad == null) {
            throw new IllegalArgumentException("La cantidad es obligatoria");
        }
        if (precioUnitario == null) {
            throw new IllegalArgumentException("El precio unitario es obligatorio");
        }
    }

    /**
     * Comportamiento de dominio: Calcular totales del item
     */
    private void calcularTotales() {
        // Subtotal = cantidad * precio_unitario
        this.subtotal = precioUnitario.multiplicar(
            cantidad.getValor().compareTo(1) == 0 ?
                java.math.BigDecimal.ONE :
                new java.math.BigDecimal(cantidad.getValor())
        );

        // Impuesto = subtotal * (porcentaje_impuesto / 100)
        this.impuesto = subtotal.aplicarPorcentaje(porcentajeImpuesto);

        // Descuento = subtotal * (porcentaje_descuento / 100)
        this.descuento = subtotal.aplicarPorcentaje(porcentajeDescuento);

        // Total = subtotal + impuesto - descuento
        this.total = subtotal.sumar(impuesto).restar(descuento);
    }

    /**
     * Recalcular totales si cambian porcentajes
     */
    public void recalcular() {
        calcularTotales();
    }

    // Getters
    public String getDescripcion() {
        return descripcion;
    }

    public Cantidad getCantidad() {
        return cantidad;
    }

    public Dinero getPrecioUnitario() {
        return precioUnitario;
    }

    public Porcentaje getPorcentajeImpuesto() {
        return porcentajeImpuesto;
    }

    public Porcentaje getPorcentajeDescuento() {
        return porcentajeDescuento;
    }

    public Dinero getSubtotal() {
        return subtotal;
    }

    public Dinero getImpuesto() {
        return impuesto;
    }

    public Dinero getDescuento() {
        return descuento;
    }

    public Dinero getTotal() {
        return total;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemFactura that = (ItemFactura) o;
        return Objects.equals(descripcion, that.descripcion) &&
               Objects.equals(cantidad, that.cantidad) &&
               Objects.equals(precioUnitario, that.precioUnitario);
    }

    @Override
    public int hashCode() {
        return Objects.hash(descripcion, cantidad, precioUnitario);
    }

    @Override
    public String toString() {
        return "ItemFactura{" +
                "descripcion='" + descripcion + '\'' +
                ", cantidad=" + cantidad +
                ", total=" + total +
                '}';
    }
}
