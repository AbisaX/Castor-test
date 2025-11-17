package com.castor.facturacion.domain.port.out;

import com.castor.facturacion.domain.Factura;
import com.castor.facturacion.domain.valueobject.FacturaId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Puerto de Salida (Driven Port)
 * Define el contrato para la persistencia de facturas.
 *
 * Este puerto es implementado por los adaptadores de persistencia
 * (FacturaRepositoryAdapter) en la capa de infraestructura.
 */
public interface FacturaRepositoryPort {

    /**
     * Guardar una factura
     *
     * @param factura Factura a guardar
     * @return Factura guardada
     */
    Factura guardar(Factura factura);

    /**
     * Buscar factura por ID
     *
     * @param id ID de la factura
     * @return Optional con la factura si existe
     */
    Optional<Factura> buscarPorId(FacturaId id);

    /**
     * Buscar factura por ID (valor Long)
     *
     * @param id ID de la factura
     * @return Optional con la factura si existe
     */
    Optional<Factura> buscarPorId(Long id);

    /**
     * Listar todas las facturas con paginación
     *
     * @param pageable Información de paginación
     * @return Página de facturas
     */
    Page<Factura> listarTodas(Pageable pageable);

    /**
     * Listar facturas por cliente con paginación
     *
     * @param clienteId ID del cliente
     * @param pageable Información de paginación
     * @return Página de facturas del cliente
     */
    Page<Factura> listarPorCliente(Long clienteId, Pageable pageable);

    /**
     * Verificar si existe una factura
     *
     * @param id ID de la factura
     * @return true si existe
     */
    boolean existe(FacturaId id);

    /**
     * Eliminar una factura
     *
     * @param id ID de la factura
     */
    void eliminar(FacturaId id);
}
