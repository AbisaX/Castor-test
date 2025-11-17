-- =====================================================
-- Script de inicialización para PostgreSQL
-- Base de datos de CLIENTES
-- =====================================================

-- Crear extensiones si son necesarias
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Eliminar tabla si existe (para desarrollo)
DROP TABLE IF EXISTS clientes CASCADE;

-- Crear tabla de clientes
CREATE TABLE clientes (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(200) NOT NULL,
    nit VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL,
    telefono VARCHAR(20),
    direccion VARCHAR(255),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$'),
    CONSTRAINT chk_nombre_not_empty CHECK (LENGTH(TRIM(nombre)) > 0),
    CONSTRAINT chk_nit_not_empty CHECK (LENGTH(TRIM(nit)) > 0)
);

-- Crear índices para mejorar el rendimiento
CREATE INDEX idx_clientes_nit ON clientes(nit);
CREATE INDEX idx_clientes_email ON clientes(email);
CREATE INDEX idx_clientes_activo ON clientes(activo);
CREATE INDEX idx_clientes_fecha_creacion ON clientes(fecha_creacion DESC);

-- Crear función para actualizar fecha_actualizacion automáticamente
CREATE OR REPLACE FUNCTION actualizar_fecha_modificacion()
RETURNS TRIGGER AS $$
BEGIN
    NEW.fecha_actualizacion = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Crear trigger para actualizar fecha_actualizacion
CREATE TRIGGER trigger_actualizar_fecha_modificacion
    BEFORE UPDATE ON clientes
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_fecha_modificacion();

-- Insertar datos de prueba
INSERT INTO clientes (nombre, nit, email, telefono, direccion, activo) VALUES
('Corporación ABC S.A.S.', '900111222-3', 'contacto@abc.com', '3001234567', 'Calle 100 #15-20, Bogotá', TRUE),
('Distribuidora XYZ Ltda.', '800222333-4', 'info@xyz.com', '3109876543', 'Carrera 7 #32-16, Medellín', TRUE),
('Comercializadora 123 S.A.', '900333444-5', 'ventas@123.com', '3207654321', 'Avenida 68 #45-67, Cali', TRUE),
('Servicios Tecnológicos DEF', '800444555-6', 'soporte@def.com', '3151234567', 'Calle 50 #23-45, Barranquilla', TRUE),
('Inversiones GHI S.A.S.', '900555666-7', 'admin@ghi.com', '3189876543', 'Carrera 15 #78-90, Cartagena', FALSE);

-- Verificar datos insertados
SELECT COUNT(*) as total_clientes FROM clientes;
SELECT * FROM clientes ORDER BY id;

-- Comentarios para documentación
COMMENT ON TABLE clientes IS 'Tabla que almacena la información de clientes corporativos';
COMMENT ON COLUMN clientes.id IS 'Identificador único del cliente (autoincremental)';
COMMENT ON COLUMN clientes.nit IS 'Número de Identificación Tributaria (único)';
COMMENT ON COLUMN clientes.activo IS 'Indica si el cliente está activo para operaciones';
COMMENT ON COLUMN clientes.fecha_creacion IS 'Fecha y hora de creación del registro';
COMMENT ON COLUMN clientes.fecha_actualizacion IS 'Fecha y hora de la última actualización';

-- Crear vista para clientes activos
CREATE OR REPLACE VIEW clientes_activos AS
SELECT
    id,
    nombre,
    nit,
    email,
    telefono,
    direccion,
    fecha_creacion
FROM clientes
WHERE activo = TRUE
ORDER BY nombre;

-- Permisos (ajustar según necesidades)
-- GRANT SELECT, INSERT, UPDATE, DELETE ON clientes TO app_user;
-- GRANT USAGE, SELECT ON SEQUENCE clientes_id_seq TO app_user;

COMMIT;

-- Mostrar resumen
SELECT 'PostgreSQL - Base de datos de CLIENTES inicializada correctamente' as mensaje;
SELECT
    COUNT(*) as total_clientes,
    SUM(CASE WHEN activo = TRUE THEN 1 ELSE 0 END) as clientes_activos,
    SUM(CASE WHEN activo = FALSE THEN 1 ELSE 0 END) as clientes_inactivos
FROM clientes;
