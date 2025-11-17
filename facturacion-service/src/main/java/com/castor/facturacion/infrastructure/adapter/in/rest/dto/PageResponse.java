package com.castor.facturacion.infrastructure.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO genérico para respuestas paginadas.
 *
 * Proporciona metadatos de paginación junto con los datos.
 *
 * @param <T> Tipo de los elementos en la página
 */
@Schema(description = "Respuesta paginada genérica")
public class PageResponse<T> {

    @Schema(description = "Lista de elementos en la página actual")
    private List<T> content;

    @Schema(description = "Número de página actual (0-indexed)", example = "0")
    @JsonProperty("page_number")
    private int pageNumber;

    @Schema(description = "Tamaño de página", example = "20")
    @JsonProperty("page_size")
    private int pageSize;

    @Schema(description = "Número total de elementos", example = "100")
    @JsonProperty("total_elements")
    private long totalElements;

    @Schema(description = "Número total de páginas", example = "5")
    @JsonProperty("total_pages")
    private int totalPages;

    @Schema(description = "Indica si es la primera página", example = "true")
    @JsonProperty("is_first")
    private boolean isFirst;

    @Schema(description = "Indica si es la última página", example = "false")
    @JsonProperty("is_last")
    private boolean isLast;

    @Schema(description = "Indica si tiene página anterior", example = "false")
    @JsonProperty("has_previous")
    private boolean hasPrevious;

    @Schema(description = "Indica si tiene página siguiente", example = "true")
    @JsonProperty("has_next")
    private boolean hasNext;

    // Constructor por defecto
    public PageResponse() {
    }

    // Constructor completo
    public PageResponse(List<T> content, int pageNumber, int pageSize, long totalElements,
                       int totalPages, boolean isFirst, boolean isLast,
                       boolean hasPrevious, boolean hasNext) {
        this.content = content;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.isFirst = isFirst;
        this.isLast = isLast;
        this.hasPrevious = hasPrevious;
        this.hasNext = hasNext;
    }

    // Getters y Setters

    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public boolean isFirst() {
        return isFirst;
    }

    public void setFirst(boolean first) {
        isFirst = first;
    }

    public boolean isLast() {
        return isLast;
    }

    public void setLast(boolean last) {
        isLast = last;
    }

    public boolean isHasPrevious() {
        return hasPrevious;
    }

    public void setHasPrevious(boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }

    @Override
    public String toString() {
        return "PageResponse{" +
               "pageNumber=" + pageNumber +
               ", pageSize=" + pageSize +
               ", totalElements=" + totalElements +
               ", totalPages=" + totalPages +
               '}';
    }
}
