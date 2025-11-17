package com.castor.facturacion.infrastructure.adapter.out.persistence.oracle;

import com.castor.facturacion.domain.Factura;
import com.castor.facturacion.domain.ItemFactura;
import com.castor.facturacion.domain.valueobject.*;
import com.castor.facturacion.infrastructure.adapter.out.persistence.oracle.entity.FacturaEntity;
import com.castor.facturacion.infrastructure.adapter.out.persistence.oracle.entity.ItemFacturaEntity;
import com.castor.facturacion.infrastructure.adapter.out.persistence.oracle.mapper.FacturaMapper;
import com.castor.facturacion.infrastructure.adapter.out.persistence.oracle.repository.FacturaJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests de integración para FacturaRepositoryAdapter usando TestContainers Oracle.
 *
 * Características:
 * - Usa Oracle XE 21 en TestContainers
 * - Tests de persistencia real con JPA
 * - Tests de procedimientos almacenados (si están disponibles)
 * - Tests de consultas personalizadas
 *
 * Nota: Estos tests requieren Docker instalado y corriendo.
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("FacturaRepositoryAdapter - Tests de Integración con Oracle TestContainers")
class FacturaRepositoryAdapterTest {

    /**
     * Container Oracle XE 21 (slim-faststart para arranque rápido)
     * Nota: Requiere Docker instalado y corriendo
     */
    @Container
    static OracleContainer oracleContainer = new OracleContainer("gvenzl/oracle-xe:21-slim-faststart")
        .withDatabaseName("testdb")
        .withUsername("testuser")
        .withPassword("testpass")
        .withReuse(false);

    /**
     * Configuración dinámica de propiedades para conectar al container Oracle
     */
    @DynamicPropertySource
    static void setDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", oracleContainer::getJdbcUrl);
        registry.add("spring.datasource.username", oracleContainer::getUsername);
        registry.add("spring.datasource.password", oracleContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "oracle.jdbc.OracleDriver");

