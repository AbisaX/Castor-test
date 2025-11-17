package com.castor.facturacion.infrastructure.adapter.in.rest;

import com.castor.facturacion.domain.Factura;
import com.castor.facturacion.domain.port.in.FacturaUseCase;
import com.castor.facturacion.infrastructure.adapter.in.rest.dto.CrearFacturaRequest;
import com.castor.facturacion.infrastructure.adapter.in.rest.dto.FacturaResponse;
import com.castor.facturacion.infrastructure.adapter.in.rest.dto.PageResponse;
import com.castor.facturacion.infrastructure.adapter.in.rest.mapper.FacturaDTOMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller para la gestión de facturas.
 *
 * Adapter de entrada (Driving Adapter) que expone endpoints REST
 * y delega la lógica de negocio al caso de uso FacturaUseCase.
 *
 * Incluye paginación defensiva y documentación Swagger completa.
 */
@RestController
@RequestMapping("/api/v1/facturas")
@Tag(name = "Facturas", description = "API para gestión de facturas")
public class FacturaController {

    private static final Logger log = LoggerFactory.getLogger(FacturaController.class);

    // Constantes para paginación defensiva
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 100;

    private final FacturaUseCase facturaUseCase;
    private final FacturaDTOMapper mapper;

    public FacturaController(FacturaUseCase facturaUseCase, FacturaDTOMapper mapper) {
        this.facturaUseCase = facturaUseCase;
        this.mapper = mapper;
    }

    /**
     * Crear una nueva factura
     */
    @PostMapping
    @Operation(
        summary = "Crear factura",
        description = "Crea una nueva factura con sus items. Valida el cliente y calcula impuestos automáticamente."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Factura creada exitosamente",
            content = @Content(schema = @Schema(implementation = FacturaResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos de entrada inválidos"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Cliente no encontrado o inactivo"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Error interno del servidor"
        )
    })
    public ResponseEntity<FacturaResponse> crearFactura(
        @Valid @RequestBody CrearFacturaRequest request
    ) {
        log.info("Creando factura para cliente: {}", request.getClienteId());

        Factura factura = mapper.toDomain(request);
        Factura facturaCreada = facturaUseCase.crearFactura(factura);
        FacturaResponse response = mapper.toResponse(facturaCreada);

        log.info("Factura creada exitosamente: {} con total: {}",
            response.getNumero(), response.getTotalFinal());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Obtener factura por ID
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Obtener factura por ID",
        description = "Recupera los detalles completos de una factura específica"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Factura encontrada",
            content = @Content(schema = @Schema(implementation = FacturaResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Factura no encontrada"
        )
    })
    public ResponseEntity<FacturaResponse> obtenerFacturaPorId(
        @Parameter(description = "ID de la factura", example = "1")
        @PathVariable Long id
    ) {
        log.info("Consultando factura con ID: {}", id);

        Factura factura = facturaUseCase.obtenerFacturaPorId(id);
        FacturaResponse response = mapper.toResponse(factura);

        log.debug("Factura encontrada: {}", response.getNumero());

        return ResponseEntity.ok(response);
    }

    /**
     * Listar todas las facturas con paginación defensiva
     */
    @GetMapping
    @Operation(
        summary = "Listar todas las facturas",
        description = "Obtiene un listado paginado de todas las facturas. " +
                     "Parámetros de paginación: page >= 0, size entre 1 y 100"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de facturas obtenida exitosamente",
            content = @Content(schema = @Schema(implementation = PageResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Parámetros de paginación inválidos"
        )
    })
    public ResponseEntity<PageResponse<FacturaResponse>> listarFacturas(
        @Parameter(description = "Número de página (0-indexed)", example = "0")
        @RequestParam(defaultValue = "0") int page,

        @Parameter(description = "Tamaño de página (1-100)", example = "10")
        @RequestParam(defaultValue = "10") int size,

        @Parameter(description = "Campo para ordenar", example = "fechaCreacion")
        @RequestParam(defaultValue = "fechaCreacion") String sortBy,

        @Parameter(description = "Dirección de ordenamiento", example = "DESC")
        @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        log.info("Listando facturas - page: {}, size: {}, sortBy: {}, sortDirection: {}",
            page, size, sortBy, sortDirection);

        // Paginación defensiva
        Pageable pageable = createSafePageable(page, size, sortBy, sortDirection);

        Page<Factura> facturasPage = facturaUseCase.listarFacturas(pageable);
        List<FacturaResponse> facturas = mapper.toResponseList(facturasPage.getContent());

        PageResponse<FacturaResponse> response = new PageResponse<>(
            facturas,
            facturasPage.getNumber(),
            facturasPage.getSize(),
            facturasPage.getTotalElements(),
            facturasPage.getTotalPages(),
            facturasPage.isFirst(),
            facturasPage.isLast(),
            facturasPage.hasPrevious(),
            facturasPage.hasNext()
        );

        log.debug("Total de facturas encontradas: {}", facturasPage.getTotalElements());

        return ResponseEntity.ok(response);
    }

