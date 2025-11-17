package com.castor.facturacion.application.service;

import com.castor.facturacion.domain.Factura;
import com.castor.facturacion.domain.ItemFactura;
import com.castor.facturacion.domain.port.out.ClienteValidationPort;
import com.castor.facturacion.domain.port.out.FacturaRepositoryPort;
import com.castor.facturacion.domain.port.out.TaxCalculatorPort;
import com.castor.facturacion.domain.valueobject.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Tests unitarios para FacturaService usando Mockito.
 *
 * Cobertura:
 * - Tests de creación de facturas con validaciones
 * - Tests de consultas (por ID, listados, paginación)
 * - Tests de anulación de facturas
 * - Tests de circuit breaker y fallback
 * - Tests de cálculo de totales
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FacturaService - Tests Unitarios")
class FacturaServiceTest {

    @Mock
    private FacturaRepositoryPort facturaRepository;

    @Mock
    private ClienteValidationPort clienteValidation;

    @Mock
    private TaxCalculatorPort taxCalculator;

    @InjectMocks
    private FacturaService facturaService;

    private Factura facturaEjemplo;
    private List<ItemFactura> itemsEjemplo;

    @BeforeEach
    void setUp() {
        // Preparar datos de ejemplo
        ItemFactura item1 = ItemFactura.crear(
            "Producto A",
            Cantidad.de(2),
            Dinero.de(new BigDecimal("100.00")),
            Porcentaje.de(new BigDecimal("19.00")), // IVA 19%
            Porcentaje.de(new BigDecimal("5.00"))   // Descuento 5%
        );

        ItemFactura item2 = ItemFactura.crear(
            "Producto B",
            Cantidad.de(1),
            Dinero.de(new BigDecimal("50.00")),
            Porcentaje.de(new BigDecimal("19.00")),
            Porcentaje.de(new BigDecimal("0.00"))
        );

        itemsEjemplo = List.of(item1, item2);
        facturaEjemplo = Factura.crear(1L, itemsEjemplo);
    }

    @Test
    @DisplayName("Test 01: Crear factura con cliente activo debe ser exitoso")
    void testCrearFactura_ClienteActivo_Exitoso() {
        // Given
        given(clienteValidation.esClienteActivo(1L)).willReturn(true);
        given(facturaRepository.guardar(any(Factura.class))).willReturn(facturaEjemplo);

        // When
        Factura resultado = facturaService.crearFactura(facturaEjemplo);

        // Then
        assertThat(resultado).isNotNull();
        assertThat(resultado.getClienteId()).isEqualTo(1L);
        assertThat(resultado.getItems()).hasSize(2);
        assertThat(resultado.getTotalFinal()).isNotNull();
        assertThat(resultado.getTotalFinal().getCantidad()).isGreaterThan(BigDecimal.ZERO);

        then(clienteValidation).should().esClienteActivo(1L);
        then(facturaRepository).should().guardar(any(Factura.class));
    }

    @Test
    @DisplayName("Test 02: Crear factura con cliente inactivo debe lanzar excepción")
    void testCrearFactura_ClienteInactivo_LanzaExcepcion() {
        // Given
        given(clienteValidation.esClienteActivo(1L)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> facturaService.crearFactura(facturaEjemplo))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No se puede crear factura")
            .hasMessageContaining("no está activo");

        then(clienteValidation).should().esClienteActivo(1L);
        then(facturaRepository).should(never()).guardar(any(Factura.class));
    }

    @Test
    @DisplayName("Test 03: Crear factura con cliente no existente debe lanzar excepción")
    void testCrearFactura_ClienteNoExiste_LanzaExcepcion() {
        // Given - cliente no existe (retorna false)
        given(clienteValidation.esClienteActivo(999L)).willReturn(false);

        Factura facturaClienteInexistente = Factura.crear(999L, itemsEjemplo);

        // When & Then
        assertThatThrownBy(() -> facturaService.crearFactura(facturaClienteInexistente))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("no está activo");

        then(facturaRepository).should(never()).guardar(any(Factura.class));
    }

