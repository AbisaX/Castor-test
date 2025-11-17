package com.castor.facturacion.infrastructure.adapter.in.rest.mapper;

import com.castor.facturacion.domain.Factura;
import com.castor.facturacion.domain.ItemFactura;
import com.castor.facturacion.domain.valueobject.Cantidad;
import com.castor.facturacion.domain.valueobject.Dinero;
import com.castor.facturacion.domain.valueobject.Porcentaje;
import com.castor.facturacion.infrastructure.adapter.in.rest.dto.CrearFacturaRequest;
import com.castor.facturacion.infrastructure.adapter.in.rest.dto.FacturaResponse;
import com.castor.facturacion.infrastructure.adapter.in.rest.dto.ItemFacturaRequest;
import com.castor.facturacion.infrastructure.adapter.in.rest.dto.ItemFacturaResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para convertir entre DTOs de la capa REST y objetos de Dominio.
 *
 * Responsable de la traducción bidireccional entre:
 * - CrearFacturaRequest -> Domain (Factura, ItemFactura)
 * - Domain (Factura, ItemFactura) -> FacturaResponse
 */
@Component
public class FacturaDTOMapper {

    /**
     * Convierte un DTO de request a una entidad de dominio Factura
     */
    public Factura toDomain(CrearFacturaRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("El request no puede ser nulo");
        }

        List<ItemFactura> items = request.getItems().stream()
            .map(this::itemToDomain)
            .collect(Collectors.toList());

        return Factura.crear(request.getClienteId(), items);
    }

    /**
     * Convierte un ItemFacturaRequest a ItemFactura del dominio
     */
    private ItemFactura itemToDomain(ItemFacturaRequest itemRequest) {
        if (itemRequest == null) {
            throw new IllegalArgumentException("El item request no puede ser nulo");
        }

        return ItemFactura.crear(
            itemRequest.getDescripcion(),
            Cantidad.of(itemRequest.getCantidad()),
            Dinero.of(itemRequest.getPrecioUnitario()),
            itemRequest.getPorcentajeImpuesto() != null
                ? Porcentaje.of(itemRequest.getPorcentajeImpuesto())
                : Porcentaje.cero(),
            itemRequest.getPorcentajeDescuento() != null
                ? Porcentaje.of(itemRequest.getPorcentajeDescuento())
                : Porcentaje.cero()
        );
    }

    /**
     * Convierte una Factura de dominio a FacturaResponse DTO
     */
    public FacturaResponse toResponse(Factura factura) {
        if (factura == null) {
            throw new IllegalArgumentException("La factura no puede ser nula");
        }

        List<ItemFacturaResponse> itemsResponse = factura.getItems().stream()
            .map(this::itemToResponse)
            .collect(Collectors.toList());

        return new FacturaResponse(
            factura.getId() != null ? factura.getId().getValue() : null,
            factura.getNumero() != null ? factura.getNumero().getValor() : null,
            factura.getClienteId(),
            itemsResponse,
            factura.getSubtotalGeneral().getCantidad(),
            factura.getTotalImpuestos().getCantidad(),
            factura.getTotalDescuentos().getCantidad(),
            factura.getTotalFinal().getCantidad(),
            factura.getFechaCreacion()
        );
    }

    /**
     * Convierte un ItemFactura de dominio a ItemFacturaResponse DTO
     */
    private ItemFacturaResponse itemToResponse(ItemFactura item) {
        if (item == null) {
            throw new IllegalArgumentException("El item no puede ser nulo");
        }

        ItemFacturaResponse response = new ItemFacturaResponse();
        response.setDescripcion(item.getDescripcion());
        response.setCantidad(item.getCantidad().getValor());
        response.setPrecioUnitario(item.getPrecioUnitario().getCantidad());
        response.setPorcentajeImpuesto(item.getPorcentajeImpuesto().getValor());
        response.setPorcentajeDescuento(item.getPorcentajeDescuento().getValor());
        response.setSubtotal(item.getSubtotal().getCantidad());
        response.setImpuesto(item.getImpuesto().getCantidad());
        response.setDescuento(item.getDescuento().getCantidad());
        response.setTotal(item.getTotal().getCantidad());

        return response;
    }

    /**
     * Convierte una lista de Facturas de dominio a lista de FacturaResponse
     */
    public List<FacturaResponse> toResponseList(List<Factura> facturas) {
        if (facturas == null) {
            throw new IllegalArgumentException("La lista de facturas no puede ser nula");
        }

        return facturas.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
}
