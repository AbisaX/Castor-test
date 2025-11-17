package com.castor.facturacion.infrastructure.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para la respuesta de una factura.
 *
 * DTO optimizado para serialización JSON con formato snake_case.
 */
@Schema(description = "Respuesta con los datos completos de una factura")
public class FacturaResponse {

    @Schema(description = "ID de la factura", example = "1")
    private Long id;

    @Schema(description = "Número de factura", example = "FACT-20250116123045")
    private String numero;

    @Schema(description = "ID del cliente", example = "1")
    @JsonProperty("cliente_id")
    private Long clienteId;

    @Schema(description = "Lista de items de la factura")
    private List<ItemFacturaResponse> items;

    @Schema(description = "Subtotal general (suma de subtotales de items)", example = "7000000.00")
    @JsonProperty("subtotal_general")
    private BigDecimal subtotalGeneral;

    @Schema(description = "Total de impuestos (IVA)", example = "1330000.00")
    @JsonProperty("total_impuestos")
    private BigDecimal totalImpuestos;

    @Schema(description = "Total de descuentos", example = "700000.00")
    @JsonProperty("total_descuentos")
    private BigDecimal totalDescuentos;

    @Schema(description = "Total final de la factura", example = "7630000.00")
    @JsonProperty("total_final")
    private BigDecimal totalFinal;

    @Schema(description = "Fecha y hora de creación", example = "2025-01-16T12:30:45")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("fecha_creacion")
    private LocalDateTime fechaCreacion;

    // Constructor por defecto
    public FacturaResponse() {
    }

    // Constructor completo
    public FacturaResponse(Long id, String numero, Long clienteId, List<ItemFacturaResponse> items,
                          BigDecimal subtotalGeneral, BigDecimal totalImpuestos,
                          BigDecimal totalDescuentos, BigDecimal totalFinal,
                          LocalDateTime fechaCreacion) {
        this.id = id;
        this.numero = numero;
        this.clienteId = clienteId;
        this.items = items;
        this.subtotalGeneral = subtotalGeneral;
        this.totalImpuestos = totalImpuestos;
        this.totalDescuentos = totalDescuentos;
        this.totalFinal = totalFinal;
        this.fechaCreacion = fechaCreacion;
    }

    // Getters y Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public List<ItemFacturaResponse> getItems() {
        return items;
    }

    public void setItems(List<ItemFacturaResponse> items) {
        this.items = items;
    }

    public BigDecimal getSubtotalGeneral() {
        return subtotalGeneral;
    }

    public void setSubtotalGeneral(BigDecimal subtotalGeneral) {
        this.subtotalGeneral = subtotalGeneral;
    }

    public BigDecimal getTotalImpuestos() {
        return totalImpuestos;
    }

    public void setTotalImpuestos(BigDecimal totalImpuestos) {
        this.totalImpuestos = totalImpuestos;
    }

    public BigDecimal getTotalDescuentos() {
        return totalDescuentos;
    }

    public void setTotalDescuentos(BigDecimal totalDescuentos) {
        this.totalDescuentos = totalDescuentos;
    }

    public BigDecimal getTotalFinal() {
        return totalFinal;
    }

    public void setTotalFinal(BigDecimal totalFinal) {
        this.totalFinal = totalFinal;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    @Override
    public String toString() {
        return "FacturaResponse{" +
               "id=" + id +
               ", numero='" + numero + '\'' +
               ", clienteId=" + clienteId +
               ", totalFinal=" + totalFinal +
               '}';
    }
}
