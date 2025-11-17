package com.castor.clientes.infrastructure.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de request para Cliente
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos para crear o actualizar un cliente")
public class ClienteRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, max = 200, message = "El nombre debe tener entre 3 y 200 caracteres")
    @Schema(description = "Nombre o razón social del cliente", example = "Acme Corporation S.A.")
    @JsonProperty("nombre")
    private String nombre;

    @NotBlank(message = "El NIT es obligatorio")
    @Size(min = 9, max = 15, message = "El NIT debe tener entre 9 y 15 caracteres")
    @Schema(description = "Número de Identificación Tributaria", example = "900123456-7")
    @JsonProperty("nit")
    private String nit;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    @Schema(description = "Correo electrónico del cliente", example = "contacto@acme.com")
    @JsonProperty("email")
    private String email;

    @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    @Schema(description = "Teléfono de contacto", example = "+57 310 1234567")
    @JsonProperty("telefono")
    private String telefono;

    @Size(max = 255, message = "La dirección no puede exceder 255 caracteres")
    @Schema(description = "Dirección física del cliente", example = "Calle 123 #45-67, Bogotá")
    @JsonProperty("direccion")
    private String direccion;

    @Schema(description = "Estado activo del cliente", example = "true", defaultValue = "true")
    @JsonProperty("activo")
    private Boolean activo;
}
