package com.castor.facturacion.domain.port.out;

/**
 * Puerto de Salida (Driven Port)
 * Define el contrato para validar clientes.
 *
 * Este puerto se implementa en la infraestructura para comunicarse
 * con el microservicio de clientes o ejecutar procedimientos almacenados.
 */
public interface ClienteValidationPort {

    /**
     * Verificar si un cliente existe y está activo
     *
     * @param clienteId ID del cliente
     * @return true si el cliente existe y está activo
     */
    boolean esClienteActivo(Long clienteId);

    /**
     * Verificar si un cliente existe
     *
     * @param clienteId ID del cliente
     * @return true si el cliente existe
     */
    boolean existeCliente(Long clienteId);
}
