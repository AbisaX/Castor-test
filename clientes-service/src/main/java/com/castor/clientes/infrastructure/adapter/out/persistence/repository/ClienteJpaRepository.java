package com.castor.clientes.infrastructure.adapter.out.persistence.repository;

import com.castor.clientes.infrastructure.adapter.out.persistence.entity.ClienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad Cliente en PostgreSQL
 */
@Repository
public interface ClienteJpaRepository extends JpaRepository<ClienteEntity, Long> {
    boolean existsByNit(String nit);
    Optional<ClienteEntity> findByNit(String nit);
}
