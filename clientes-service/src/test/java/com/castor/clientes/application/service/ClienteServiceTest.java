package com.castor.clientes.application.service;

import com.castor.clientes.domain.Cliente;
import com.castor.clientes.domain.port.out.ClienteRepositoryPort;
import com.castor.clientes.domain.valueobject.ClienteId;
import com.castor.clientes.domain.valueobject.Email;
import com.castor.clientes.domain.valueobject.Nit;
import com.castor.clientes.domain.valueobject.NombreCliente;
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

import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

/**
 * Tests unitarios para ClienteService
 * Utiliza Mockito para simular dependencias y AssertJ para assertions fluidas
 * Patrón AAA: Arrange-Act-Assert
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests Unitarios - ClienteService")
class ClienteServiceTest {

    @Mock
    private ClienteRepositoryPort clienteRepository;

    @InjectMocks
    private ClienteService clienteService;

    private Cliente clienteValido;
    private ClienteId clienteIdExistente;
    private Nit nitValido;
    private NombreCliente nombreValido;
    private Email emailValido;

    @BeforeEach
    void setUp() {
        // Arrange: Preparar datos de prueba comunes
        clienteIdExistente = ClienteId.of(1L);
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
    @DisplayName("Debe crear cliente exitosamente cuando los datos son validos")
    void testCrearCliente_Exitoso() {
        // Arrange: Configurar mocks para el caso exitoso
        given(clienteRepository.existePorNit(nitValido)).willReturn(false);

        Cliente clienteConId = Cliente.reconstituir(
            clienteIdExistente,
            nombreValido,
            nitValido,
            emailValido,
            "+57 310 1234567",
            "Calle 123 #45-67, Bogota",
            true,
            clienteValido.getFechaCreacion(),
            clienteValido.getFechaActualizacion()
        );
        given(clienteRepository.guardar(any(Cliente.class))).willReturn(clienteConId);

        // Act: Ejecutar el metodo bajo prueba
        Cliente resultado = clienteService.crearCliente(clienteValido);

        // Assert: Verificar el comportamiento esperado
        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(clienteIdExistente);
        assertThat(resultado.getNombre()).isEqualTo(nombreValido);
        assertThat(resultado.getNit()).isEqualTo(nitValido);
        assertThat(resultado.getEmail()).isEqualTo(emailValido);
        assertThat(resultado.isActivo()).isTrue();

        // Verificar interacciones con el repositorio
        then(clienteRepository).should().existePorNit(nitValido);
        then(clienteRepository).should().guardar(clienteValido);
        then(clienteRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("Debe lanzar excepcion cuando se intenta crear cliente con NIT duplicado")
    void testCrearCliente_NitDuplicado_LanzaExcepcion() {
        // Arrange: Simular que el NIT ya existe en el sistema
        given(clienteRepository.existePorNit(nitValido)).willReturn(true);

        // Act & Assert: Verificar que se lanza la excepcion esperada
        assertThatThrownBy(() -> clienteService.crearCliente(clienteValido))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Ya existe un cliente con el NIT");

        // Verificar que NO se intento guardar el cliente
        then(clienteRepository).should().existePorNit(nitValido);
        then(clienteRepository).should(never()).guardar(any(Cliente.class));
    }

    @Test
    @DisplayName("Debe obtener cliente exitosamente cuando existe")
    void testObtenerClientePorId_Existente() {
        // Arrange: Preparar cliente existente
        Cliente clienteExistente = Cliente.reconstituir(
            clienteIdExistente,
            nombreValido,
            nitValido,
            emailValido,
            "+57 310 1234567",
            "Calle 123 #45-67, Bogota",
            true,
            clienteValido.getFechaCreacion(),
            clienteValido.getFechaActualizacion()
        );
        given(clienteRepository.buscarPorId(clienteIdExistente)).willReturn(Optional.of(clienteExistente));

        // Act: Buscar el cliente
        Cliente resultado = clienteService.obtenerCliente(1L);

        // Assert: Verificar el resultado
        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(clienteIdExistente);
        assertThat(resultado.getNombre()).isEqualTo(nombreValido);
        assertThat(resultado.getNit()).isEqualTo(nitValido);

        // Verificar interacciones
        then(clienteRepository).should().buscarPorId(clienteIdExistente);
    }

    @Test
    @DisplayName("Debe lanzar excepcion cuando el cliente no existe")
    void testObtenerClientePorId_NoExistente() {
        // Arrange: Simular que el cliente no existe
        Long idInexistente = 999L;
        ClienteId clienteIdInexistente = ClienteId.of(idInexistente);
        given(clienteRepository.buscarPorId(clienteIdInexistente)).willReturn(Optional.empty());

        // Act & Assert: Verificar excepcion
        assertThatThrownBy(() -> clienteService.obtenerCliente(idInexistente))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cliente no encontrado con ID: " + idInexistente);

        // Verificar interacciones
        then(clienteRepository).should().buscarPorId(clienteIdInexistente);
    }

    @Test
    @DisplayName("Debe actualizar cliente exitosamente cuando existe")
    void testActualizarCliente_Exitoso() {
        // Arrange: Cliente existente y datos actualizados
        Cliente clienteExistente = Cliente.reconstituir(
            clienteIdExistente,
            nombreValido,
            nitValido,
            emailValido,
            "+57 310 1234567",
            "Calle 123 #45-67, Bogota",
            true,
            clienteValido.getFechaCreacion(),
            clienteValido.getFechaActualizacion()
        );

        NombreCliente nuevoNombre = NombreCliente.of("Nueva Razon Social S.A.");
        Email nuevoEmail = Email.of("nuevo@email.com");
        Cliente clienteConCambios = Cliente.crear(
            nuevoNombre,
            nitValido, // Mismo NIT
            nuevoEmail,
            "+57 320 9876543",
            "Nueva Direccion 456"
        );

        given(clienteRepository.buscarPorId(clienteIdExistente)).willReturn(Optional.of(clienteExistente));
        given(clienteRepository.guardar(any(Cliente.class))).willReturn(clienteExistente);

        // Act: Actualizar el cliente
        Cliente resultado = clienteService.actualizarCliente(1L, clienteConCambios);

        // Assert: Verificar la actualizacion
        assertThat(resultado).isNotNull();
        assertThat(resultado.getNombre()).isEqualTo(nuevoNombre);
        assertThat(resultado.getEmail()).isEqualTo(nuevoEmail);

        // Verificar interacciones
        then(clienteRepository).should().buscarPorId(clienteIdExistente);
        then(clienteRepository).should().guardar(any(Cliente.class));
    }

    @Test
    @DisplayName("Debe desactivar cliente exitosamente")
    void testDesactivarCliente_Exitoso() {
        // Arrange: Cliente activo
        Cliente clienteActivo = Cliente.reconstituir(
            clienteIdExistente,
            nombreValido,
            nitValido,
            emailValido,
            "+57 310 1234567",
            "Calle 123 #45-67, Bogota",
            true, // Activo
            clienteValido.getFechaCreacion(),
            clienteValido.getFechaActualizacion()
        );

        Cliente clienteParaDesactivar = Cliente.reconstituir(
            ClienteId.generate(),
            nombreValido,
            nitValido,
            emailValido,
            "+57 310 1234567",
            "Calle 123 #45-67, Bogota",
            false, // Inactivo
            clienteValido.getFechaCreacion(),
            clienteValido.getFechaActualizacion()
        );

        given(clienteRepository.buscarPorId(clienteIdExistente)).willReturn(Optional.of(clienteActivo));
        given(clienteRepository.guardar(any(Cliente.class))).willReturn(clienteActivo);

        // Act: Desactivar el cliente
        Cliente resultado = clienteService.actualizarCliente(1L, clienteParaDesactivar);

        // Assert: Verificar que el cliente fue desactivado
        assertThat(resultado).isNotNull();
        assertThat(resultado.isActivo()).isFalse();

        // Verificar interacciones
        then(clienteRepository).should().buscarPorId(clienteIdExistente);
        then(clienteRepository).should().guardar(any(Cliente.class));
    }

    @Test
    @DisplayName("Debe activar cliente exitosamente")
    void testActivarCliente_Exitoso() {
        // Arrange: Cliente inactivo
        Cliente clienteInactivo = Cliente.reconstituir(
            clienteIdExistente,
            nombreValido,
            nitValido,
            emailValido,
            "+57 310 1234567",
            "Calle 123 #45-67, Bogota",
            false, // Inactivo
            clienteValido.getFechaCreacion(),
            clienteValido.getFechaActualizacion()
        );

        Cliente clienteParaActivar = Cliente.reconstituir(
            ClienteId.generate(),
            nombreValido,
            nitValido,
            emailValido,
            "+57 310 1234567",
            "Calle 123 #45-67, Bogota",
            true, // Activo
            clienteValido.getFechaCreacion(),
            clienteValido.getFechaActualizacion()
        );

        given(clienteRepository.buscarPorId(clienteIdExistente)).willReturn(Optional.of(clienteInactivo));
        given(clienteRepository.guardar(any(Cliente.class))).willReturn(clienteInactivo);

        // Act: Activar el cliente
        Cliente resultado = clienteService.actualizarCliente(1L, clienteParaActivar);

        // Assert: Verificar que el cliente fue activado
        assertThat(resultado).isNotNull();
        assertThat(resultado.isActivo()).isTrue();

        // Verificar interacciones
        then(clienteRepository).should().buscarPorId(clienteIdExistente);
        then(clienteRepository).should().guardar(any(Cliente.class));
    }

    @Test
    @DisplayName("Debe listar clientes con paginacion exitosamente")
    void testListarClientes_ConPaginacion() {
        // Arrange: Crear una pagina de clientes
        Cliente cliente1 = Cliente.reconstituir(
            ClienteId.of(1L),
            NombreCliente.of("Cliente Uno"),
            Nit.of("900111111-1"),
            Email.of("uno@test.com"),
            "111111111",
            "Direccion 1",
            true,
            clienteValido.getFechaCreacion(),
            clienteValido.getFechaActualizacion()
        );

        Cliente cliente2 = Cliente.reconstituir(
            ClienteId.of(2L),
            NombreCliente.of("Cliente Dos"),
            Nit.of("900222222-2"),
            Email.of("dos@test.com"),
            "222222222",
            "Direccion 2",
            true,
            clienteValido.getFechaCreacion(),
            clienteValido.getFechaActualizacion()
        );

        Pageable pageable = PageRequest.of(0, 10);
        Page<Cliente> clientesPage = new PageImpl<>(Arrays.asList(cliente1, cliente2), pageable, 2);

        given(clienteRepository.buscarTodos(pageable)).willReturn(clientesPage);

        // Act: Obtener la pagina de clientes
        Page<Cliente> resultado = clienteService.obtenerClientes(pageable);

        // Assert: Verificar la paginacion
        assertThat(resultado).isNotNull();
        assertThat(resultado.getContent()).hasSize(2);
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        assertThat(resultado.getContent()).containsExactly(cliente1, cliente2);

        // Verificar interacciones
        then(clienteRepository).should().buscarTodos(pageable);
    }

    @Test
    @DisplayName("Debe buscar cliente por NIT exitosamente cuando existe")
    void testBuscarPorNit_Existente() {
        // Arrange: Cliente con NIT especifico
        Cliente clienteExistente = Cliente.reconstituir(
            clienteIdExistente,
            nombreValido,
            nitValido,
            emailValido,
            "+57 310 1234567",
            "Calle 123 #45-67, Bogota",
            true,
            clienteValido.getFechaCreacion(),
            clienteValido.getFechaActualizacion()
        );

        given(clienteRepository.existePorNit(nitValido)).willReturn(true);

        // Act: Buscar por NIT
        boolean existe = clienteService.existePorNit(nitValido.getValor());

        // Assert: Verificar que existe
        assertThat(existe).isTrue();

        // Verificar interacciones
        then(clienteRepository).should().existePorNit(nitValido);
    }

    @Test
    @DisplayName("Debe eliminar cliente exitosamente cuando existe")
    void testEliminarCliente_Exitoso() {
        // Arrange: Cliente existente
        Cliente clienteExistente = Cliente.reconstituir(
            clienteIdExistente,
            nombreValido,
            nitValido,
            emailValido,
            "+57 310 1234567",
            "Calle 123 #45-67, Bogota",
            true,
            clienteValido.getFechaCreacion(),
            clienteValido.getFechaActualizacion()
        );

        given(clienteRepository.buscarPorId(clienteIdExistente)).willReturn(Optional.of(clienteExistente));
        willDoNothing().given(clienteRepository).eliminar(clienteIdExistente);

        // Act: Eliminar el cliente
        clienteService.eliminarCliente(1L);

        // Assert: Verificar que se elimino correctamente
        then(clienteRepository).should().buscarPorId(clienteIdExistente);
        then(clienteRepository).should().eliminar(clienteIdExistente);
    }

    @Test
    @DisplayName("Debe lanzar excepcion al eliminar cliente inexistente")
    void testEliminarCliente_NoExistente_LanzaExcepcion() {
        // Arrange: Cliente no existe
        Long idInexistente = 999L;
        ClienteId clienteIdInexistente = ClienteId.of(idInexistente);
        given(clienteRepository.buscarPorId(clienteIdInexistente)).willReturn(Optional.empty());

        // Act & Assert: Verificar excepcion
        assertThatThrownBy(() -> clienteService.eliminarCliente(idInexistente))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cliente no encontrado con ID: " + idInexistente);

        // Verificar que NO se intento eliminar
        then(clienteRepository).should().buscarPorId(clienteIdInexistente);
        then(clienteRepository).should(never()).eliminar(any(ClienteId.class));
    }

    @Test
    @DisplayName("Debe lanzar excepcion cuando el tamano de pagina excede el maximo")
    void testListarClientes_TamanioPaginaExcedido_LanzaExcepcion() {
        // Arrange: Pageable con tamano superior al maximo permitido (100)
        Pageable pageableInvalido = PageRequest.of(0, 101);

        // Act & Assert: Verificar excepcion
        assertThatThrownBy(() -> clienteService.obtenerClientes(pageableInvalido))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("El tamaño de página no puede exceder 100 elementos");

        // Verificar que NO se consulto el repositorio
        then(clienteRepository).should(never()).buscarTodos(any(Pageable.class));
    }

    @Test
    @DisplayName("Debe lanzar excepcion al actualizar con NIT duplicado de otro cliente")
    void testActualizarCliente_NitDuplicado_LanzaExcepcion() {
        // Arrange: Cliente existente con un NIT
        Cliente clienteExistente = Cliente.reconstituir(
            clienteIdExistente,
            nombreValido,
            nitValido,
            emailValido,
            "+57 310 1234567",
            "Calle 123 #45-67, Bogota",
            true,
            clienteValido.getFechaCreacion(),
            clienteValido.getFechaActualizacion()
        );

        // Cliente con NIT diferente (que ya existe en otro cliente)
        Nit nitDuplicado = Nit.of("900999999-9");
        Cliente clienteConNitDuplicado = Cliente.crear(
            nombreValido,
            nitDuplicado,
            emailValido,
            "+57 310 1234567",
            "Calle 123"
        );

        given(clienteRepository.buscarPorId(clienteIdExistente)).willReturn(Optional.of(clienteExistente));
        given(clienteRepository.existePorNit(nitDuplicado)).willReturn(true);

        // Act & Assert: Verificar excepcion
        assertThatThrownBy(() -> clienteService.actualizarCliente(1L, clienteConNitDuplicado))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Ya existe un cliente con el NIT");

        // Verificar interacciones
        then(clienteRepository).should().buscarPorId(clienteIdExistente);
        then(clienteRepository).should().existePorNit(nitDuplicado);
        then(clienteRepository).should(never()).guardar(any(Cliente.class));
    }
}
