package com.castor.clientes.infrastructure.adapter.out.persistence;

import com.castor.clientes.domain.Cliente;
import com.castor.clientes.domain.valueobject.ClienteId;
import com.castor.clientes.domain.valueobject.Email;
import com.castor.clientes.domain.valueobject.Nit;
import com.castor.clientes.domain.valueobject.NombreCliente;
import com.castor.clientes.infrastructure.adapter.out.persistence.entity.ClienteEntity;
import com.castor.clientes.infrastructure.adapter.out.persistence.mapper.ClienteMapper;
import com.castor.clientes.infrastructure.adapter.out.persistence.repository.ClienteJpaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests de integracion para ClienteRepositoryAdapter
 * Utiliza TestContainers con PostgreSQL para pruebas realistas
 * Patrón AAA: Arrange-Act-Assert
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = "classpath:application-test.yml")
@Import({ClienteRepositoryAdapter.class, ClienteMapper.class})
@DisplayName("Tests Integracion - ClienteRepositoryAdapter con TestContainers")
class ClienteRepositoryAdapterTest {

    /**
     * Contenedor de PostgreSQL 15 para tests de integracion
     * Se inicia automaticamente y se destruye al finalizar los tests
     */
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @Autowired
    private ClienteJpaRepository jpaRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ClienteRepositoryAdapter repositoryAdapter;

    @Autowired
    private ClienteMapper mapper;

    private Cliente clienteValido;
    private Nit nitValido;
    private NombreCliente nombreValido;
    private Email emailValido;

    @BeforeEach
    void setUp() {
        // Arrange: Limpiar base de datos antes de cada test
        jpaRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();

        // Preparar datos de prueba comunes
        nitValido = Nit.of("900123456-7");
        nombreValido = NombreCliente.of("Acme Corporation S.A.");
        emailValido = Email.of("contacto@acme.com");

        clienteValido = Cliente.crear(
            nombreValido,
            nitValido,
            emailValido,
            "+57 310 1234567",
            "Calle 123 #45-67, Bogota"
        );
    }

    @Test
    @DisplayName("Debe guardar cliente exitosamente en la base de datos")
    void testGuardarCliente() {
        // Act: Guardar el cliente
        Cliente clienteGuardado = repositoryAdapter.guardar(clienteValido);

        // Assert: Verificar que se guardo correctamente
        assertThat(clienteGuardado).isNotNull();
        assertThat(clienteGuardado.getId()).isNotNull();
        assertThat(clienteGuardado.getId().getValor()).isPositive();
        assertThat(clienteGuardado.getNombre()).isEqualTo(nombreValido);
        assertThat(clienteGuardado.getNit()).isEqualTo(nitValido);
        assertThat(clienteGuardado.getEmail()).isEqualTo(emailValido);
        assertThat(clienteGuardado.isActivo()).isTrue();
        assertThat(clienteGuardado.getFechaCreacion()).isNotNull();
        assertThat(clienteGuardado.getFechaActualizacion()).isNotNull();

        // Verificar que realmente esta en la base de datos
        entityManager.flush();
        entityManager.clear();

        Optional<ClienteEntity> entityEnBD = jpaRepository.findById(clienteGuardado.getId().getValor());
        assertThat(entityEnBD).isPresent();
        assertThat(entityEnBD.get().getNombre()).isEqualTo("Acme Corporation S.A.");
    }

    @Test
    @DisplayName("Debe buscar cliente por ID exitosamente")
    void testBuscarPorId() {
        // Arrange: Guardar un cliente en la base de datos
        ClienteEntity entity = ClienteEntity.builder()
            .nombre("Test Cliente")
            .nit("900555555-5")
            .email("test@test.com")
            .telefono("3001234567")
            .direccion("Direccion Test")
            .activo(true)
            .fechaCreacion(LocalDateTime.now())
            .fechaActualizacion(LocalDateTime.now())
            .build();

        ClienteEntity entityGuardada = jpaRepository.save(entity);
        entityManager.flush();
        entityManager.clear();

        ClienteId clienteId = ClienteId.of(entityGuardada.getId());

        // Act: Buscar por ID
        Optional<Cliente> clienteEncontrado = repositoryAdapter.buscarPorId(clienteId);

        // Assert: Verificar que se encontro
        assertThat(clienteEncontrado).isPresent();
        assertThat(clienteEncontrado.get().getId().getValor()).isEqualTo(entityGuardada.getId());
        assertThat(clienteEncontrado.get().getNombre().getValor()).isEqualTo("Test Cliente");
        assertThat(clienteEncontrado.get().getNit().getValor()).isEqualTo("900555555-5");
    }

