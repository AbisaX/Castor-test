package com.castor.facturacion.infrastructure.adapter.out.persistence.oracle.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad JPA para Factura (Oracle).
 *
 * Representa la tabla FACTURAS en la base de datos Oracle.
 * Usa Lombok para reducir boilerplate code.
 */
@Entity
@Table(name = "FACTURAS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacturaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "factura_seq")
    @SequenceGenerator(name = "factura_seq", sequenceName = "FACTURAS_SEQ", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NUMERO", nullable = false, unique = true, length = 50)
    private String numero;

    @Column(name = "CLIENTE_ID", nullable = false)
    private Long clienteId;

    @OneToMany(
        mappedBy = "factura",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<ItemFacturaEntity> items = new ArrayList<>();

    @Column(name = "SUBTOTAL_GENERAL", nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotalGeneral;

    @Column(name = "TOTAL_IMPUESTOS", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalImpuestos;

    @Column(name = "TOTAL_DESCUENTOS", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalDescuentos;

    @Column(name = "TOTAL_FINAL", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalFinal;

    @Column(name = "FECHA_CREACION", nullable = false)
    private LocalDateTime fechaCreacion;

    /**
     * Método auxiliar para agregar items a la factura
     * Mantiene la sincronización bidireccional
     */
    public void addItem(ItemFacturaEntity item) {
        items.add(item);
        item.setFactura(this);
    }

    /**
     * Método auxiliar para eliminar items de la factura
     * Mantiene la sincronización bidireccional
     */
    public void removeItem(ItemFacturaEntity item) {
        items.remove(item);
        item.setFactura(null);
    }

    /**
     * Pre-persist callback para establecer valores por defecto
     */
    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
}