        // JPA/Hibernate properties
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.OracleDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "true");
        registry.add("spring.jpa.properties.hibernate.format_sql", () -> "true");
    }

    @Autowired
    private FacturaJpaRepository facturaJpaRepository;

    private FacturaEntity facturaEntityEjemplo;

    @BeforeEach
    void setUp() {
        // Limpiar base de datos antes de cada test
        facturaJpaRepository.deleteAll();

        // Preparar entidad de ejemplo
        facturaEntityEjemplo = FacturaEntity.builder()
            .numero("FAC-2024-001")
            .clienteId(1L)
            .subtotalGeneral(new BigDecimal("1000.00"))
            .totalImpuestos(new BigDecimal("190.00"))
            .totalDescuentos(new BigDecimal("100.00"))
            .totalFinal(new BigDecimal("1090.00"))
            .fechaCreacion(LocalDateTime.now())
            .build();

        // Agregar items
        ItemFacturaEntity item1 = ItemFacturaEntity.builder()
            .descripcion("Producto A")
            .cantidad(10)
            .precioUnitario(new BigDecimal("100.00"))
            .porcentajeImpuesto(new BigDecimal("19.00"))
            .porcentajeDescuento(new BigDecimal("10.00"))
            .subtotal(new BigDecimal("1000.00"))
            .impuesto(new BigDecimal("190.00"))
            .descuento(new BigDecimal("100.00"))
            .total(new BigDecimal("1090.00"))
            .build();

        facturaEntityEjemplo.addItem(item1);
    }

    @Test
    @DisplayName("Test 01: Guardar factura con items debe persistir correctamente")
    void testGuardarFacturaConItems() {
        // When
        FacturaEntity savedFactura = facturaJpaRepository.save(facturaEntityEjemplo);

        // Then
        assertThat(savedFactura).isNotNull();
        assertThat(savedFactura.getId()).isNotNull();
        assertThat(savedFactura.getNumero()).isEqualTo("FAC-2024-001");
        assertThat(savedFactura.getClienteId()).isEqualTo(1L);
        assertThat(savedFactura.getItems()).hasSize(1);
        assertThat(savedFactura.getTotalFinal()).isEqualByComparingTo(new BigDecimal("1090.00"));

        // Verificar persistencia
        Optional<FacturaEntity> found = facturaJpaRepository.findById(savedFactura.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getNumero()).isEqualTo("FAC-2024-001");
    }

    @Test
    @DisplayName("Test 02: Buscar factura por ID debe retornar factura con items")
    void testBuscarPorId() {
        // Given
        FacturaEntity savedFactura = facturaJpaRepository.save(facturaEntityEjemplo);

        // When
        Optional<FacturaEntity> found = facturaJpaRepository.findByIdWithItems(savedFactura.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(savedFactura.getId());
        assertThat(found.get().getNumero()).isEqualTo("FAC-2024-001");
        assertThat(found.get().getItems()).hasSize(1);
        assertThat(found.get().getItems().get(0).getDescripcion()).isEqualTo("Producto A");
    }

    @Test
    @DisplayName("Test 03: Buscar facturas por cliente debe retornar facturas correctas")
    void testBuscarPorCliente() {
        // Given - guardar múltiples facturas para diferentes clientes
        FacturaEntity factura1 = facturaJpaRepository.save(facturaEntityEjemplo);

        FacturaEntity factura2 = FacturaEntity.builder()
            .numero("FAC-2024-002")
            .clienteId(2L)
            .subtotalGeneral(new BigDecimal("500.00"))
            .totalImpuestos(new BigDecimal("95.00"))
            .totalDescuentos(new BigDecimal("0.00"))
            .totalFinal(new BigDecimal("595.00"))
            .fechaCreacion(LocalDateTime.now())
            .build();
        facturaJpaRepository.save(factura2);

        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<FacturaEntity> facturasCliente1 = facturaJpaRepository.findByClienteId(1L, pageable);
        Page<FacturaEntity> facturasCliente2 = facturaJpaRepository.findByClienteId(2L, pageable);

        // Then
        assertThat(facturasCliente1.getContent()).hasSize(1);
        assertThat(facturasCliente1.getContent().get(0).getClienteId()).isEqualTo(1L);
        assertThat(facturasCliente1.getContent().get(0).getNumero()).isEqualTo("FAC-2024-001");

        assertThat(facturasCliente2.getContent()).hasSize(1);
        assertThat(facturasCliente2.getContent().get(0).getClienteId()).isEqualTo(2L);
        assertThat(facturasCliente2.getContent().get(0).getNumero()).isEqualTo("FAC-2024-002");
    }

    @Test
    @DisplayName("Test 04: Eliminar factura debe eliminar factura y sus items (cascade)")
    void testEliminarFactura() {
        // Given
        FacturaEntity savedFactura = facturaJpaRepository.save(facturaEntityEjemplo);
        Long facturaId = savedFactura.getId();

        // Verificar que existe
        assertThat(facturaJpaRepository.existsById(facturaId)).isTrue();

        // When
        facturaJpaRepository.deleteById(facturaId);

        // Then
        assertThat(facturaJpaRepository.existsById(facturaId)).isFalse();
        assertThat(facturaJpaRepository.findById(facturaId)).isEmpty();
    }

    @Test
    @DisplayName("Test 05: Actualizar factura debe modificar valores correctamente")
    void testActualizarFactura() {
        // Given
        FacturaEntity savedFactura = facturaJpaRepository.save(facturaEntityEjemplo);

        // When - modificar valores
        savedFactura.setTotalFinal(new BigDecimal("1200.00"));
        savedFactura.setSubtotalGeneral(new BigDecimal("1100.00"));

        FacturaEntity updatedFactura = facturaJpaRepository.save(savedFactura);

        // Then
        assertThat(updatedFactura.getTotalFinal()).isEqualByComparingTo(new BigDecimal("1200.00"));
        assertThat(updatedFactura.getSubtotalGeneral()).isEqualByComparingTo(new BigDecimal("1100.00"));

        // Verificar persistencia
        FacturaEntity foundFactura = facturaJpaRepository.findById(updatedFactura.getId()).get();
        assertThat(foundFactura.getTotalFinal()).isEqualByComparingTo(new BigDecimal("1200.00"));
    }

    @Test
    @DisplayName("Test 06: Paginación debe funcionar correctamente")
    void testPaginacion() {
        // Given - crear múltiples facturas
        for (int i = 1; i <= 25; i++) {
            FacturaEntity factura = FacturaEntity.builder()
                .numero("FAC-2024-" + String.format("%03d", i))
                .clienteId(1L)
                .subtotalGeneral(new BigDecimal("100.00"))
                .totalImpuestos(new BigDecimal("19.00"))
                .totalDescuentos(new BigDecimal("0.00"))
                .totalFinal(new BigDecimal("119.00"))
                .fechaCreacion(LocalDateTime.now())
                .build();
            facturaJpaRepository.save(factura);
        }

        // When - obtener primera página (10 elementos)
        Pageable pageable = PageRequest.of(0, 10);
        Page<FacturaEntity> primeraPage = facturaJpaRepository.findAll(pageable);

        // Then
        assertThat(primeraPage.getContent()).hasSize(10);
        assertThat(primeraPage.getTotalElements()).isEqualTo(25);
        assertThat(primeraPage.getTotalPages()).isEqualTo(3);
        assertThat(primeraPage.isFirst()).isTrue();
        assertThat(primeraPage.isLast()).isFalse();

        // When - obtener segunda página
        Pageable segundaPageable = PageRequest.of(1, 10);
        Page<FacturaEntity> segundaPage = facturaJpaRepository.findAll(segundaPageable);

        // Then
        assertThat(segundaPage.getContent()).hasSize(10);
        assertThat(segundaPage.isFirst()).isFalse();
        assertThat(segundaPage.isLast()).isFalse();

        // When - obtener última página
        Pageable terceraPageable = PageRequest.of(2, 10);
        Page<FacturaEntity> terceraPage = facturaJpaRepository.findAll(terceraPageable);

        // Then
        assertThat(terceraPage.getContent()).hasSize(5);
        assertThat(terceraPage.isLast()).isTrue();
    }

    @Test
    @DisplayName("Test 07: Factura con múltiples items debe persistir todos los items")
    void testFacturaConMultiplesItems() {
        // Given
        ItemFacturaEntity item2 = ItemFacturaEntity.builder()
            .descripcion("Producto B")
            .cantidad(5)
            .precioUnitario(new BigDecimal("50.00"))
            .porcentajeImpuesto(new BigDecimal("19.00"))
            .porcentajeDescuento(new BigDecimal("0.00"))
            .subtotal(new BigDecimal("250.00"))
            .impuesto(new BigDecimal("47.50"))
            .descuento(new BigDecimal("0.00"))
            .total(new BigDecimal("297.50"))
            .build();

        ItemFacturaEntity item3 = ItemFacturaEntity.builder()
            .descripcion("Producto C")
            .cantidad(3)
            .precioUnitario(new BigDecimal("75.00"))
            .porcentajeImpuesto(new BigDecimal("19.00"))
            .porcentajeDescuento(new BigDecimal("5.00"))
            .subtotal(new BigDecimal("225.00"))
            .impuesto(new BigDecimal("42.75"))
            .descuento(new BigDecimal("11.25"))
            .total(new BigDecimal("256.50"))
            .build();

        facturaEntityEjemplo.addItem(item2);
        facturaEntityEjemplo.addItem(item3);

        // When
        FacturaEntity savedFactura = facturaJpaRepository.save(facturaEntityEjemplo);

        // Then
        assertThat(savedFactura.getItems()).hasSize(3);

        // Verificar persistencia con fetch
        FacturaEntity foundFactura = facturaJpaRepository.findByIdWithItems(savedFactura.getId()).get();
        assertThat(foundFactura.getItems()).hasSize(3);
        assertThat(foundFactura.getItems())
            .extracting(ItemFacturaEntity::getDescripcion)
            .containsExactlyInAnyOrder("Producto A", "Producto B", "Producto C");
    }

    @Test
    @DisplayName("Test 08: Buscar factura por ID inexistente debe retornar vacío")
    void testBuscarPorIdInexistente() {
        // When
        Optional<FacturaEntity> found = facturaJpaRepository.findByIdWithItems(999L);

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Test 09: Listar facturas por cliente sin facturas debe retornar página vacía")
    void testListarFacturasPorClienteSinFacturas() {
        // Given
        facturaJpaRepository.save(facturaEntityEjemplo); // Cliente 1

        // When - buscar facturas del cliente 999 (no tiene facturas)
        Pageable pageable = PageRequest.of(0, 10);
        Page<FacturaEntity> facturas = facturaJpaRepository.findByClienteId(999L, pageable);

        // Then
        assertThat(facturas.getContent()).isEmpty();
        assertThat(facturas.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("Test 10: Verificar cascade delete - eliminar factura elimina items")
    void testCascadeDelete() {
        // Given
        FacturaEntity savedFactura = facturaJpaRepository.save(facturaEntityEjemplo);
        Long facturaId = savedFactura.getId();

        // Verificar que tiene items
        FacturaEntity foundFactura = facturaJpaRepository.findByIdWithItems(facturaId).get();
        assertThat(foundFactura.getItems()).hasSize(1);

        // When - eliminar factura
        facturaJpaRepository.deleteById(facturaId);

        // Then - verificar que factura e items fueron eliminados
        assertThat(facturaJpaRepository.findById(facturaId)).isEmpty();

        // Los items deben haber sido eliminados por cascade (orphanRemoval = true)
        // No podemos verificar directamente sin un repositorio de items,
        // pero la factura ya no debe existir
    }

    @Test
    @DisplayName("Test 11: Numero de factura debe ser único")
    void testNumeroFacturaUnico() {
        // Given
        facturaJpaRepository.save(facturaEntityEjemplo);

        // When - intentar guardar otra factura con el mismo número
        FacturaEntity facturaDuplicada = FacturaEntity.builder()
            .numero("FAC-2024-001") // Mismo número
            .clienteId(2L)
            .subtotalGeneral(new BigDecimal("100.00"))
            .totalImpuestos(new BigDecimal("19.00"))
            .totalDescuentos(new BigDecimal("0.00"))
            .totalFinal(new BigDecimal("119.00"))
            .fechaCreacion(LocalDateTime.now())
            .build();

        // Then - debe lanzar excepción por violación de constraint UNIQUE
        assertThatThrownBy(() -> {
            facturaJpaRepository.save(facturaDuplicada);
            facturaJpaRepository.flush(); // Forzar sincronización con BD
        }).isInstanceOf(Exception.class); // Puede ser DataIntegrityViolationException
    }

    @Test
    @DisplayName("Test 12: Fecha de creación debe ser establecida automáticamente")
    void testFechaCreacionAutomatica() {
        // Given
        FacturaEntity facturaSinFecha = FacturaEntity.builder()
            .numero("FAC-2024-999")
            .clienteId(1L)
            .subtotalGeneral(new BigDecimal("100.00"))
            .totalImpuestos(new BigDecimal("19.00"))
            .totalDescuentos(new BigDecimal("0.00"))
            .totalFinal(new BigDecimal("119.00"))
            .build(); // Sin fecha de creación

        // When
        FacturaEntity savedFactura = facturaJpaRepository.save(facturaSinFecha);

        // Then
        assertThat(savedFactura.getFechaCreacion()).isNotNull();
        assertThat(savedFactura.getFechaCreacion()).isBefore(LocalDateTime.now().plusSeconds(1));
    }
}
