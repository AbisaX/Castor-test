package com.castor.facturacion.application.service;

import com.castor.facturacion.domain.Factura;
import com.castor.facturacion.domain.port.in.FacturaUseCase;
import com.castor.facturacion.domain.port.out.ClienteValidationPort;
import com.castor.facturacion.domain.port.out.FacturaRepositoryPort;
import com.castor.facturacion.domain.port.out.TaxCalculatorPort;
import com.castor.facturacion.domain.valueobject.FacturaId;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Servicio de Aplicación que implementa los casos de uso de Factura.
 *
 * Esta clase orquesta la lógica de negocio y coordina entre el dominio
 * y los puertos de salida (repositorios, servicios externos).
 */
@Service
@Transactional
public class FacturaService implements FacturaUseCase {

    private static final Logger log = LoggerFactory.getLogger(FacturaService.class);

    private final FacturaRepositoryPort facturaRepository;
    private final ClienteValidationPort clienteValidation;
    private final TaxCalculatorPort taxCalculator;

    public FacturaService(FacturaRepositoryPort facturaRepository,
                          ClienteValidationPort clienteValidation,
                          TaxCalculatorPort taxCalculator) {
        this.facturaRepository = facturaRepository;
        this.clienteValidation = clienteValidation;
        this.taxCalculator = taxCalculator;
    }

    @Override
    @CircuitBreaker(name = "facturacion-service", fallbackMethod = "fallbackCrearFactura")
    @Retry(name = "facturacion-service")
    public Factura crearFactura(Factura factura) {
        log.info("Creando factura para cliente: {}", factura.getClienteId());

        // Validación de negocio: El cliente debe estar activo
        if (!clienteValidation.esClienteActivo(factura.getClienteId())) {
            log.error("Intento de crear factura para cliente inactivo o inexistente: {}",
                     factura.getClienteId());
            throw new IllegalStateException(
                "No se puede crear factura. El cliente no existe o no está activo: "
                + factura.getClienteId()
            );
        }

        // Validación del dominio
        if (!factura.esValida()) {
            log.error("Intento de crear factura inválida: {}", factura);
            throw new IllegalArgumentException("La factura no cumple con las reglas de negocio");
        }

        // Calcular totales si es necesario (ya calculados en el dominio)
        factura.calcularTotales();

        // Persistir la factura
        Factura facturaGuardada = facturaRepository.guardar(factura);
        log.info("Factura creada exitosamente: {}", facturaGuardada.getNumero());

        return facturaGuardada;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Factura> obtenerFacturaPorId(FacturaId id) {
        log.debug("Buscando factura por ID: {}", id);
        return facturaRepository.buscarPorId(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Factura> obtenerFacturaPorId(Long id) {
        log.debug("Buscando factura por ID: {}", id);
        return facturaRepository.buscarPorId(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Factura> listarFacturas(Pageable pageable) {
        log.debug("Listando facturas con paginación: {}", pageable);
        return facturaRepository.listarTodas(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Factura> listarFacturasPorCliente(Long clienteId, Pageable pageable) {
        log.debug("Listando facturas para cliente: {} con paginación: {}", clienteId, pageable);

        // Validar que el cliente exista
        if (!clienteValidation.existeCliente(clienteId)) {
            log.warn("Búsqueda de facturas para cliente inexistente: {}", clienteId);
            throw new IllegalArgumentException("El cliente no existe: " + clienteId);
        }

        return facturaRepository.listarPorCliente(clienteId, pageable);
    }

    @Override
    public void anularFactura(FacturaId id) {
        log.info("Anulando factura: {}", id);

        Factura factura = facturaRepository.buscarPorId(id)
            .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada: " + id));

        // Aquí podrías agregar lógica adicional (ej: verificar que no esté ya anulada)

        facturaRepository.eliminar(id);
        log.info("Factura anulada exitosamente: {}", factura.getNumero());
    }

    /**
     * Método fallback para cuando falla la creación de factura
     */
    private Factura fallbackCrearFactura(Factura factura, Exception ex) {
        log.error("Fallback activado al crear factura para cliente: {}. Error: {}",
                 factura.getClienteId(), ex.getMessage());
        throw new RuntimeException(
            "El servicio de facturación no está disponible temporalmente. " +
            "Por favor, intente nuevamente más tarde.", ex
        );
    }
}