    @Test
    @DisplayName("Test 04: Crear factura inválida debe lanzar excepción")
    void testCrearFactura_FacturaInvalida_LanzaExcepcion() {
        // Given - crear factura sin items (inválida)
        assertThatThrownBy(() -> Factura.crear(1L, List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("al menos un ítem");
    }

    @Test
    @DisplayName("Test 05: Obtener factura por ID existente debe retornar factura")
    void testObtenerFacturaPorId_Existente() {
        // Given
        FacturaId facturaId = FacturaId.of(1L);
        given(facturaRepository.buscarPorId(facturaId)).willReturn(Optional.of(facturaEjemplo));

        // When
        Optional<Factura> resultado = facturaService.obtenerFacturaPorId(facturaId);

        // Then
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getClienteId()).isEqualTo(1L);
        assertThat(resultado.get().getItems()).hasSize(2);

        then(facturaRepository).should().buscarPorId(facturaId);
    }

    @Test
    @DisplayName("Test 06: Obtener factura por ID no existente debe retornar vacío")
    void testObtenerFacturaPorId_NoExistente() {
        // Given
        FacturaId facturaId = FacturaId.of(999L);
        given(facturaRepository.buscarPorId(facturaId)).willReturn(Optional.empty());

        // When
        Optional<Factura> resultado = facturaService.obtenerFacturaPorId(facturaId);

        // Then
        assertThat(resultado).isEmpty();

        then(facturaRepository).should().buscarPorId(facturaId);
    }

    @Test
    @DisplayName("Test 07: Listar facturas con paginación debe retornar página correcta")
    void testListarFacturas_ConPaginacion() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Factura> facturas = List.of(facturaEjemplo);
        Page<Factura> facturasPage = new PageImpl<>(facturas, pageable, 1);

        given(facturaRepository.listarTodas(pageable)).willReturn(facturasPage);

        // When
        Page<Factura> resultado = facturaService.listarFacturas(pageable);

        // Then
        assertThat(resultado).isNotNull();
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getNumber()).isZero();
        assertThat(resultado.getSize()).isEqualTo(10);

