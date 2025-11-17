package com.castor.clientes.infrastructure.adapter.out.persistence.mapper;

import com.castor.clientes.domain.Cliente;
import com.castor.clientes.domain.valueobject.*;
import com.castor.clientes.infrastructure.adapter.out.persistence.entity.ClienteEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper entre entidad JPA y modelo de dominio DDD para Cliente
 * Convierte entre Value Objects y primitivos
 */
@Component
public class ClienteMapper {

    public Cliente toDomain(ClienteEntity entity) {
        if (entity == null) {
            return null;
        }

        return Cliente.reconstituir(
            ClienteId.of(entity.getId()),
            NombreCliente.of(entity.getNombre()),
            Nit.of(entity.getNit()),
            Email.of(entity.getEmail()),
            entity.getTelefono(),
            entity.getDireccion(),
            entity.getActivo(),
            entity.getFechaCreacion(),
            entity.getFechaActualizacion()
        );
    }

    public ClienteEntity toEntity(Cliente domain) {
        if (domain == null) {
            return null;
        }

        return ClienteEntity.builder()
                .id(domain.getId() != null && !domain.getId().esNuevo() ? domain.getId().getValor() : null)
                .nombre(domain.getNombre().getValor())
                .nit(domain.getNit().getValor())
                .email(domain.getEmail().getValor())
                .telefono(domain.getTelefono())
                .direccion(domain.getDireccion())
                .activo(domain.isActivo())
                .fechaCreacion(domain.getFechaCreacion())
                .fechaActualizacion(domain.getFechaActualizacion())
                .build();
    }
}
