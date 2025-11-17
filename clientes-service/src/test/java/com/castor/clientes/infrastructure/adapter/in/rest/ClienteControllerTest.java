package com.castor.clientes.infrastructure.adapter.in.rest;

import com.castor.clientes.domain.Cliente;
import com.castor.clientes.domain.port.in.ClienteUseCase;
import com.castor.clientes.domain.valueobject.ClienteId;
import com.castor.clientes.domain.valueobject.Email;
import com.castor.clientes.domain.valueobject.Nit;
import com.castor.clientes.domain.valueobject.NombreCliente;
import com.castor.clientes.infrastructure.adapter.in.rest.dto.ClienteRequestDTO;
import com.castor.clientes.infrastructure.adapter.in.rest.dto.ClienteResponseDTO;
import com.castor.clientes.infrastructure.adapter.in.rest.mapper.ClienteDTOMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de API REST para ClienteController
 * Utiliza MockMvc para simular peticiones HTTP
 * Patrón AAA: Arrange-Act-Assert
 */
@WebMvcTest(ClienteController.class)
@Import(ClienteDTOMapper.class)
@DisplayName("Tests API REST - ClienteController")
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ClienteUseCase clienteUseCase;

    @Autowired
    private ClienteDTOMapper mapper;

    private ClienteRequestDTO requestValido;
    private Cliente clienteDominio;
    private LocalDateTime ahora;

    @BeforeEach
    void setUp() {
        // Arrange: Preparar datos de prueba comunes
        ahora = LocalDateTime.now();

        requestValido = ClienteRequestDTO.builder()
            .nombre("Acme Corporation S.A.")
            .nit("900123456-7")
            .email("contacto@acme.com")
            .telefono("+57 310 1234567")
            .direccion("Calle 123 #45-67, Bogota")
            .activo(true)
            .build();

        clienteDominio = Cliente.reconstituir(
            ClienteId.of(1L),
            NombreCliente.of("Acme Corporation S.A."),
            Nit.of("900123456-7"),
            Email.of("contacto@acme.com"),
            "+57 310 1234567",
            "Calle 123 #45-67, Bogota",
            true,
            ahora,
            ahora
        );
    }

    @Test
    @DisplayName("POST /api/v1/clientes - Debe crear cliente y retornar 201 Created")
    void testCrearCliente_201Created() throws Exception {
        // Arrange: Configurar mock para creacion exitosa
        given(clienteUseCase.crearCliente(any(Cliente.class))).willReturn(clienteDominio);

        // Act & Assert: Ejecutar peticion y verificar respuesta
        mockMvc.perform(post("/api/v1/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestValido)))
            .andDo(print())
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.nombre", is("Acme Corporation S.A.")))
            .andExpect(jsonPath("$.nit", is("900123456-7")))
            .andExpect(jsonPath("$.email", is("contacto@acme.com")))
            .andExpect(jsonPath("$.telefono", is("+57 310 1234567")))
            .andExpect(jsonPath("$.direccion", is("Calle 123 #45-67, Bogota")))
            .andExpect(jsonPath("$.activo", is(true)))
            .andExpect(jsonPath("$.fechaCreacion", notNullValue()))
            .andExpect(jsonPath("$.fechaActualizacion", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/clientes - Debe retornar 400 Bad Request cuando falta nombre")
    void testCrearCliente_ValidacionFalla_400BadRequest() throws Exception {
        // Arrange: Request sin nombre (campo obligatorio)
        ClienteRequestDTO requestInvalido = ClienteRequestDTO.builder()
            .nit("900123456-7")
            .email("contacto@acme.com")
            .build();

        // Act & Assert: Verificar validacion
        mockMvc.perform(post("/api/v1/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestInvalido)))
            .andDo(print())
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/clientes - Debe retornar 400 cuando el email es invalido")
    void testCrearCliente_EmailInvalido_400BadRequest() throws Exception {
        // Arrange: Request con email invalido
        ClienteRequestDTO requestInvalido = ClienteRequestDTO.builder()
            .nombre("Acme Corporation S.A.")
            .nit("900123456-7")
            .email("email-invalido")
            .build();

        // Act & Assert: Verificar validacion de email
        mockMvc.perform(post("/api/v1/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestInvalido)))
            .andDo(print())
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/clientes - Debe retornar 400 cuando el nombre es muy corto")
    void testCrearCliente_NombreMuyCorto_400BadRequest() throws Exception {
        // Arrange: Request con nombre muy corto (minimo 3 caracteres)
        ClienteRequestDTO requestInvalido = ClienteRequestDTO.builder()
            .nombre("AB")
            .nit("900123456-7")
            .email("contacto@acme.com")
            .build();

        // Act & Assert: Verificar validacion de longitud minima
        mockMvc.perform(post("/api/v1/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestInvalido)))
            .andDo(print())
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/clientes/{id} - Debe retornar cliente existente con 200 OK")
    void testObtenerCliente_200Ok() throws Exception {
        // Arrange: Configurar mock para cliente existente
        given(clienteUseCase.obtenerCliente(1L)).willReturn(clienteDominio);

        // Act & Assert: Ejecutar peticion GET
        mockMvc.perform(get("/api/v1/clientes/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.nombre", is("Acme Corporation S.A.")))
            .andExpect(jsonPath("$.nit", is("900123456-7")))
            .andExpect(jsonPath("$.email", is("contacto@acme.com")));
    }

    @Test
    @DisplayName("GET /api/v1/clientes/{id} - Debe retornar 404 Not Found cuando cliente no existe")
    void testObtenerCliente_404NotFound() throws Exception {
        // Arrange: Configurar mock para cliente inexistente
        Long idInexistente = 999L;
        given(clienteUseCase.obtenerCliente(idInexistente))
            .willThrow(new IllegalArgumentException("Cliente no encontrado con ID: " + idInexistente));

        // Act & Assert: Verificar respuesta 404
        mockMvc.perform(get("/api/v1/clientes/{id}", idInexistente)
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/clientes - Debe listar clientes con paginacion y retornar 200 OK")
    void testListarClientes_ConPaginacion_200Ok() throws Exception {
        // Arrange: Crear pagina de clientes
        Cliente cliente1 = Cliente.reconstituir(
            ClienteId.of(1L),
            NombreCliente.of("Cliente Uno"),
            Nit.of("900111111-1"),
            Email.of("uno@test.com"),
            "111111111",
            "Direccion 1",
            true,
            ahora,
            ahora
        );

        Cliente cliente2 = Cliente.reconstituir(
            ClienteId.of(2L),
            NombreCliente.of("Cliente Dos"),
            Nit.of("900222222-2"),
            Email.of("dos@test.com"),
            "222222222",
            "Direccion 2",
            true,
            ahora,
            ahora
        );

        Pageable pageable = PageRequest.of(0, 10);
        Page<Cliente> clientesPage = new PageImpl<>(Arrays.asList(cliente1, cliente2), pageable, 2);

        given(clienteUseCase.obtenerClientes(any(Pageable.class))).willReturn(clientesPage);

        // Act & Assert: Ejecutar peticion GET con paginacion
        mockMvc.perform(get("/api/v1/clientes")
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "id")
                .param("sortDirection", "ASC")
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content", hasSize(2)))
            .andExpect(jsonPath("$.content[0].id", is(1)))
            .andExpect(jsonPath("$.content[0].nombre", is("Cliente Uno")))
            .andExpect(jsonPath("$.content[1].id", is(2)))
            .andExpect(jsonPath("$.content[1].nombre", is("Cliente Dos")))
            .andExpect(jsonPath("$.totalElements", is(2)))
            .andExpect(jsonPath("$.totalPages", is(1)))
            .andExpect(jsonPath("$.size", is(10)))
            .andExpect(jsonPath("$.number", is(0)));
    }

    @Test
    @DisplayName("GET /api/v1/clientes - Debe usar valores por defecto cuando la paginacion es invalida")
    void testListarClientes_PaginacionInvalida_UsaDefaults() throws Exception {
        // Arrange: Pagina vacia como respuesta por defecto
        Pageable pageable = PageRequest.of(0, 10);
        Page<Cliente> clientesPage = new PageImpl<>(Arrays.asList(), pageable, 0);

        given(clienteUseCase.obtenerClientes(any(Pageable.class))).willReturn(clientesPage);

        // Act & Assert: Ejecutar peticion sin parametros (usa defaults)
        mockMvc.perform(get("/api/v1/clientes")
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content", hasSize(0)))
            .andExpect(jsonPath("$.totalElements", is(0)));
    }

    @Test
    @DisplayName("GET /api/v1/clientes - Debe ajustar tamano de pagina cuando excede el maximo")
    void testListarClientes_TamanioPaginaExcedido_AjustaAMaximo() throws Exception {
        // Arrange: Configurar respuesta con tamano ajustado
        Pageable pageable = PageRequest.of(0, 100); // Maximo permitido
        Page<Cliente> clientesPage = new PageImpl<>(Arrays.asList(), pageable, 0);

        given(clienteUseCase.obtenerClientes(any(Pageable.class))).willReturn(clientesPage);

        // Act & Assert: Peticion con size=150 (excede maximo de 100)
        mockMvc.perform(get("/api/v1/clientes")
                .param("page", "0")
                .param("size", "150") // Se ajustara a 100
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/v1/clientes/{id} - Debe actualizar cliente y retornar 200 OK")
    void testActualizarCliente_200Ok() throws Exception {
        // Arrange: Preparar cliente actualizado
        Cliente clienteActualizado = Cliente.reconstituir(
            ClienteId.of(1L),
            NombreCliente.of("Nueva Razon Social S.A."),
            Nit.of("900123456-7"),
            Email.of("nuevo@email.com"),
            "+57 320 9876543",
            "Nueva Direccion 456",
            true,
            ahora,
            ahora
        );

        given(clienteUseCase.actualizarCliente(eq(1L), any(Cliente.class))).willReturn(clienteActualizado);

        ClienteRequestDTO requestActualizado = ClienteRequestDTO.builder()
            .nombre("Nueva Razon Social S.A.")
            .nit("900123456-7")
            .email("nuevo@email.com")
            .telefono("+57 320 9876543")
            .direccion("Nueva Direccion 456")
            .activo(true)
            .build();

        // Act & Assert: Ejecutar peticion PUT
        mockMvc.perform(put("/api/v1/clientes/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestActualizado)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.nombre", is("Nueva Razon Social S.A.")))
            .andExpect(jsonPath("$.email", is("nuevo@email.com")))
            .andExpect(jsonPath("$.telefono", is("+57 320 9876543")))
            .andExpect(jsonPath("$.direccion", is("Nueva Direccion 456")));
    }

    @Test
    @DisplayName("PUT /api/v1/clientes/{id} - Debe retornar 404 cuando cliente no existe")
    void testActualizarCliente_404NotFound() throws Exception {
        // Arrange: Configurar mock para cliente inexistente
        Long idInexistente = 999L;
        given(clienteUseCase.actualizarCliente(eq(idInexistente), any(Cliente.class)))
            .willThrow(new IllegalArgumentException("Cliente no encontrado con ID: " + idInexistente));

        // Act & Assert: Verificar respuesta 404
        mockMvc.perform(put("/api/v1/clientes/{id}", idInexistente)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestValido)))
            .andDo(print())
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/clientes/{id} - Debe retornar 400 cuando datos son invalidos")
    void testActualizarCliente_DatosInvalidos_400BadRequest() throws Exception {
        // Arrange: Request con datos invalidos (email incorrecto)
        ClienteRequestDTO requestInvalido = ClienteRequestDTO.builder()
            .nombre("Acme Corporation S.A.")
            .nit("900123456-7")
            .email("email-sin-formato-valido")
            .build();

        // Act & Assert: Verificar validacion
        mockMvc.perform(put("/api/v1/clientes/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestInvalido)))
            .andDo(print())
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/v1/clientes/{id} - Debe eliminar cliente y retornar 204 No Content")
    void testEliminarCliente_204NoContent() throws Exception {
        // Arrange: Configurar mock para eliminacion exitosa
        willDoNothing().given(clienteUseCase).eliminarCliente(1L);

        // Act & Assert: Ejecutar peticion DELETE
        mockMvc.perform(delete("/api/v1/clientes/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/clientes/{id} - Debe retornar 404 cuando cliente no existe")
    void testEliminarCliente_404NotFound() throws Exception {
        // Arrange: Configurar mock para cliente inexistente
        Long idInexistente = 999L;
        willThrow(new IllegalArgumentException("Cliente no encontrado con ID: " + idInexistente))
            .given(clienteUseCase).eliminarCliente(idInexistente);

        // Act & Assert: Verificar respuesta 404
        mockMvc.perform(delete("/api/v1/clientes/{id}", idInexistente)
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/clientes - Debe manejar direccion de ordenamiento invalida")
    void testListarClientes_DireccionOrdenamientoInvalida_UsaDefault() throws Exception {
        // Arrange: Configurar respuesta
        Pageable pageable = PageRequest.of(0, 10);
        Page<Cliente> clientesPage = new PageImpl<>(Arrays.asList(), pageable, 0);

        given(clienteUseCase.obtenerClientes(any(Pageable.class))).willReturn(clientesPage);

        // Act & Assert: Peticion con sortDirection invalido (usa ASC por defecto)
        mockMvc.perform(get("/api/v1/clientes")
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "nombre")
                .param("sortDirection", "INVALIDO")
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/clientes - Debe manejar pagina negativa usando valor por defecto")
    void testListarClientes_PaginaNegativa_UsaDefault() throws Exception {
        // Arrange: Configurar respuesta
        Pageable pageable = PageRequest.of(0, 10);
        Page<Cliente> clientesPage = new PageImpl<>(Arrays.asList(), pageable, 0);

        given(clienteUseCase.obtenerClientes(any(Pageable.class))).willReturn(clientesPage);

        // Act & Assert: Peticion con page negativo (se ajusta a 0)
        mockMvc.perform(get("/api/v1/clientes")
                .param("page", "-1")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/clientes - Debe retornar 400 cuando NIT es muy corto")
    void testCrearCliente_NitMuyCorto_400BadRequest() throws Exception {
        // Arrange: Request con NIT muy corto (minimo 9 caracteres)
        ClienteRequestDTO requestInvalido = ClienteRequestDTO.builder()
            .nombre("Acme Corporation S.A.")
            .nit("12345")
            .email("contacto@acme.com")
            .build();

        // Act & Assert: Verificar validacion
        mockMvc.perform(post("/api/v1/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestInvalido)))
            .andDo(print())
            .andExpect(status().isBadRequest());
    }
}
