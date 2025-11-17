package com.castor.facturacion.infrastructure.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * DTO para la respuesta de un item de factura.
 */
@Schema(description = "Item de factura en la respuesta")
public class ItemFacturaResponse {

    @Schema(description = "Descripción del producto/servicio", example = "Laptop Dell XPS 15")
    private String descripcion;

    @Schema(description = "Cantidad", example = "2")
    private Integer cantidad;

    @Schema(description = "Precio unitario", example = "3500000.00")
    @JsonProperty("precio_unitario")
    private BigDecimal precioUnitario;

    @Schema(description = "Porcentaje de impuesto aplicado", example = "19.0")
    @JsonProperty("porcentaje_impuesto")
    private BigDecimal porcentajeImpuesto;

    @Schema(description = "Porcentaje de descuento aplicado", example = "10.0")
    @JsonProperty("porcentaje_descuento")
    private BigDecimal porcentajeDescuento;

    @Schema(description = "Subtotal del item", example = "7000000.00")
    private BigDecimal subtotal;

    @Schema(description = "Impuesto calculado", example = "1330000.00")
    private BigDecimal impuesto;

    @Schema(description = "Descuento calculado", example = "700000.00")
    private BigDecimal descuento;

    @Schema(description = "Total del item", example = "7630000.00")
    private BigDecimal total;

    // Constructor por defecto
    public ItemFacturaResponse() {
    }

    // Constructor completo
    public ItemFacturaResponse(String descripcion, Integer cantidad, BigDecimal precioUnitario,
                              BigDecimal porcentajeImpuesto, BigDecimal porcentajeDescuento,
                              BigDecimal subtotal, BigDecimal impuesto,
                              BigDecimal descuento, BigDecimal total) {
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.porcentajeImpuesto = porcentajeImpuesto;
        this.porcentajeDescuento = porcentajeDescuento;
        this.subtotal = subtotal;
        this.impuesto = impuesto;
        this.descuento = descuento;
        this.total = total;
    }

    // Getters y Setters

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public BigDecimal getPorcentajeImpuesto() {
        return porcentajeImpuesto;
    }

    public void setPorcentajeImpuesto(BigDecimal porcentajeImpuesto) {
        this.porcentajeImpuesto = porcentajeImpuesto;
    }

    public BigDecimal getPorcentajeDescuento() {
        return porcentajeDescuento;
    }

    public void setPorcentajeDescuento(BigDecimal porcentajeDescuento) {
        this.porcentajeDescuento = porcentajeDescuento;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getImpuesto() {
        return impuesto;
    }

    public void setImpuesto(BigDecimal impuesto) {
        this.impuesto = impuesto;
    }

    public BigDecimal getDescuento() {
        return descuento;
    }

    public void setDescuento(BigDecimal descuento) {
        this.descuento = descuento;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    @Override
    public String toString() {
        return "ItemFacturaResponse{" +
               "descripcion='" + descripcion + '\'' +
               ", cantidad=" + cantidad +
               ", total=" + total +
               '}';
    }
}
