package com.castor.clientes.application.service;

import com.castor.clientes.domain.Cliente;
import com.castor.clientes.domain.port.in.ClienteUseCase;
import com.castor.clientes.domain.port.out.ClienteRepositoryPort;
import com.castor.clientes.domain.valueobject.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * Servicio de aplicación: Gestión de Clientes
 * Implementa los casos de uso de cliente con DDD
 * Incluye Circuit Breaker y Retry para resiliencia
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClienteService implements ClienteUseCase {

    private final ClienteRepositoryPort clienteRepository;
    private static final String RESILIENCE_INSTANCE = "clienteService";

    @Override
    @Transactional
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "crearClienteFallback")
    @Retry(name = RESILIENCE_INSTANCE)
    public Cliente crearCliente(Cliente cliente) {
        log.info("Creando nuevo cliente: {}", cliente.getNombre());
        log.debug("Iniciando creación con NIT: {} y email: {}", cliente.getNit(), cliente.getEmail());

        // Verificar que el NIT no exista
        if (clienteRepository.existePorNit(cliente.getNit())) {
            log.warn("Intento de crear cliente con NIT duplicado: {}", cliente.getNit());
            throw new IllegalArgumentException("Ya existe un cliente con el NIT: " + cliente.getNit());
        }

        Cliente clienteGuardado = clienteRepository.guardar(cliente);
        log.info("Cliente creado exitosamente con ID: {}", clienteGuardado.getId());

        return clienteGuardado;
    }

    /**
     * Fallback para crearCliente cuando falla el circuit breaker
     */
    private Cliente crearClienteFallback(Cliente cliente, Exception ex) {
        log.error("Fallback activado para crearCliente. Error: {}", ex.getMessage(), ex);
        throw new IllegalStateException("El servicio de clientes no está disponible temporalmente. Por favor intente más tarde.", ex);
    }

    @Override
    @Transactional
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "actualizarClienteFallback")
    @Retry(name = RESILIENCE_INSTANCE)
    public Cliente actualizarCliente(Long id, Cliente clienteActualizado) {
        log.info("Actualizando cliente con ID: {}", id);
        log.debug("Actualizando con datos: nombre={}, NIT={}",
                  clienteActualizado.getNombre(), clienteActualizado.getNit());

        ClienteId clienteId = ClienteId.of(id);
        Cliente clienteExistente = clienteRepository.buscarPorId(clienteId)
                .orElseThrow(() -> {
                    log.warn("Cliente no encontrado con ID: {}", id);
                    return new IllegalArgumentException("Cliente no encontrado con ID: " + id);
                });

        // Validar NIT si cambió
        if (!clienteExistente.getNit().equals(clienteActualizado.getNit()) &&
            clienteRepository.existePorNit(clienteActualizado.getNit())) {
            log.warn("Intento de actualizar con NIT duplicado: {}", clienteActualizado.getNit());
            throw new IllegalArgumentException("Ya existe un cliente con el NIT: " + clienteActualizado.getNit());
        }

        // Usar método de dominio para actualizar
        clienteExistente.actualizar(
            clienteActualizado.getNombre(),
            clienteActualizado.getEmail(),
            clienteActualizado.getTelefono(),
            clienteActualizado.getDireccion()
        );

        // Manejar activación/desactivación
        if (clienteActualizado.isActivo() && !clienteExistente.isActivo()) {
            log.debug("Activando cliente {}", id);
            clienteExistente.activar();
        } else if (!clienteActualizado.isActivo() && clienteExistente.isActivo()) {
            log.debug("Desactivando cliente {}", id);
            clienteExistente.desactivar();
        }

        Cliente clienteGuardado = clienteRepository.guardar(clienteExistente);
        log.info("Cliente {} actualizado exitosamente", id);

        return clienteGuardado;
    }

    /**
     * Fallback para actualizarCliente cuando falla el circuit breaker
     */
    private Cliente actualizarClienteFallback(Long id, Cliente clienteActualizado, Exception ex) {
        log.error("Fallback activado para actualizarCliente con ID: {}. Error: {}", id, ex.getMessage(), ex);
        throw new IllegalStateException("El servicio de clientes no está disponible temporalmente. Por favor intente más tarde.", ex);
    }

    @Override
    @Transactional(readOnly = true)
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "obtenerClienteFallback")
    @Retry(name = RESILIENCE_INSTANCE)
    public Cliente obtenerCliente(Long id) {
        log.info("Buscando cliente con ID: {}", id);
        ClienteId clienteId = ClienteId.of(id);
        return clienteRepository.buscarPorId(clienteId)
                .orElseThrow(() -> {
                    log.warn("Cliente no encontrado con ID: {}", id);
                    return new IllegalArgumentException("Cliente no encontrado con ID: " + id);
                });
    }

    /**
     * Fallback para obtenerCliente cuando falla el circuit breaker
     */
    private Cliente obtenerClienteFallback(Long id, Exception ex) {
        log.error("Fallback activado para obtenerCliente con ID: {}. Error: {}", id, ex.getMessage(), ex);
        throw new IllegalStateException("El servicio de clientes no está disponible temporalmente. Por favor intente más tarde.", ex);
    }

    @Override
    @Transactional(readOnly = true)
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "obtenerClientesFallback")
    @Retry(name = RESILIENCE_INSTANCE)
    public Page<Cliente> obtenerClientes(Pageable pageable) {
        log.info("Obteniendo clientes - Página: {}, Tamaño: {}", pageable.getPageNumber(), pageable.getPageSize());

        // Validación defensiva de paginación
        if (pageable.getPageSize() > 100) {
            log.warn("Tamaño de página excede el máximo: {}", pageable.getPageSize());
            throw new IllegalArgumentException("El tamaño de página no puede exceder 100 elementos");
        }

        Page<Cliente> result = clienteRepository.buscarTodos(pageable);
        log.debug("Encontrados {} clientes de un total de {}", result.getNumberOfElements(), result.getTotalElements());
        return result;
    }

    /**
     * Fallback para obtenerClientes cuando falla el circuit breaker
     */
    private Page<Cliente> obtenerClientesFallback(Pageable pageable, Exception ex) {
        log.error("Fallback activado para obtenerClientes. Error: {}", ex.getMessage(), ex);
        // Retornar página vacía en lugar de lanzar excepción
        return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }

    @Override
    @Transactional
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "eliminarClienteFallback")
    @Retry(name = RESILIENCE_INSTANCE)
    public void eliminarCliente(Long id) {
        log.info("Eliminando cliente con ID: {}", id);

        ClienteId clienteId = ClienteId.of(id);
        if (!clienteRepository.buscarPorId(clienteId).isPresent()) {
            log.warn("Intento de eliminar cliente inexistente con ID: {}", id);
            throw new IllegalArgumentException("Cliente no encontrado con ID: " + id);
        }

        clienteRepository.eliminar(clienteId);
        log.info("Cliente {} eliminado exitosamente", id);
    }

    /**
     * Fallback para eliminarCliente cuando falla el circuit breaker
     */
    private void eliminarClienteFallback(Long id, Exception ex) {
        log.error("Fallback activado para eliminarCliente con ID: {}. Error: {}", id, ex.getMessage(), ex);
        throw new IllegalStateException("El servicio de clientes no está disponible temporalmente. Por favor intente más tarde.", ex);
    }

    @Override
    @Transactional(readOnly = true)
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "existePorNitFallback")
    public boolean existePorNit(String nit) {
        log.debug("Verificando existencia de NIT: {}", nit);
        Nit nitVO = Nit.of(nit);
        boolean existe = clienteRepository.existePorNit(nitVO);
        log.debug("NIT {} existe: {}", nit, existe);
        return existe;
    }

    /**
     * Fallback para existePorNit cuando falla el circuit breaker
     */
    private boolean existePorNitFallback(String nit, Exception ex) {
        log.error("Fallback activado para existePorNit con NIT: {}. Error: {}", nit, ex.getMessage(), ex);
        // Retornar false como valor seguro (permite la operación)
        return false;
    }
}
