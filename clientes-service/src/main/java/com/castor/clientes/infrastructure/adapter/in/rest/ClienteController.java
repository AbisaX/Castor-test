package com.castor.clientes.infrastructure.adapter.in.rest;

import com.castor.clientes.domain.Cliente;
import com.castor.clientes.domain.port.in.ClienteUseCase;
import com.castor.clientes.infrastructure.adapter.in.rest.dto.ClienteRequestDTO;
import com.castor.clientes.infrastructure.adapter.in.rest.dto.ClienteResponseDTO;
import com.castor.clientes.infrastructure.adapter.in.rest.dto.PageResponse;
import com.castor.clientes.infrastructure.adapter.in.rest.mapper.ClienteDTOMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para la gestión de clientes
 * Adaptador de entrada que expone los casos de uso mediante API REST
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/clientes")
@RequiredArgsConstructor
@Validated
@Tag(name = "Clientes", description = "API para gestión de clientes")
public class ClienteController {

    private final ClienteUseCase clienteUseCase;
    private final ClienteDTOMapper mapper;

    // Constantes para paginación
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 100;

    @Operation(summary = "Crear nuevo cliente", description = "Crea un nuevo cliente en el sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Cliente creado exitosamente",
                     content = @Content(schema = @Schema(implementation = ClienteResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "409", description = "Ya existe un cliente con el mismo NIT"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping
    public ResponseEntity<ClienteResponseDTO> crearCliente(
            @Parameter(description = "Datos del cliente a crear", required = true)
            @Valid @RequestBody ClienteRequestDTO request) {
        log.info("POST /api/v1/clientes - Crear cliente: {}", request.getNombre());
        log.debug("Request details - NIT: {}, Email: {}", request.getNit(), request.getEmail());

        Cliente cliente = mapper.toDomain(request);
        Cliente clienteCreado = clienteUseCase.crearCliente(cliente);
        ClienteResponseDTO response = mapper.toResponse(clienteCreado);

        log.info("Cliente creado exitosamente con ID: {}", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Obtener cliente por ID", description = "Retorna la información de un cliente específico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cliente encontrado",
                     content = @Content(schema = @Schema(implementation = ClienteResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Cliente no encontrado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> obtenerCliente(
            @Parameter(description = "ID del cliente", required = true, example = "1")
            @PathVariable Long id) {
        log.info("GET /api/v1/clientes/{} - Obtener cliente", id);

        Cliente cliente = clienteUseCase.obtenerCliente(id);
        ClienteResponseDTO response = mapper.toResponse(cliente);

        log.debug("Cliente encontrado: {}", response.getNombre());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Listar clientes con paginación",
               description = "Retorna una lista paginada de todos los clientes")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de clientes obtenida exitosamente"),
        @ApiResponse(responseCode = "400", description = "Parámetros de paginación inválidos"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping
    public ResponseEntity<PageResponse<ClienteResponseDTO>> obtenerClientes(
            @Parameter(description = "Número de página (0-indexed)", example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Tamaño de página (máximo 100)", example = "10")
            @RequestParam(required = false) Integer size,
            @Parameter(description = "Campo para ordenar", example = "nombre")
            @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Dirección de ordenamiento", example = "ASC")
            @RequestParam(defaultValue = "ASC") String sortDirection) {

        log.info("GET /api/v1/clientes - Página: {}, Tamaño: {}, Ordenar por: {} {}",
                 page, size, sortBy, sortDirection);

        // Crear paginación defensiva
        Pageable pageable = createSafePageable(page, size, sortBy, sortDirection);

        Page<Cliente> clientesPage = clienteUseCase.obtenerClientes(pageable);
        Page<ClienteResponseDTO> responsePage = clientesPage.map(mapper::toResponse);
        PageResponse<ClienteResponseDTO> response = PageResponse.from(responsePage);

        log.info("Retornando {} clientes de un total de {}",
                 response.getContent().size(), response.getTotalElements());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Actualizar cliente", description = "Actualiza la información de un cliente existente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cliente actualizado exitosamente",
                     content = @Content(schema = @Schema(implementation = ClienteResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Cliente no encontrado"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "409", description = "El NIT ya está siendo usado por otro cliente"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> actualizarCliente(
            @Parameter(description = "ID del cliente", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Datos actualizados del cliente", required = true)
            @Valid @RequestBody ClienteRequestDTO request) {
        log.info("PUT /api/v1/clientes/{} - Actualizar cliente", id);
        log.debug("Update details - Nombre: {}, NIT: {}", request.getNombre(), request.getNit());

        Cliente cliente = mapper.toDomain(request);
        Cliente clienteActualizado = clienteUseCase.actualizarCliente(id, cliente);
        ClienteResponseDTO response = mapper.toResponse(clienteActualizado);

        log.info("Cliente {} actualizado exitosamente", id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Eliminar cliente", description = "Elimina un cliente del sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Cliente eliminado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Cliente no encontrado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCliente(
            @Parameter(description = "ID del cliente", required = true, example = "1")
            @PathVariable Long id) {
        log.info("DELETE /api/v1/clientes/{} - Eliminar cliente", id);

        clienteUseCase.eliminarCliente(id);

        log.info("Cliente {} eliminado exitosamente", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Crea un Pageable con validaciones defensivas
     * Asegura que los parámetros de paginación estén dentro de límites seguros
     */
    private Pageable createSafePageable(Integer page, Integer size, String sortBy, String sortDirection) {
        // Aplicar valores por defecto si son nulos
        int safePage = (page != null) ? page : DEFAULT_PAGE;
        int safeSize = (size != null) ? size : DEFAULT_SIZE;

        // Validación defensiva de página
        if (safePage < 0) {
            log.warn("Parámetro 'page' inválido: {}. Usando valor por defecto: {}", safePage, DEFAULT_PAGE);
            safePage = DEFAULT_PAGE;
        }

        // Validación defensiva de tamaño
        if (safeSize < MIN_PAGE_SIZE) {
            log.warn("Parámetro 'size' muy pequeño: {}. Usando mínimo: {}", safeSize, MIN_PAGE_SIZE);
            safeSize = MIN_PAGE_SIZE;
        } else if (safeSize > MAX_PAGE_SIZE) {
            log.warn("Parámetro 'size' muy grande: {}. Usando máximo: {}", safeSize, MAX_PAGE_SIZE);
            safeSize = MAX_PAGE_SIZE;
        }

        // Crear Sort
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(sortDirection);
        } catch (IllegalArgumentException e) {
            log.warn("Dirección de ordenamiento inválida: {}. Usando ASC", sortDirection);
            direction = Sort.Direction.ASC;
        }

        Sort sort = Sort.by(direction, sortBy);

        log.debug("Pageable creado - Página: {}, Tamaño: {}, Sort: {} {}", safePage, safeSize, sortBy, direction);
        return PageRequest.of(safePage, safeSize, sort);
    }
}