    @Test
    @DisplayName("Debe retornar Optional.empty cuando el cliente no existe")
    void testBuscarPorId_NoExistente() {
        // Arrange: ID de cliente inexistente
        ClienteId clienteIdInexistente = ClienteId.of(99999L);

        // Act: Buscar por ID inexistente
        Optional<Cliente> clienteEncontrado = repositoryAdapter.buscarPorId(clienteIdInexistente);

        // Assert: Verificar que no se encontro
        assertThat(clienteEncontrado).isEmpty();
    }

    @Test
    @DisplayName("Debe buscar cliente por NIT exitosamente")
    void testBuscarPorNit() {
        // Arrange: Guardar cliente con NIT especifico
        Cliente clienteGuardado = repositoryAdapter.guardar(clienteValido);
        entityManager.flush();
        entityManager.clear();

        // Act: Buscar por NIT
        Optional<Cliente> clienteEncontrado = repositoryAdapter.buscarPorNit(nitValido);

        // Assert: Verificar que se encontro
        assertThat(clienteEncontrado).isPresent();
        assertThat(clienteEncontrado.get().getNit()).isEqualTo(nitValido);
        assertThat(clienteEncontrado.get().getNombre()).isEqualTo(nombreValido);
    }

    @Test
    @DisplayName("Debe verificar existencia de cliente por NIT")
    void testExistsByNit() {
        // Arrange: Guardar cliente
        repositoryAdapter.guardar(clienteValido);
        entityManager.flush();

        // Act & Assert: Verificar que existe
        boolean existe = repositoryAdapter.existePorNit(nitValido);
        assertThat(existe).isTrue();

        // Verificar que NIT inexistente no existe
        Nit nitInexistente = Nit.of("900999999-9");
        boolean noExiste = repositoryAdapter.existePorNit(nitInexistente);
        assertThat(noExiste).isFalse();
    }

    @Test
    @DisplayName("Debe actualizar cliente existente correctamente")
    void testActualizarCliente() {
        // Arrange: Guardar cliente inicial
        Cliente clienteGuardado = repositoryAdapter.guardar(clienteValido);
        entityManager.flush();
        entityManager.clear();

        // Modificar el cliente usando metodos de dominio
        NombreCliente nuevoNombre = NombreCliente.of("Nueva Razon Social");
        Email nuevoEmail = Email.of("nuevo@email.com");
        clienteGuardado.actualizar(
            nuevoNombre,
            nuevoEmail,
            "+57 320 9876543",
            "Nueva Direccion 456"
        );

        // Act: Actualizar el cliente
        Cliente clienteActualizado = repositoryAdapter.guardar(clienteGuardado);
        entityManager.flush();
        entityManager.clear();

        // Assert: Verificar actualizacion
        assertThat(clienteActualizado.getNombre()).isEqualTo(nuevoNombre);
        assertThat(clienteActualizado.getEmail()).isEqualTo(nuevoEmail);
        assertThat(clienteActualizado.getTelefono()).isEqualTo("+57 320 9876543");
        assertThat(clienteActualizado.getDireccion()).isEqualTo("Nueva Direccion 456");
        assertThat(clienteActualizado.getFechaActualizacion()).isAfter(clienteActualizado.getFechaCreacion());

        // Verificar en la base de datos
        Optional<ClienteEntity> entityActualizada = jpaRepository.findById(clienteActualizado.getId().getValor());
        assertThat(entityActualizada).isPresent();
        assertThat(entityActualizada.get().getNombre()).isEqualTo("Nueva Razon Social");
        assertThat(entityActualizada.get().getEmail()).isEqualTo("nuevo@email.com");
    }

