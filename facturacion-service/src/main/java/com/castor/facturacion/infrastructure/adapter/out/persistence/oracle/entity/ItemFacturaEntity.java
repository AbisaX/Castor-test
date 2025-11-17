package com.castor.facturacion.infrastructure.adapter.out.persistence.oracle.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Entidad JPA para Item de Factura (Oracle).
 *
 * Representa la tabla ITEMS_FACTURA en la base de datos Oracle.
 * Usa Lombok para reducir boilerplate code.
 */
@Entity
@Table(name = "ITEMS_FACTURA")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemFacturaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "item_factura_seq")
    @SequenceGenerator(name = "item_factura_seq", sequenceName = "ITEMS_FACTURA_SEQ", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FACTURA_ID", nullable = false)
    private FacturaEntity factura;

    @Column(name = "DESCRIPCION", nullable = false, length = 500)
    private String descripcion;

    @Column(name = "CANTIDAD", nullable = false)
    private Integer cantidad;

    @Column(name = "PRECIO_UNITARIO", nullable = false, precision = 15, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "PORCENTAJE_IMPUESTO", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeImpuesto;

    @Column(name = "PORCENTAJE_DESCUENTO", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeDescuento;

    @Column(name = "SUBTOTAL", nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "IMPUESTO", nullable = false, precision = 15, scale = 2)
    private BigDecimal impuesto;

    @Column(name = "DESCUENTO", nullable = false, precision = 15, scale = 2)
    private BigDecimal descuento;

    @Column(name = "TOTAL", nullable = false, precision = 15, scale = 2)
    private BigDecimal total;

    /**
     * Sobreescribir toString para evitar lazy loading issues
     */
    @Override
    public String toString() {
        return "ItemFacturaEntity{" +
               "id=" + id +
               ", descripcion='" + descripcion + '\'' +
               ", cantidad=" + cantidad +
               ", total=" + total +
               '}';
    }
}
