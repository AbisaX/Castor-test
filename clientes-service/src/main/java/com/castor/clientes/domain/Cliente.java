package com.castor.clientes.domain;

import com.castor.clientes.domain.valueobject.ClienteId;
import com.castor.clientes.domain.valueobject.Email;
import com.castor.clientes.domain.valueobject.Nit;
import com.castor.clientes.domain.valueobject.NombreCliente;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Agregado Root: Cliente
 * Bounded Context: Gestión de Clientes
 *
 * Representa un cliente corporativo del sistema de facturación.
 * Sin anotaciones de framework - Dominio puro.
 */
public class Cliente {
    private ClienteId id;
    private NombreCliente nombre;
    private Nit nit;
    private Email email;
    private String telefono;
    private String direccion;
    private boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    // Constructor privado para construcción controlada
    private Cliente() {
    }

    // Constructor completo
    private Cliente(ClienteId id, NombreCliente nombre, Nit nit, Email email,
                    String telefono, String direccion, boolean activo,
                    LocalDateTime fechaCreacion, LocalDateTime fechaActualizacion) {
        this.id = id;
        this.nombre = nombre;
        this.nit = nit;
        this.email = email;
        this.telefono = telefono;
        this.direccion = direccion;
        this.activo = activo;
        this.fechaCreacion = fechaCreacion;
        this.fechaActualizacion = fechaActualizacion;
    }

    /**
     * Factory Method: Crear nuevo cliente
     */
    public static Cliente crear(NombreCliente nombre, Nit nit, Email email,
                                String telefono, String direccion) {
        validarDatosCreacion(nombre, nit, email);

        LocalDateTime ahora = LocalDateTime.now();
        return new Cliente(
            ClienteId.generate(),
            nombre,
            nit,
            email,
            telefono,
            direccion,
            true, // Por defecto activo
            ahora,
            ahora
        );
    }

    /**
     * Factory Method: Reconstruir desde persistencia
     */
    public static Cliente reconstituir(ClienteId id, NombreCliente nombre, Nit nit, Email email,
                                       String telefono, String direccion, boolean activo,
                                       LocalDateTime fechaCreacion, LocalDateTime fechaActualizacion) {
        return new Cliente(id, nombre, nit, email, telefono, direccion, activo, fechaCreacion, fechaActualizacion);
    }

    private static void validarDatosCreacion(NombreCliente nombre, Nit nit, Email email) {
        if (nombre == null) {
            throw new IllegalArgumentException("El nombre del cliente es obligatorio");
        }
        if (nit == null) {
            throw new IllegalArgumentException("El NIT del cliente es obligatorio");
        }
        if (email == null) {
            throw new IllegalArgumentException("El email del cliente es obligatorio");
        }
    }

    /**
     * Comportamiento de dominio: Actualizar información del cliente
     */
    public void actualizar(NombreCliente nuevoNombre, Email nuevoEmail,
                          String nuevoTelefono, String nuevaDireccion) {
        if (nuevoNombre != null) {
            this.nombre = nuevoNombre;
        }
        if (nuevoEmail != null) {
            this.email = nuevoEmail;
        }
        this.telefono = nuevoTelefono;
        this.direccion = nuevaDireccion;
        this.fechaActualizacion = LocalDateTime.now();
    }

    /**
     * Comportamiento de dominio: Activar cliente
     */
    public void activar() {
        if (this.activo) {
            throw new IllegalStateException("El cliente ya está activo");
        }
        this.activo = true;
        this.fechaActualizacion = LocalDateTime.now();
    }

    /**
     * Comportamiento de dominio: Desactivar cliente
     */
    public void desactivar() {
        if (!this.activo) {
            throw new IllegalStateException("El cliente ya está inactivo");
        }
        this.activo = false;
        this.fechaActualizacion = LocalDateTime.now();
    }

    /**
     * Regla de negocio: Verificar si el cliente puede realizar operaciones
     */
    public boolean puedeRealizarOperaciones() {
        return this.activo;
    }

    // Getters sin setters (inmutabilidad controlada)
    public ClienteId getId() {
        return id;
    }

    public NombreCliente getNombre() {
        return nombre;
    }

    public Nit getNit() {
        return nit;
    }

    public Email getEmail() {
        return email;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public boolean isActivo() {
        return activo;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    // Método interno para establecer ID después de persistencia
    void setId(ClienteId id) {
        if (this.id != null && !this.id.esNuevo()) {
            throw new IllegalStateException("No se puede cambiar el ID de un cliente existente");
        }
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cliente cliente = (Cliente) o;
        return Objects.equals(id, cliente.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Cliente{" +
                "id=" + id +
                ", nombre=" + nombre +
                ", nit=" + nit +
                ", activo=" + activo +
                '}';
    }
}