    @Test
    @DisplayName("Debe eliminar cliente exitosamente")
    void testEliminarCliente() {
        // Arrange: Guardar cliente
        Cliente clienteGuardado = repositoryAdapter.guardar(clienteValido);
        entityManager.flush();
        entityManager.clear();

        ClienteId clienteId = clienteGuardado.getId();
        assertThat(jpaRepository.existsById(clienteId.getValor())).isTrue();

        // Act: Eliminar el cliente
        repositoryAdapter.eliminar(clienteId);
        entityManager.flush();

        // Assert: Verificar eliminacion
        boolean existe = jpaRepository.existsById(clienteId.getValor());
        assertThat(existe).isFalse();

        Optional<Cliente> clienteEliminado = repositoryAdapter.buscarPorId(clienteId);
        assertThat(clienteEliminado).isEmpty();
    }

    @Test
    @DisplayName("Debe listar todos los clientes con paginacion")
    void testListarTodos_ConPaginacion() {
        // Arrange: Guardar multiples clientes
        Cliente cliente1 = Cliente.crear(
            NombreCliente.of("Cliente Uno"),
            Nit.of("900111111-1"),
            Email.of("uno@test.com"),
            "111111111",
            "Direccion 1"
        );

        Cliente cliente2 = Cliente.crear(
            NombreCliente.of("Cliente Dos"),
            Nit.of("900222222-2"),
            Email.of("dos@test.com"),
            "222222222",
            "Direccion 2"
        );

        Cliente cliente3 = Cliente.crear(
            NombreCliente.of("Cliente Tres"),
            Nit.of("900333333-3"),
            Email.of("tres@test.com"),
            "333333333",
            "Direccion 3"
        );

        repositoryAdapter.guardar(cliente1);
        repositoryAdapter.guardar(cliente2);
        repositoryAdapter.guardar(cliente3);
        entityManager.flush();
        entityManager.clear();

        // Act: Obtener primera pagina con 2 elementos
        Pageable pageable = PageRequest.of(0, 2);
        Page<Cliente> primeraPage = repositoryAdapter.buscarTodos(pageable);

        // Assert: Verificar paginacion
        assertThat(primeraPage).isNotNull();
        assertThat(primeraPage.getContent()).hasSize(2);
        assertThat(primeraPage.getTotalElements()).isEqualTo(3);
        assertThat(primeraPage.getTotalPages()).isEqualTo(2);
        assertThat(primeraPage.hasNext()).isTrue();

        // Act: Obtener segunda pagina
        Pageable pageableSegundo = PageRequest.of(1, 2);
        Page<Cliente> segundaPage = repositoryAdapter.buscarTodos(pageableSegundo);

        // Assert: Verificar segunda pagina
        assertThat(segundaPage.getContent()).hasSize(1);
        assertThat(segundaPage.hasNext()).isFalse();
    }

    @Test
    @DisplayName("Debe retornar pagina vacia cuando no hay clientes")
    void testListarTodos_PaginaVacia() {
        // Act: Buscar clientes en base de datos vacia
        Pageable pageable = PageRequest.of(0, 10);
        Page<Cliente> clientesPage = repositoryAdapter.buscarTodos(pageable);

        // Assert: Verificar que la pagina esta vacia
        assertThat(clientesPage).isNotNull();
        assertThat(clientesPage.getContent()).isEmpty();
        assertThat(clientesPage.getTotalElements()).isZero();
        assertThat(clientesPage.getTotalPages()).isZero();
    }

