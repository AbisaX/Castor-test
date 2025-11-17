-- =====================================================
-- Script de inicialización para Oracle
-- Base de datos de FACTURAS
-- =====================================================

-- Conectar como SYSTEM o usuario con privilegios
-- CONNECT system/oracle@ORCLCDB;

-- Eliminar tablas si existen (para desarrollo)
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE items_factura CASCADE CONSTRAINTS';
EXCEPTION
    WHEN OTHERS THEN NULL;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE facturas CASCADE CONSTRAINTS';
EXCEPTION
    WHEN OTHERS THEN NULL;
END;
/

-- Eliminar secuencias si existen
BEGIN
    EXECUTE IMMEDIATE 'DROP SEQUENCE seq_facturas';
EXCEPTION
    WHEN OTHERS THEN NULL;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'DROP SEQUENCE seq_items_factura';
EXCEPTION
    WHEN OTHERS THEN NULL;
END;
/

-- Crear secuencias
CREATE SEQUENCE seq_facturas
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

CREATE SEQUENCE seq_items_factura
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- Crear tabla de facturas
CREATE TABLE facturas (
    id NUMBER PRIMARY KEY,
    cliente_id NUMBER NOT NULL,
    numero_factura VARCHAR2(50) NOT NULL UNIQUE,
    fecha_emision TIMESTAMP NOT NULL,
    subtotal NUMBER(15,2) NOT NULL,
    impuestos NUMBER(15,2) NOT NULL,
    descuentos NUMBER(15,2) NOT NULL,
    total NUMBER(15,2) NOT NULL,
    estado VARCHAR2(20) NOT NULL,

    -- Constraints
    CONSTRAINT chk_subtotal_positivo CHECK (subtotal >= 0),
    CONSTRAINT chk_impuestos_positivo CHECK (impuestos >= 0),
    CONSTRAINT chk_descuentos_positivo CHECK (descuentos >= 0),
    CONSTRAINT chk_total_positivo CHECK (total >= 0),
    CONSTRAINT chk_estado_valido CHECK (estado IN ('EMITIDA', 'PAGADA', 'ANULADA', 'VENCIDA'))
);

-- Crear tabla de items de factura
CREATE TABLE items_factura (
    id NUMBER PRIMARY KEY,
    factura_id NUMBER NOT NULL,
    descripcion VARCHAR2(500) NOT NULL,
    cantidad NUMBER NOT NULL,
    precio_unitario NUMBER(15,2) NOT NULL,
    porcentaje_impuesto NUMBER(5,2) DEFAULT 0,
    porcentaje_descuento NUMBER(5,2) DEFAULT 0,

    -- Foreign Key
    CONSTRAINT fk_items_factura FOREIGN KEY (factura_id) REFERENCES facturas(id) ON DELETE CASCADE,

    -- Constraints
    CONSTRAINT chk_cantidad_positiva CHECK (cantidad > 0),
    CONSTRAINT chk_precio_positivo CHECK (precio_unitario > 0),
    CONSTRAINT chk_porcentaje_impuesto CHECK (porcentaje_impuesto >= 0 AND porcentaje_impuesto <= 100),
    CONSTRAINT chk_porcentaje_descuento CHECK (porcentaje_descuento >= 0 AND porcentaje_descuento <= 100)
);

-- Crear índices
CREATE INDEX idx_facturas_cliente ON facturas(cliente_id);
CREATE INDEX idx_facturas_numero ON facturas(numero_factura);
CREATE INDEX idx_facturas_fecha ON facturas(fecha_emision DESC);
CREATE INDEX idx_facturas_estado ON facturas(estado);
CREATE INDEX idx_items_factura ON items_factura(factura_id);

-- Crear triggers para autoincrementar IDs
CREATE OR REPLACE TRIGGER trg_facturas_id
BEFORE INSERT ON facturas
FOR EACH ROW
BEGIN
    IF :NEW.id IS NULL THEN
        SELECT seq_facturas.NEXTVAL INTO :NEW.id FROM DUAL;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_items_factura_id
BEFORE INSERT ON items_factura
FOR EACH ROW
BEGIN
    IF :NEW.id IS NULL THEN
        SELECT seq_items_factura.NEXTVAL INTO :NEW.id FROM DUAL;
    END IF;
END;
/

-- =====================================================
-- PROCEDIMIENTO ALMACENADO PL/SQL
-- Valida si un cliente existe y está activo
-- =====================================================

CREATE OR REPLACE PROCEDURE validar_cliente_activo (
    p_cliente_id IN NUMBER,
    p_es_activo OUT NUMBER
) AS
    v_count NUMBER;
    v_existe_cliente NUMBER;
