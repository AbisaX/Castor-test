package com.castor.facturacion.domain;

import com.castor.facturacion.domain.valueobject.Dinero;
import com.castor.facturacion.domain.valueobject.FacturaId;
import com.castor.facturacion.domain.valueobject.NumeroFactura;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Agregado Root: Factura
 * Bounded Context: Gestión de Facturación
 *
 * Representa una factura de venta con múltiples items.
 * Sin anotaciones de framework - Dominio puro.
 */
public class Factura {
    private FacturaId id;
    private NumeroFactura numero;
    private Long clienteId;  // Referencia al cliente (otro bounded context)
    private List<ItemFactura> items;
    private Dinero subtotalGeneral;
    private Dinero totalImpuestos;
    private Dinero totalDescuentos;
    private Dinero totalFinal;
    private LocalDateTime fechaCreacion;

    // Constructor privado
    private Factura() {
        this.items = new ArrayList<>();
    }

    private Factura(FacturaId id, NumeroFactura numero, Long clienteId,
                   List<ItemFactura> items, LocalDateTime fechaCreacion) {
        this.id = id;
        this.numero = numero;
        this.clienteId = clienteId;
        this.items = new ArrayList<>(items);
        this.fechaCreacion = fechaCreacion;
        calcularTotales();
    }

    /**
     * Factory Method: Crear nueva factura
     */
    public static Factura crear(Long clienteId, List<ItemFactura> items) {
        validarDatosCreacion(clienteId, items);

        return new Factura(
            FacturaId.generate(),
            NumeroFactura.generar(),
            clienteId,
            items,
            LocalDateTime.now()
        );
    }

    /**
     * Factory Method: Reconstruir desde persistencia
     */
    public static Factura reconstituir(FacturaId id, NumeroFactura numero, Long clienteId,
                                      List<ItemFactura> items, Dinero subtotalGeneral,
                                      Dinero totalImpuestos, Dinero totalDescuentos,
                                      Dinero totalFinal, LocalDateTime fechaCreacion) {
        Factura factura = new Factura();
        factura.id = id;
        factura.numero = numero;
        factura.clienteId = clienteId;
        factura.items = new ArrayList<>(items);
        factura.subtotalGeneral = subtotalGeneral;
        factura.totalImpuestos = totalImpuestos;
        factura.totalDescuentos = totalDescuentos;
        factura.totalFinal = totalFinal;
        factura.fechaCreacion = fechaCreacion;
        return factura;
    }

    private static void validarDatosCreacion(Long clienteId, List<ItemFactura> items) {
        if (clienteId == null || clienteId <= 0) {
            throw new IllegalArgumentException("El ID del cliente es obligatorio y debe ser positivo");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("La factura debe tener al menos un ítem");
        }
        if (items.size() > 100) {
            throw new IllegalArgumentException("La factura no puede tener más de 100 ítems");
        }
    }

    /**
     * Comportamiento de dominio: Agregar item a la factura
     */
    public void agregarItem(ItemFactura item) {
        if (item == null) {
            throw new IllegalArgumentException("El ítem no puede ser nulo");
        }
        if (this.items.size() >= 100) {
            throw new IllegalStateException("No se pueden agregar más de 100 ítems a una factura");
        }
        this.items.add(item);
        calcularTotales();
    }

    /**
     * Comportamiento de dominio: Calcular totales de la factura
     */
    public void calcularTotales() {
        if (items.isEmpty()) {
            this.subtotalGeneral = Dinero.cero();
            this.totalImpuestos = Dinero.cero();
            this.totalDescuentos = Dinero.cero();
            this.totalFinal = Dinero.cero();
            return;
        }

        // Sumar todos los subtotales
        this.subtotalGeneral = items.stream()
            .map(ItemFactura::getSubtotal)
            .reduce(Dinero.cero(), Dinero::sumar);

        // Sumar todos los impuestos
        this.totalImpuestos = items.stream()
            .map(ItemFactura::getImpuesto)
            .reduce(Dinero.cero(), Dinero::sumar);

        // Sumar todos los descuentos
        this.totalDescuentos = items.stream()
            .map(ItemFactura::getDescuento)
            .reduce(Dinero.cero(), Dinero::sumar);

        // Total final = subtotal + impuestos - descuentos
        this.totalFinal = subtotalGeneral.sumar(totalImpuestos).restar(totalDescuentos);
    }

    /**
     * Regla de negocio: Verificar si la factura puede ser modificada
     * (Solo facturas nuevas que aún no tienen ID persistido)
     */
    public boolean puedeSerModificada() {
        return this.id == null || this.id.esNuevo();
    }

    /**
     * Regla de negocio: Verificar si la factura es válida para guardar
     */
    public boolean esValida() {
        return clienteId != null &&
               !items.isEmpty() &&
               totalFinal != null &&
               !totalFinal.esCero();
    }

    // Getters sin setters (inmutabilidad controlada)
    public FacturaId getId() {
        return id;
    }

    public NumeroFactura getNumero() {
        return numero;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public List<ItemFactura> getItems() {
        return Collections.unmodifiableList(items);
    }

    public Dinero getSubtotalGeneral() {
        return subtotalGeneral;
    }

    public Dinero getTotalImpuestos() {
        return totalImpuestos;
    }

    public Dinero getTotalDescuentos() {
        return totalDescuentos;
    }

    public Dinero getTotalFinal() {
        return totalFinal;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    // Método interno para establecer ID después de persistencia
    void setId(FacturaId id) {
        if (this.id != null && !this.id.esNuevo()) {
            throw new IllegalStateException("No se puede cambiar el ID de una factura existente");
        }
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Factura factura = (Factura) o;
        return Objects.equals(id, factura.id) || Objects.equals(numero, factura.numero);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, numero);
    }

    @Override
    public String toString() {
        return "Factura{" +
                "id=" + id +
                ", numero=" + numero +
                ", clienteId=" + clienteId +
                ", items=" + items.size() +
                ", totalFinal=" + totalFinal +
                '}';
    }
}
