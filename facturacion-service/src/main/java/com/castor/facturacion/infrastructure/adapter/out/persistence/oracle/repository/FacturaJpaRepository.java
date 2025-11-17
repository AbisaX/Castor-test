package com.castor.facturacion.infrastructure.adapter.out.persistence.oracle.repository;

import com.castor.facturacion.infrastructure.adapter.out.persistence.oracle.entity.FacturaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para Facturas.
 *
 * Extiende JpaRepository para operaciones CRUD básicas
 * e incluye queries personalizadas.
 */
@Repository
public interface FacturaJpaRepository extends JpaRepository<FacturaEntity, Long> {

    /**
     * Buscar facturas por ID de cliente con paginación
     */
    @Query("SELECT f FROM FacturaEntity f WHERE f.clienteId = :clienteId")
    Page<FacturaEntity> findByClienteId(@Param("clienteId") Long clienteId, Pageable pageable);

    /**
     * Buscar factura por número
     */
    @Query("SELECT f FROM FacturaEntity f WHERE f.numero = :numero")
    Optional<FacturaEntity> findByNumero(@Param("numero") String numero);

    /**
     * Verificar si existe una factura para un cliente
     */
    @Query("SELECT COUNT(f) > 0 FROM FacturaEntity f WHERE f.clienteId = :clienteId")
    boolean existsByClienteId(@Param("clienteId") Long clienteId);

    /**
     * Buscar factura con items (JOIN FETCH para evitar N+1)
     */
    @Query("SELECT f FROM FacturaEntity f LEFT JOIN FETCH f.items WHERE f.id = :id")
    Optional<FacturaEntity> findByIdWithItems(@Param("id") Long id);
}
