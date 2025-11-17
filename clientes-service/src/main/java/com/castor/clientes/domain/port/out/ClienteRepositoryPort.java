package com.castor.clientes.domain.port.out;

import com.castor.clientes.domain.Cliente;
import com.castor.clientes.domain.valueobject.ClienteId;
import com.castor.clientes.domain.valueobject.Nit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Puerto de salida: Repositorio de Clientes
 * Define el contrato para la persistencia de clientes (PostgreSQL)
 */
public interface ClienteRepositoryPort {
    Cliente guardar(Cliente cliente);
    Optional<Cliente> buscarPorId(ClienteId id);
    Page<Cliente> buscarTodos(Pageable pageable);
    void eliminar(ClienteId id);
    boolean existePorNit(Nit nit);
    Optional<Cliente> buscarPorNit(Nit nit);
}
