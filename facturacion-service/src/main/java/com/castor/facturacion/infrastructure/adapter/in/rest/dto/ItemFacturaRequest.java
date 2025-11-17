package com.castor.facturacion.infrastructure.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * DTO para un item de factura en la petición.
 */
@Schema(description = "Item de una factura")
public class ItemFacturaRequest {

    @Schema(description = "Descripción del producto o servicio", example = "Laptop Dell XPS 15", required = true)
    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    private String descripcion;

    @Schema(description = "Cantidad de unidades", example = "2", required = true)
    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    @Max(value = 10000, message = "La cantidad no puede exceder 10000")
    private Integer cantidad;

    @Schema(description = "Precio unitario en COP", example = "3500000.00", required = true)
    @NotNull(message = "El precio unitario es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio unitario debe ser mayor a cero")
    @JsonProperty("precio_unitario")
    private BigDecimal precioUnitario;

    @Schema(description = "Porcentaje de impuesto (IVA)", example = "19.0")
    @DecimalMin(value = "0.0", message = "El porcentaje de impuesto no puede ser negativo")
    @DecimalMax(value = "100.0", message = "El porcentaje de impuesto no puede exceder 100")
    @JsonProperty("porcentaje_impuesto")
    private BigDecimal porcentajeImpuesto;

    @Schema(description = "Porcentaje de descuento", example = "10.0")
    @DecimalMin(value = "0.0", message = "El porcentaje de descuento no puede ser negativo")
    @DecimalMax(value = "100.0", message = "El porcentaje de descuento no puede exceder 100")
    @JsonProperty("porcentaje_descuento")
    private BigDecimal porcentajeDescuento;

    // Constructor por defecto
    public ItemFacturaRequest() {
    }

    // Constructor completo
    public ItemFacturaRequest(String descripcion, Integer cantidad, BigDecimal precioUnitario,
                             BigDecimal porcentajeImpuesto, BigDecimal porcentajeDescuento) {
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.porcentajeImpuesto = porcentajeImpuesto;
        this.porcentajeDescuento = porcentajeDescuento;
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

    @Override
    public String toString() {
        return "ItemFacturaRequest{" +
               "descripcion='" + descripcion + '\'' +
               ", cantidad=" + cantidad +
               ", precioUnitario=" + precioUnitario +
               '}';
    }
}
