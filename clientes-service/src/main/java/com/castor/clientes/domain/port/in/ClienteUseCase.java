package com.castor.clientes.domain.port.in;

import com.castor.clientes.domain.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Puerto de entrada: Casos de uso de Cliente
 * Define las operaciones disponibles para la gestión de clientes
 */
public interface ClienteUseCase {
    Cliente crearCliente(Cliente cliente);
    Cliente actualizarCliente(Long id, Cliente cliente);
    Cliente obtenerCliente(Long id);
    Page<Cliente> obtenerClientes(Pageable pageable);
    void eliminarCliente(Long id);
    boolean existePorNit(String nit);
}