    /**
     * Listar facturas por cliente con paginación defensiva
     */
    @GetMapping("/cliente/{clienteId}")
    @Operation(
        summary = "Listar facturas por cliente",
        description = "Obtiene un listado paginado de todas las facturas de un cliente específico. " +
                     "Parámetros de paginación: page >= 0, size entre 1 y 100"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de facturas del cliente obtenida exitosamente",
            content = @Content(schema = @Schema(implementation = PageResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Parámetros de paginación inválidos o ID de cliente inválido"
        )
    })
    public ResponseEntity<PageResponse<FacturaResponse>> listarFacturasPorCliente(
        @Parameter(description = "ID del cliente", example = "1")
        @PathVariable Long clienteId,

        @Parameter(description = "Número de página (0-indexed)", example = "0")
        @RequestParam(defaultValue = "0") int page,

        @Parameter(description = "Tamaño de página (1-100)", example = "10")
        @RequestParam(defaultValue = "10") int size,

        @Parameter(description = "Campo para ordenar", example = "fechaCreacion")
        @RequestParam(defaultValue = "fechaCreacion") String sortBy,

        @Parameter(description = "Dirección de ordenamiento", example = "DESC")
        @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        log.info("Listando facturas del cliente {} - page: {}, size: {}", clienteId, page, size);

        // Validación del ID de cliente
        if (clienteId == null || clienteId <= 0) {
            throw new IllegalArgumentException("El ID del cliente debe ser un número positivo");
        }

        // Paginación defensiva
        Pageable pageable = createSafePageable(page, size, sortBy, sortDirection);

        Page<Factura> facturasPage = facturaUseCase.listarFacturasPorCliente(clienteId, pageable);
        List<FacturaResponse> facturas = mapper.toResponseList(facturasPage.getContent());

        PageResponse<FacturaResponse> response = new PageResponse<>(
            facturas,
            facturasPage.getNumber(),
            facturasPage.getSize(),
            facturasPage.getTotalElements(),
            facturasPage.getTotalPages(),
            facturasPage.isFirst(),
            facturasPage.isLast(),
            facturasPage.hasPrevious(),
            facturasPage.hasNext()
        );

        log.debug("Total de facturas encontradas para cliente {}: {}",
            clienteId, facturasPage.getTotalElements());

        return ResponseEntity.ok(response);
    }

    /**
     * Método auxiliar para crear un Pageable con validación defensiva
     *
     * Valida:
     * - page >= 0
     * - size entre MIN_SIZE (1) y MAX_SIZE (100)
     * - sortDirection válido (ASC o DESC)
     */
    private Pageable createSafePageable(int page, int size, String sortBy, String sortDirection) {
        // Validar y ajustar page
        int safePage = Math.max(page, DEFAULT_PAGE);
        if (page < 0) {
            log.warn("Page negativo recibido: {}. Usando valor por defecto: {}", page, DEFAULT_PAGE);
        }

        // Validar y ajustar size
        int safeSize = size;
        if (size < MIN_SIZE) {
            log.warn("Size menor que mínimo recibido: {}. Usando valor mínimo: {}", size, MIN_SIZE);
            safeSize = MIN_SIZE;
        } else if (size > MAX_SIZE) {
            log.warn("Size mayor que máximo recibido: {}. Usando valor máximo: {}", size, MAX_SIZE);
            safeSize = MAX_SIZE;
        }

        // Validar sortBy (campo permitido)
        String safeSortBy = sortBy;
        if (sortBy == null || sortBy.trim().isEmpty()) {
            safeSortBy = "fechaCreacion";
            log.debug("SortBy vacío. Usando valor por defecto: fechaCreacion");
        }

        // Validar sortDirection
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(sortDirection);
        } catch (IllegalArgumentException e) {
            log.warn("SortDirection inválido: {}. Usando DESC por defecto", sortDirection);
            direction = Sort.Direction.DESC;
        }

        Sort sort = Sort.by(direction, safeSortBy);

        log.debug("Pageable creado - page: {}, size: {}, sort: {}", safePage, safeSize, sort);

        return PageRequest.of(safePage, safeSize, sort);
    }
}
