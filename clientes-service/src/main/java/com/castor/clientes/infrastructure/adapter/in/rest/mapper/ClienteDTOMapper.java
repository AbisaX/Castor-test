package com.castor.clientes.infrastructure.adapter.in.rest.mapper;

import com.castor.clientes.domain.Cliente;
import com.castor.clientes.domain.valueobject.*;
import com.castor.clientes.infrastructure.adapter.in.rest.dto.ClienteRequestDTO;
import com.castor.clientes.infrastructure.adapter.in.rest.dto.ClienteResponseDTO;
import org.springframework.stereotype.Component;

/**
 * Mapper entre DTOs y modelo de dominio DDD para Cliente
 * Convierte entre DTOs primitivos y Value Objects de dominio
 */
@Component
public class ClienteDTOMapper {

    public Cliente toDomain(ClienteRequestDTO dto) {
        return Cliente.crear(
            NombreCliente.of(dto.getNombre()),
            Nit.of(dto.getNit()),
            Email.of(dto.getEmail()),
            dto.getTelefono(),
            dto.getDireccion()
        );
    }

    public ClienteResponseDTO toResponse(Cliente cliente) {
        return ClienteResponseDTO.builder()
                .id(cliente.getId() != null && !cliente.getId().esNuevo() ? cliente.getId().getValor() : null)
                .nombre(cliente.getNombre().getValor())
                .nit(cliente.getNit().getValor())
                .email(cliente.getEmail().getValor())
                .telefono(cliente.getTelefono())
                .direccion(cliente.getDireccion())
                .activo(cliente.isActivo())
                .fechaCreacion(cliente.getFechaCreacion())
                .fechaActualizacion(cliente.getFechaActualizacion())
                .build();
    }
}