BEGIN
    -- Log inicial
    DBMS_OUTPUT.PUT_LINE('Validando cliente ID: ' || p_cliente_id);

    -- Por defecto, asumir que no es válido
    p_es_activo := 0;

    -- Validar parámetros de entrada
    IF p_cliente_id IS NULL OR p_cliente_id <= 0 THEN
        DBMS_OUTPUT.PUT_LINE('Error: ID de cliente inválido');
        RETURN;
    END IF;

    -- Verificar si el cliente existe en PostgreSQL
    -- NOTA: En un escenario real, esto requeriría DB Link o llamada a servicio externo
    -- Para esta implementación, asumimos que si el cliente_id > 0, existe
    -- En producción, esto se validaría contra la base de datos PostgreSQL usando DB_LINK

    -- Simulación: consideramos válidos los clientes con ID entre 1 y 1000
    IF p_cliente_id > 0 AND p_cliente_id <= 1000 THEN
        p_es_activo := 1;
        DBMS_OUTPUT.PUT_LINE('Cliente validado: ID ' || p_cliente_id || ' está activo');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Cliente no válido: ID ' || p_cliente_id);
    END IF;

    -- En producción, el código sería algo como:
    /*
    SELECT COUNT(*)
    INTO v_existe_cliente
    FROM clientes@postgres_link
    WHERE id = p_cliente_id AND activo = 1;

    IF v_existe_cliente > 0 THEN
        p_es_activo := 1;
    END IF;
    */

EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Error en validación: ' || SQLERRM);
        p_es_activo := 0;
END validar_cliente_activo;
/

-- Crear función para calcular total de factura
CREATE OR REPLACE FUNCTION calcular_total_factura (
    p_factura_id IN NUMBER
) RETURN NUMBER AS
    v_total NUMBER(15,2);
BEGIN
    SELECT NVL(SUM(cantidad * precio_unitario), 0)
    INTO v_total
    FROM items_factura
    WHERE factura_id = p_factura_id;

    RETURN v_total;
END;
/

-- Insertar datos de prueba
INSERT INTO facturas (cliente_id, numero_factura, fecha_emision, subtotal, impuestos, descuentos, total, estado)
VALUES (1, 'FACT-20240101120000', SYSTIMESTAMP, 1000000, 190000, 50000, 1140000, 'EMITIDA');

INSERT INTO items_factura (factura_id, descripcion, cantidad, precio_unitario, porcentaje_impuesto, porcentaje_descuento)
VALUES (seq_facturas.CURRVAL, 'Computador Portátil', 2, 500000, 19, 5);

INSERT INTO facturas (cliente_id, numero_factura, fecha_emision, subtotal, impuestos, descuentos, total, estado)
VALUES (2, 'FACT-20240101130000', SYSTIMESTAMP, 500000, 95000, 25000, 570000, 'EMITIDA');

INSERT INTO items_factura (factura_id, descripcion, cantidad, precio_unitario, porcentaje_impuesto, porcentaje_descuento)
VALUES (seq_facturas.CURRVAL, 'Monitor 24 pulgadas', 1, 500000, 19, 5);

COMMIT;

-- Verificar datos insertados
SELECT COUNT(*) as total_facturas FROM facturas;
SELECT COUNT(*) as total_items FROM items_factura;

-- Crear vista para reporte de facturas
CREATE OR REPLACE VIEW vista_facturas_detalle AS
SELECT
    f.id as factura_id,
    f.numero_factura,
    f.cliente_id,
    f.fecha_emision,
    f.estado,
    i.id as item_id,
    i.descripcion,
    i.cantidad,
    i.precio_unitario,
    (i.cantidad * i.precio_unitario) as subtotal_item
FROM facturas f
LEFT JOIN items_factura i ON f.id = i.factura_id
ORDER BY f.fecha_emision DESC, i.id;

-- Comentarios para documentación
COMMENT ON TABLE facturas IS 'Tabla que almacena las facturas emitidas a los clientes';
COMMENT ON TABLE items_factura IS 'Detalle de los items/productos de cada factura';
COMMENT ON COLUMN facturas.numero_factura IS 'Número único de la factura (formato: FACT-YYYYMMDDHHMMSS)';
COMMENT ON COLUMN facturas.estado IS 'Estado de la factura: EMITIDA, PAGADA, ANULADA, VENCIDA';

-- Mostrar resumen
SELECT 'Oracle - Base de datos de FACTURAS inicializada correctamente' as mensaje FROM DUAL;

SELECT
    COUNT(*) as total_facturas,
    SUM(CASE WHEN estado = 'EMITIDA' THEN 1 ELSE 0 END) as facturas_emitidas,
    SUM(total) as monto_total
FROM facturas;

-- Probar procedimiento almacenado
DECLARE
    v_resultado NUMBER;
BEGIN
    validar_cliente_activo(1, v_resultado);
    DBMS_OUTPUT.PUT_LINE('Resultado validación cliente 1: ' || v_resultado);

    validar_cliente_activo(9999, v_resultado);
    DBMS_OUTPUT.PUT_LINE('Resultado validación cliente 9999: ' || v_resultado);
END;
/

-- Habilitar DBMS_OUTPUT para ver los mensajes
SET SERVEROUTPUT ON;
