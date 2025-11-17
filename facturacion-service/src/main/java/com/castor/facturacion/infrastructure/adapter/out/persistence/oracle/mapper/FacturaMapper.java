package com.castor.facturacion.infrastructure.adapter.out.persistence.oracle.mapper;

import com.castor.facturacion.domain.Factura;
import com.castor.facturacion.domain.ItemFactura;
import com.castor.facturacion.domain.valueobject.*;
import com.castor.facturacion.infrastructure.adapter.out.persistence.oracle.entity.FacturaEntity;
import com.castor.facturacion.infrastructure.adapter.out.persistence.oracle.entity.ItemFacturaEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para convertir entre entidades de dominio y entidades JPA.
 *
 * Responsable de la traducción bidireccional entre:
 * - Domain (Factura, ItemFactura) ↔ JPA Entity (FacturaEntity, ItemFacturaEntity)
 */
@Component
public class FacturaMapper {

    /**
     * Convierte una Factura de dominio a FacturaEntity JPA
     */
    public FacturaEntity toEntity(Factura factura) {
        if (factura == null) {
            throw new IllegalArgumentException("La factura no puede ser nula");
        }

        FacturaEntity entity = FacturaEntity.builder()
            .id(factura.getId() != null && !factura.getId().esNuevo()
                ? factura.getId().getValor()
                : null)
            .numero(factura.getNumero() != null ? factura.getNumero().getValor() : null)
            .clienteId(factura.getClienteId())
            .subtotalGeneral(factura.getSubtotalGeneral().getCantidad())
            .totalImpuestos(factura.getTotalImpuestos().getCantidad())
            .totalDescuentos(factura.getTotalDescuentos().getCantidad())
            .totalFinal(factura.getTotalFinal().getCantidad())
            .fechaCreacion(factura.getFechaCreacion())
            .build();

        // Mapear items con relación bidireccional
        List<ItemFacturaEntity> itemEntities = factura.getItems().stream()
            .map(item -> toItemEntity(item, entity))
            .collect(Collectors.toList());

        entity.setItems(itemEntities);

        return entity;
    }

    /**
     * Convierte un ItemFactura de dominio a ItemFacturaEntity JPA
     */
    private ItemFacturaEntity toItemEntity(ItemFactura item, FacturaEntity facturaEntity) {
        if (item == null) {
            throw new IllegalArgumentException("El item no puede ser nulo");
        }

        return ItemFacturaEntity.builder()
            .factura(facturaEntity)
            .descripcion(item.getDescripcion())
            .cantidad(item.getCantidad().getValor())
            .precioUnitario(item.getPrecioUnitario().getCantidad())
            .porcentajeImpuesto(item.getPorcentajeImpuesto().getValor())
            .porcentajeDescuento(item.getPorcentajeDescuento().getValor())
            .subtotal(item.getSubtotal().getCantidad())
            .impuesto(item.getImpuesto().getCantidad())
            .descuento(item.getDescuento().getCantidad())
            .total(item.getTotal().getCantidad())
            .build();
    }

    /**
     * Convierte una FacturaEntity JPA a Factura de dominio
     */
    public Factura toDomain(FacturaEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("La entidad factura no puede ser nula");
        }

        // Mapear items
        List<ItemFactura> items = entity.getItems().stream()
            .map(this::toItemDomain)
            .collect(Collectors.toList());

        // Reconstruir factura desde persistencia
        return Factura.reconstituir(
            entity.getId() != null ? FacturaId.of(entity.getId()) : FacturaId.generate(),
            entity.getNumero() != null ? NumeroFactura.of(entity.getNumero()) : null,
            entity.getClienteId(),
            items,
            Dinero.of(entity.getSubtotalGeneral()),
            Dinero.of(entity.getTotalImpuestos()),
            Dinero.of(entity.getTotalDescuentos()),
            Dinero.of(entity.getTotalFinal()),
            entity.getFechaCreacion()
        );
    }

    /**
     * Convierte un ItemFacturaEntity JPA a ItemFactura de dominio
     */
    private ItemFactura toItemDomain(ItemFacturaEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("La entidad item no puede ser nula");
        }

        // Reconstruir item desde persistencia
        return ItemFactura.reconstituir(
            entity.getDescripcion(),
            Cantidad.of(entity.getCantidad()),
            Dinero.of(entity.getPrecioUnitario()),
            Porcentaje.of(entity.getPorcentajeImpuesto()),
            Porcentaje.of(entity.getPorcentajeDescuento()),
            Dinero.of(entity.getSubtotal()),
            Dinero.of(entity.getImpuesto()),
            Dinero.of(entity.getDescuento()),
            Dinero.of(entity.getTotal())
        );
    }

    /**
     * Convierte una lista de FacturaEntity a lista de Factura
     */
    public List<Factura> toDomainList(List<FacturaEntity> entities) {
        if (entities == null) {
            throw new IllegalArgumentException("La lista de entidades no puede ser nula");
        }

        return entities.stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Actualiza una FacturaEntity existente con datos de dominio
     * Útil para operaciones de actualización
     */
    public void updateEntity(Factura factura, FacturaEntity entity) {
        if (factura == null || entity == null) {
            throw new IllegalArgumentException("La factura y entidad no pueden ser nulas");
        }

        entity.setNumero(factura.getNumero().getValor());
        entity.setClienteId(factura.getClienteId());
        entity.setSubtotalGeneral(factura.getSubtotalGeneral().getCantidad());
        entity.setTotalImpuestos(factura.getTotalImpuestos().getCantidad());
        entity.setTotalDescuentos(factura.getTotalDescuentos().getCantidad());
        entity.setTotalFinal(factura.getTotalFinal().getCantidad());

        // Actualizar items (limpiar y agregar nuevos)
        entity.getItems().clear();

        List<ItemFacturaEntity> itemEntities = factura.getItems().stream()
            .map(item -> toItemEntity(item, entity))
            .collect(Collectors.toList());

        entity.getItems().addAll(itemEntities);
    }
}
