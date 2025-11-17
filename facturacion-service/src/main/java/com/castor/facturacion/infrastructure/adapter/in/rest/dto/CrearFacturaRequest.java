package com.castor.facturacion.infrastructure.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * DTO para crear una factura (Request).
 *
 * DTO optimizado con validaciones completas y documentación Swagger.
 */
@Schema(description = "Datos para crear una nueva factura")
public class CrearFacturaRequest {

    @Schema(description = "ID del cliente", example = "1", required = true)
    @NotNull(message = "El ID del cliente es obligatorio")
    @Positive(message = "El ID del cliente debe ser positivo")
    @JsonProperty("cliente_id")
    private Long clienteId;

    @Schema(description = "Lista de items de la factura", required = true)
    @NotEmpty(message = "La factura debe tener al menos un ítem")
    @Valid
    private List<ItemFacturaRequest> items;

    // Constructor por defecto
    public CrearFacturaRequest() {
    }

    // Constructor completo
    public CrearFacturaRequest(Long clienteId, List<ItemFacturaRequest> items) {
        this.clienteId = clienteId;
        this.items = items;
    }

    // Getters y Setters

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public List<ItemFacturaRequest> getItems() {
        return items;
    }

    public void setItems(List<ItemFacturaRequest> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        return "CrearFacturaRequest{" +
               "clienteId=" + clienteId +
               ", items=" + (items != null ? items.size() : 0) +
               '}';
    }
}