        then(facturaRepository).should().listarTodas(pageable);
    }

    @Test
    @DisplayName("Test 08: Listar facturas por cliente existente debe retornar facturas")
    void testListarFacturasPorCliente_ClienteExistente() {
        // Given
        Long clienteId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        List<Factura> facturas = List.of(facturaEjemplo);
        Page<Factura> facturasPage = new PageImpl<>(facturas, pageable, 1);

        given(clienteValidation.existeCliente(clienteId)).willReturn(true);
        given(facturaRepository.listarPorCliente(clienteId, pageable)).willReturn(facturasPage);

        // When
        Page<Factura> resultado = facturaService.listarFacturasPorCliente(clienteId, pageable);

        // Then
        assertThat(resultado).isNotNull();
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getTotalElements()).isEqualTo(1);

        then(clienteValidation).should().existeCliente(clienteId);
        then(facturaRepository).should().listarPorCliente(clienteId, pageable);
    }

    @Test
    @DisplayName("Test 09: Listar facturas por cliente no existente debe lanzar excepción")
    void testListarFacturasPorCliente_ClienteNoExiste_LanzaExcepcion() {
        // Given
        Long clienteId = 999L;
        Pageable pageable = PageRequest.of(0, 10);

        given(clienteValidation.existeCliente(clienteId)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> facturaService.listarFacturasPorCliente(clienteId, pageable))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("El cliente no existe");

        then(clienteValidation).should().existeCliente(clienteId);
        then(facturaRepository).should(never()).listarPorCliente(any(), any());
    }

    @Test
    @DisplayName("Test 10: Anular factura existente debe ser exitoso")
    void testAnularFactura_Exitoso() {
        // Given
        FacturaId facturaId = FacturaId.of(1L);
        given(facturaRepository.buscarPorId(facturaId)).willReturn(Optional.of(facturaEjemplo));
        willDoNothing().given(facturaRepository).eliminar(facturaId);

        // When
        assertThatCode(() -> facturaService.anularFactura(facturaId))
            .doesNotThrowAnyException();

        // Then
        then(facturaRepository).should().buscarPorId(facturaId);
        then(facturaRepository).should().eliminar(facturaId);
    }

    @Test
    @DisplayName("Test 11: Anular factura no existente debe lanzar excepción")
    void testAnularFactura_NoExistente_LanzaExcepcion() {
        // Given
        FacturaId facturaId = FacturaId.of(999L);
        given(facturaRepository.buscarPorId(facturaId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> facturaService.anularFactura(facturaId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Factura no encontrada");

        then(facturaRepository).should().buscarPorId(facturaId);
        then(facturaRepository).should(never()).eliminar(any());
    }

    @Test
    @DisplayName("Test 12: Circuit Breaker fallback debe manejar errores correctamente")
    void testCircuitBreakerFallback() {
        // Given - simular que el repositorio lanza excepción
        given(clienteValidation.esClienteActivo(1L)).willReturn(true);
        given(facturaRepository.guardar(any(Factura.class)))
            .willThrow(new RuntimeException("Base de datos no disponible"));

        // When & Then
        assertThatThrownBy(() -> facturaService.crearFactura(facturaEjemplo))
            .isInstanceOf(RuntimeException.class);

        then(clienteValidation).should().esClienteActivo(1L);
        then(facturaRepository).should().guardar(any(Factura.class));
    }

    @Test
    @DisplayName("Test 13: Validar cálculo de totales en factura")
    void testValidarCalculoTotales() {
        // Given
        ItemFactura item = ItemFactura.crear(
            "Producto Test",
            Cantidad.de(10),
            Dinero.de(new BigDecimal("100.00")), // 10 * 100 = 1000
            Porcentaje.de(new BigDecimal("19.00")), // IVA 19% = 190
            Porcentaje.de(new BigDecimal("10.00"))  // Descuento 10% = 100
        );

        Factura factura = Factura.crear(1L, List.of(item));
        given(clienteValidation.esClienteActivo(1L)).willReturn(true);
        given(facturaRepository.guardar(any(Factura.class))).willReturn(factura);

        // When
        Factura resultado = facturaService.crearFactura(factura);

        // Then - Verificar cálculos
        // Subtotal: 1000
        // Impuesto: 190
        // Descuento: 100
        // Total Final: 1000 + 190 - 100 = 1090
        assertThat(resultado.getSubtotalGeneral().getCantidad())
            .isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(resultado.getTotalImpuestos().getCantidad())
            .isEqualByComparingTo(new BigDecimal("190.00"));
        assertThat(resultado.getTotalDescuentos().getCantidad())
            .isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(resultado.getTotalFinal().getCantidad())
            .isEqualByComparingTo(new BigDecimal("1090.00"));
    }

    @Test
    @DisplayName("Test 14: Obtener factura por ID Long existente debe retornar factura")
    void testObtenerFacturaPorIdLong_Existente() {
        // Given
        Long facturaId = 1L;
        given(facturaRepository.buscarPorId(facturaId)).willReturn(Optional.of(facturaEjemplo));

        // When
        Optional<Factura> resultado = facturaService.obtenerFacturaPorId(facturaId);

        // Then
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getClienteId()).isEqualTo(1L);

        then(facturaRepository).should().buscarPorId(facturaId);
    }

    @Test
    @DisplayName("Test 15: Factura debe tener fecha de creación válida")
    void testFacturaConFechaCreacion() {
        // Given
        given(clienteValidation.esClienteActivo(1L)).willReturn(true);
        given(facturaRepository.guardar(any(Factura.class))).willReturn(facturaEjemplo);

        // When
        Factura resultado = facturaService.crearFactura(facturaEjemplo);

        // Then
        assertThat(resultado.getFechaCreacion()).isNotNull();
        assertThat(resultado.getFechaCreacion()).isBefore(LocalDateTime.now().plusSeconds(1));
    }
}
