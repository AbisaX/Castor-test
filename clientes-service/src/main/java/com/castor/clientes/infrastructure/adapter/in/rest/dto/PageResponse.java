package com.castor.clientes.infrastructure.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO genérico para respuestas paginadas
 * Wrapper para respuestas de API con paginación
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta paginada genérica")
public class PageResponse<T> {

    @Schema(description = "Lista de elementos de la página actual")
    @JsonProperty("content")
    private List<T> content;

    @Schema(description = "Número de página actual (0-indexed)", example = "0")
    @JsonProperty("page_number")
    private int pageNumber;

    @Schema(description = "Tamaño de la página", example = "10")
    @JsonProperty("page_size")
    private int pageSize;

    @Schema(description = "Número total de elementos", example = "100")
    @JsonProperty("total_elements")
    private long totalElements;

    @Schema(description = "Número total de páginas", example = "10")
    @JsonProperty("total_pages")
    private int totalPages;

    @Schema(description = "Indica si es la primera página", example = "true")
    @JsonProperty("is_first")
    private boolean isFirst;

    @Schema(description = "Indica si es la última página", example = "false")
    @JsonProperty("is_last")
    private boolean isLast;

    @Schema(description = "Indica si tiene página siguiente", example = "true")
    @JsonProperty("has_next")
    private boolean hasNext;

    @Schema(description = "Indica si tiene página anterior", example = "false")
    @JsonProperty("has_previous")
    private boolean hasPrevious;

    /**
     * Factory method para crear PageResponse desde Spring Data Page
     */
    public static <T> PageResponse<T> from(org.springframework.data.domain.Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }
}
