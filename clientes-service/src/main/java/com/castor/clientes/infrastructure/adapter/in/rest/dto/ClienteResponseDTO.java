package com.castor.clientes.infrastructure.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de response para Cliente
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Información completa de un cliente")
public class ClienteResponseDTO {

    @Schema(description = "ID único del cliente", example = "1")
    @JsonProperty("id")
    private Long id;

    @Schema(description = "Nombre o razón social del cliente", example = "Acme Corporation S.A.")
    @JsonProperty("nombre")
    private String nombre;

    @Schema(description = "Número de Identificación Tributaria", example = "900123456-7")
    @JsonProperty("nit")
    private String nit;

    @Schema(description = "Correo electrónico del cliente", example = "contacto@acme.com")
    @JsonProperty("email")
    private String email;

    @Schema(description = "Teléfono de contacto", example = "+57 310 1234567")
    @JsonProperty("telefono")
    private String telefono;

    @Schema(description = "Dirección física del cliente", example = "Calle 123 #45-67, Bogotá")
    @JsonProperty("direccion")
    private String direccion;

    @Schema(description = "Estado activo del cliente", example = "true")
    @JsonProperty("activo")
    private Boolean activo;

    @Schema(description = "Fecha de creación del registro", example = "2024-01-15T10:30:00")
    @JsonProperty("fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Schema(description = "Fecha de última actualización", example = "2024-01-15T14:20:00")
    @JsonProperty("fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
