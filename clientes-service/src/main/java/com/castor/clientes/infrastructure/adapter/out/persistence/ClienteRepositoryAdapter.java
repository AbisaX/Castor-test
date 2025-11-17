package com.castor.clientes.infrastructure.adapter.out.persistence;

import com.castor.clientes.domain.Cliente;
import com.castor.clientes.domain.port.out.ClienteRepositoryPort;
import com.castor.clientes.domain.valueobject.ClienteId;
import com.castor.clientes.domain.valueobject.Nit;
import com.castor.clientes.infrastructure.adapter.out.persistence.mapper.ClienteMapper;
import com.castor.clientes.infrastructure.adapter.out.persistence.repository.ClienteJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adaptador de persistencia para Cliente (PostgreSQL)
 * Implementa el puerto de salida ClienteRepositoryPort
 * Traduce entre objetos de dominio DDD y entidades JPA
 */
@Component
@RequiredArgsConstructor
public class ClienteRepositoryAdapter implements ClienteRepositoryPort {

    private final ClienteJpaRepository jpaRepository;
    private final ClienteMapper mapper;

    @Override
    public Cliente guardar(Cliente cliente) {
        var entity = mapper.toEntity(cliente);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Cliente> buscarPorId(ClienteId id) {
        if (id == null || id.esNuevo()) {
            return Optional.empty();
        }
        return jpaRepository.findById(id.getValor())
                .map(mapper::toDomain);
    }

    @Override
    public Page<Cliente> buscarTodos(Pageable pageable) {
        return jpaRepository.findAll(pageable)
                .map(mapper::toDomain);
    }

    @Override
    public void eliminar(ClienteId id) {
        if (id != null && !id.esNuevo()) {
            jpaRepository.deleteById(id.getValor());
        }
    }

    @Override
    public boolean existePorNit(Nit nit) {
        if (nit == null) {
            return false;
        }
        return jpaRepository.existsByNit(nit.getValor());
    }

    @Override
    public Optional<Cliente> buscarPorNit(Nit nit) {
        if (nit == null) {
            return Optional.empty();
        }
        return jpaRepository.findByNit(nit.getValor())
                .map(mapper::toDomain);
    }
}