    @Test
    @DisplayName("Debe manejar correctamente ClienteId nulo al buscar")
    void testBuscarPorId_IdNulo() {
        // Act: Buscar con ID nulo
        Optional<Cliente> resultado = repositoryAdapter.buscarPorId(null);

        // Assert: Debe retornar Optional.empty()
        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Debe manejar correctamente ClienteId nuevo al buscar")
    void testBuscarPorId_IdNuevo() {
        // Arrange: ClienteId sin valor (nuevo)
        ClienteId clienteIdNuevo = ClienteId.generate();

        // Act: Buscar con ID nuevo
        Optional<Cliente> resultado = repositoryAdapter.buscarPorId(clienteIdNuevo);

        // Assert: Debe retornar Optional.empty()
        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Debe manejar correctamente Nit nulo al buscar")
    void testBuscarPorNit_NitNulo() {
        // Act: Buscar con NIT nulo
        Optional<Cliente> resultado = repositoryAdapter.buscarPorNit(null);

        // Assert: Debe retornar Optional.empty()
        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Debe manejar correctamente Nit nulo al verificar existencia")
    void testExistsByNit_NitNulo() {
        // Act: Verificar existencia con NIT nulo
        boolean existe = repositoryAdapter.existePorNit(null);

        // Assert: Debe retornar false
        assertThat(existe).isFalse();
    }

    @Test
    @DisplayName("No debe eliminar cuando ClienteId es nulo")
    void testEliminar_IdNulo() {
        // Arrange: Guardar un cliente
        Cliente clienteGuardado = repositoryAdapter.guardar(clienteValido);
        entityManager.flush();
        long countInicial = jpaRepository.count();

        // Act: Intentar eliminar con ID nulo (no debe hacer nada)
        repositoryAdapter.eliminar(null);
        entityManager.flush();

        // Assert: El cliente debe seguir existiendo
        long countFinal = jpaRepository.count();
        assertThat(countFinal).isEqualTo(countInicial);
        assertThat(jpaRepository.existsById(clienteGuardado.getId().getValor())).isTrue();
    }

    @Test
    @DisplayName("No debe eliminar cuando ClienteId es nuevo")
    void testEliminar_IdNuevo() {
        // Arrange: Guardar un cliente
        Cliente clienteGuardado = repositoryAdapter.guardar(clienteValido);
        entityManager.flush();
        long countInicial = jpaRepository.count();

        ClienteId clienteIdNuevo = ClienteId.generate();

        // Act: Intentar eliminar con ID nuevo (no debe hacer nada)
        repositoryAdapter.eliminar(clienteIdNuevo);
        entityManager.flush();

        // Assert: El cliente debe seguir existiendo
        long countFinal = jpaRepository.count();
        assertThat(countFinal).isEqualTo(countInicial);
        assertThat(jpaRepository.existsById(clienteGuardado.getId().getValor())).isTrue();
    }

    @Test
    @DisplayName("Debe mantener constraint de unicidad en NIT")
    void testUnicidadNit() {
        // Arrange: Guardar primer cliente
        Cliente primerCliente = repositoryAdapter.guardar(clienteValido);
        entityManager.flush();
        entityManager.clear();

        // Crear segundo cliente con mismo NIT
        Cliente segundoCliente = Cliente.crear(
            NombreCliente.of("Otro Cliente"),
            nitValido, // Mismo NIT
            Email.of("otro@test.com"),
            "9999999",
            "Otra direccion"
        );

        // Act & Assert: Debe lanzar excepcion por NIT duplicado
        assertThatThrownBy(() -> {
            repositoryAdapter.guardar(segundoCliente);
            entityManager.flush();
        }).isInstanceOf(Exception.class); // DataIntegrityViolationException o similar
    }

    @Test
    @DisplayName("Debe persistir y recuperar fechas correctamente")
    void testPersistenciaFechas() {
        // Arrange & Act: Guardar cliente
        LocalDateTime antesDeGuardar = LocalDateTime.now().minusSeconds(1);
        Cliente clienteGuardado = repositoryAdapter.guardar(clienteValido);
        LocalDateTime despuesDeGuardar = LocalDateTime.now().plusSeconds(1);
        entityManager.flush();
        entityManager.clear();

        // Recuperar cliente
        Optional<Cliente> clienteRecuperado = repositoryAdapter.buscarPorId(clienteGuardado.getId());

        // Assert: Verificar fechas
        assertThat(clienteRecuperado).isPresent();
        assertThat(clienteRecuperado.get().getFechaCreacion())
            .isAfter(antesDeGuardar)
            .isBefore(despuesDeGuardar);
        assertThat(clienteRecuperado.get().getFechaActualizacion())
            .isAfter(antesDeGuardar)
            .isBefore(despuesDeGuardar);
        assertThat(clienteRecuperado.get().getFechaCreacion())
            .isEqualTo(clienteRecuperado.get().getFechaActualizacion());
    }
}
