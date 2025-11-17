package com.castor.facturacion.domain.port.in;

import com.castor.facturacion.domain.Factura;
import com.castor.facturacion.domain.valueobject.FacturaId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Puerto de Entrada (Driving Port)
 * Define los casos de uso para la gestión de facturas.
 *
 * Este puerto es implementado por la capa de aplicación (FacturaService)
 * y usado por los adaptadores de entrada (Controllers).
 */
public interface FacturaUseCase {

    /**
     * Crear una nueva factura
     *
     * @param factura Factura a crear
     * @return Factura creada con ID asignado
     * @throws IllegalArgumentException si la factura no es válida
     * @throws IllegalStateException si el cliente no está activo
     */
    Factura crearFactura(Factura factura);

    /**
     * Obtener una factura por su ID
     *
     * @param id ID de la factura
     * @return Optional con la factura si existe
     */
    Optional<Factura> obtenerFacturaPorId(FacturaId id);

    /**
     * Obtener una factura por su ID (valor Long)
     *
     * @param id ID de la factura
     * @return Optional con la factura si existe
     */
    Optional<Factura> obtenerFacturaPorId(Long id);

    /**
     * Listar todas las facturas con paginación
     *
     * @param pageable Información de paginación
     * @return Página de facturas
     */
    Page<Factura> listarFacturas(Pageable pageable);

    /**
     * Listar facturas por cliente con paginación
     *
     * @param clienteId ID del cliente
     * @param pageable Información de paginación
     * @return Página de facturas del cliente
     */
    Page<Factura> listarFacturasPorCliente(Long clienteId, Pageable pageable);

    /**
     * Anular una factura
     *
     * @param id ID de la factura a anular
     * @throws IllegalArgumentException si la factura no existe
     */
    void anularFactura(FacturaId id);
}
