package com.castor.facturacion.infrastructure.adapter.out.persistence.oracle;

import com.castor.facturacion.domain.Factura;
import com.castor.facturacion.domain.port.out.FacturaRepositoryPort;
import com.castor.facturacion.domain.valueobject.FacturaId;
import com.castor.facturacion.infrastructure.adapter.out.persistence.oracle.entity.FacturaEntity;
import com.castor.facturacion.infrastructure.adapter.out.persistence.oracle.mapper.FacturaMapper;
import com.castor.facturacion.infrastructure.adapter.out.persistence.oracle.repository.FacturaJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Adapter de persistencia para Facturas (Implementación del puerto FacturaRepositoryPort).
 *
 * Driven Adapter que implementa el puerto de salida FacturaRepositoryPort
 * y se conecta a la base de datos Oracle usando JPA.
 *
 * Responsabilidades:
 * - Persistencia de facturas en Oracle
 * - Ejecución de procedimientos almacenados PL/SQL
 * - Traducción entre dominio y entidades JPA
 */
@Repository
@Transactional
public class FacturaRepositoryAdapter implements FacturaRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(FacturaRepositoryAdapter.class);

    private final FacturaJpaRepository jpaRepository;
    private final FacturaMapper mapper;
    private final EntityManager entityManager;

    public FacturaRepositoryAdapter(
        FacturaJpaRepository jpaRepository,
        FacturaMapper mapper,
        EntityManager entityManager
    ) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.entityManager = entityManager;
    }

    @Override
    public Factura guardar(Factura factura) {
        log.debug("Guardando factura para cliente: {}", factura.getClienteId());

        // Validar cliente antes de guardar usando procedimiento PL/SQL
        validarClienteActivo(factura.getClienteId());

        // Convertir dominio a entidad JPA
        FacturaEntity entity = mapper.toEntity(factura);

        // Persistir
        FacturaEntity savedEntity = jpaRepository.save(entity);

        log.info("Factura guardada exitosamente con ID: {} y número: {}",
            savedEntity.getId(), savedEntity.getNumero());

        // Convertir entidad a dominio
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Factura> buscarPorId(FacturaId id) {
        if (id == null || id.esNuevo()) {
            log.warn("Intento de búsqueda con ID nulo o nuevo");
            return Optional.empty();
        }

        log.debug("Buscando factura por ID: {}", id.getValor());

        return jpaRepository.findByIdWithItems(id.getValor())
            .map(entity -> {
                log.debug("Factura encontrada: {}", entity.getNumero());
                return mapper.toDomain(entity);
            });
    }

    @Override
    public Optional<Factura> buscarPorId(Long id) {
        if (id == null || id <= 0) {
            log.warn("Intento de búsqueda con ID inválido: {}", id);
            return Optional.empty();
        }

        log.debug("Buscando factura por ID (Long): {}", id);

        return jpaRepository.findByIdWithItems(id)
            .map(entity -> {
                log.debug("Factura encontrada: {}", entity.getNumero());
                return mapper.toDomain(entity);
            });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Factura> listarTodas(Pageable pageable) {
        log.debug("Listando todas las facturas - page: {}, size: {}",
            pageable.getPageNumber(), pageable.getPageSize());

        Page<FacturaEntity> entityPage = jpaRepository.findAll(pageable);

        List<Factura> facturas = mapper.toDomainList(entityPage.getContent());

        log.debug("Total de facturas encontradas: {}", entityPage.getTotalElements());

        return new PageImpl<>(facturas, pageable, entityPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Factura> listarPorCliente(Long clienteId, Pageable pageable) {
        log.debug("Listando facturas del cliente {} - page: {}, size: {}",
            clienteId, pageable.getPageNumber(), pageable.getPageSize());

        Page<FacturaEntity> entityPage = jpaRepository.findByClienteId(clienteId, pageable);

        List<Factura> facturas = mapper.toDomainList(entityPage.getContent());

        log.debug("Total de facturas del cliente {}: {}", clienteId, entityPage.getTotalElements());

        return new PageImpl<>(facturas, pageable, entityPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existe(FacturaId id) {
        if (id == null || id.esNuevo()) {
            return false;
        }

        boolean existe = jpaRepository.existsById(id.getValor());
        log.debug("Verificando existencia de factura {}: {}", id.getValor(), existe);

        return existe;
    }

    @Override
    public void eliminar(FacturaId id) {
        if (id == null || id.esNuevo()) {
            throw new IllegalArgumentException("No se puede eliminar una factura sin ID válido");
        }

        log.info("Eliminando factura con ID: {}", id.getValor());

        jpaRepository.deleteById(id.getValor());

        log.debug("Factura eliminada exitosamente");
    }

    /**
     * Ejecuta el procedimiento almacenado PL/SQL para validar que el cliente esté activo.
     *
     * Procedimiento esperado en Oracle:
     * CREATE OR REPLACE PROCEDURE validar_cliente_activo(
     *   p_cliente_id IN NUMBER,
     *   p_es_activo OUT NUMBER,
     *   p_mensaje OUT VARCHAR2
     * ) AS
     * BEGIN
     *   SELECT COUNT(*) INTO p_es_activo
     *   FROM CLIENTES
     *   WHERE ID = p_cliente_id AND ACTIVO = 1;
     *
     *   IF p_es_activo = 0 THEN
     *     p_mensaje := 'Cliente no encontrado o inactivo';
     *   ELSE
     *     p_mensaje := 'OK';
     *   END IF;
     * END;
     *
     * @param clienteId ID del cliente a validar
     * @throws IllegalStateException si el cliente no está activo
     */
    private void validarClienteActivo(Long clienteId) {
        log.debug("Validando cliente activo mediante PL/SQL: {}", clienteId);

        try {
            StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("validar_cliente_activo")
                .registerStoredProcedureParameter("p_cliente_id", Long.class, ParameterMode.IN)
                .registerStoredProcedureParameter("p_es_activo", Integer.class, ParameterMode.OUT)
                .registerStoredProcedureParameter("p_mensaje", String.class, ParameterMode.OUT);

            query.setParameter("p_cliente_id", clienteId);
            query.execute();

            Integer esActivo = (Integer) query.getOutputParameterValue("p_es_activo");
            String mensaje = (String) query.getOutputParameterValue("p_mensaje");

            log.debug("Resultado validación cliente {}: esActivo={}, mensaje={}",
                clienteId, esActivo, mensaje);

            if (esActivo == null || esActivo == 0) {
                log.warn("Validación de cliente falló: {}", mensaje);
                throw new IllegalStateException("Cliente inválido: " + mensaje);
            }

            log.debug("Cliente {} validado exitosamente", clienteId);

        } catch (Exception e) {
            log.error("Error al ejecutar procedimiento de validación de cliente: {}", e.getMessage());

            // Si el procedimiento no existe, loguear advertencia pero no fallar
            // (útil para ambientes de desarrollo sin el procedimiento)
            if (e.getMessage().contains("ORA-06550") || e.getMessage().contains("does not exist")) {
                log.warn("Procedimiento validar_cliente_activo no encontrado. " +
                        "Continuando sin validación PL/SQL.");
            } else {
                throw new IllegalStateException("Error al validar cliente: " + e.getMessage(), e);
            }
        }
    }
}
