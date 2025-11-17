-- ============================================================================
-- Script: oracle-setup.sql
-- Descripción: Configuración de Oracle para recibir datos replicados vía CDC
-- Base de datos: XE
-- Usuario: castor_facturacion
-- ============================================================================

-- Este script crea la tabla CLIENTES en Oracle para recibir datos
-- replicados desde PostgreSQL vía Debezium

-- Conectar como usuario de aplicación
CONNECT castor_facturacion/castor_pass@XE;

-- ============================================================================
-- 1. ELIMINAR TABLA SI EXISTE (OPCIONAL - SOLO PARA DESARROLLO)
-- ============================================================================

-- Descomentar si necesitas recrear la tabla desde cero
-- DROP TABLE CLIENTES CASCADE CONSTRAINTS;
-- DROP SEQUENCE CLIENTES_SEQ;

-- ============================================================================
-- 2. CREAR TABLA CLIENTES
-- ============================================================================

-- La estructura debe coincidir con la tabla en PostgreSQL
-- El conector JDBC Sink creará automáticamente la tabla si auto.create=true
-- pero es mejor crearla manualmente para tener control total

BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE CLIENTES_CDC CASCADE CONSTRAINTS';
    DBMS_OUTPUT.PUT_LINE('Tabla CLIENTES_CDC eliminada');
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -942 THEN
            RAISE;
        END IF;
        DBMS_OUTPUT.PUT_LINE('Tabla CLIENTES_CDC no existía');
END;
/

-- Crear tabla CLIENTES (estructura debe coincidir EXACTAMENTE con PostgreSQL)
CREATE TABLE CLIENTES_CDC (
    -- Campos principales (deben coincidir con PostgreSQL)
    ID NUMBER(19) NOT NULL,
    NOMBRE VARCHAR2(200) NOT NULL,
    NIT VARCHAR2(50) NOT NULL,
    EMAIL VARCHAR2(100) NOT NULL,
    TELEFONO VARCHAR2(20),
    DIRECCION VARCHAR2(255),
    ACTIVO NUMBER(1) DEFAULT 1,       -- BOOLEAN en PostgreSQL (0=false, 1=true)

    -- Campos de auditoría (coinciden con PostgreSQL)
    FECHA_CREACION TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    FECHA_ACTUALIZACION TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,

    -- Metadatos de CDC (opcional, agregados por Debezium)
    CDC_OPERATION VARCHAR2(10),       -- Tipo de operación: INSERT, UPDATE, DELETE
    CDC_TIMESTAMP NUMBER(19),         -- Timestamp del cambio en milisegundos

    -- Constraint
    CONSTRAINT PK_CLIENTES_CDC PRIMARY KEY (ID)
);

-- ============================================================================
-- 3. CREAR ÍNDICES
-- ============================================================================

-- Índice único en email para búsquedas rápidas
CREATE UNIQUE INDEX IDX_CLIENTES_CDC_EMAIL ON CLIENTES_CDC(EMAIL);

-- Índice único en NIT (debe ser único como en PostgreSQL)
CREATE UNIQUE INDEX IDX_CLIENTES_CDC_NIT ON CLIENTES_CDC(NIT);

-- Índice en nombre para búsquedas
CREATE INDEX IDX_CLIENTES_CDC_NOMBRE ON CLIENTES_CDC(NOMBRE);

-- Índice en activo para filtrar clientes activos
CREATE INDEX IDX_CLIENTES_CDC_ACTIVO ON CLIENTES_CDC(ACTIVO);

-- Índice en timestamp de CDC para auditoría
CREATE INDEX IDX_CLIENTES_CDC_TS ON CLIENTES_CDC(CDC_TIMESTAMP);

-- ============================================================================
-- 4. CREAR TRIGGERS PARA AUDITORÍA
-- ============================================================================

-- Trigger para actualizar FECHA_ACTUALIZACION automáticamente
CREATE OR REPLACE TRIGGER TRG_CLIENTES_CDC_UPDATE
BEFORE UPDATE ON CLIENTES_CDC
FOR EACH ROW
BEGIN
    :NEW.FECHA_ACTUALIZACION := CURRENT_TIMESTAMP;
END;
/

-- ============================================================================
-- 5. COMENTARIOS EN TABLA Y COLUMNAS
-- ============================================================================

COMMENT ON TABLE CLIENTES_CDC IS 'Tabla de clientes replicada desde PostgreSQL vía CDC/Debezium';

COMMENT ON COLUMN CLIENTES_CDC.ID IS 'ID único del cliente (desde PostgreSQL)';
COMMENT ON COLUMN CLIENTES_CDC.NOMBRE IS 'Nombre completo del cliente';
COMMENT ON COLUMN CLIENTES_CDC.NIT IS 'NIT del cliente (único)';
COMMENT ON COLUMN CLIENTES_CDC.EMAIL IS 'Email del cliente (único)';
COMMENT ON COLUMN CLIENTES_CDC.TELEFONO IS 'Teléfono de contacto';
COMMENT ON COLUMN CLIENTES_CDC.DIRECCION IS 'Dirección postal';
COMMENT ON COLUMN CLIENTES_CDC.ACTIVO IS 'Estado del cliente (1=activo, 0=inactivo)';
COMMENT ON COLUMN CLIENTES_CDC.FECHA_CREACION IS 'Fecha de creación del registro';
COMMENT ON COLUMN CLIENTES_CDC.FECHA_ACTUALIZACION IS 'Fecha de última actualización';
COMMENT ON COLUMN CLIENTES_CDC.CDC_OPERATION IS 'Tipo de operación CDC (INSERT/UPDATE/DELETE)';
COMMENT ON COLUMN CLIENTES_CDC.CDC_TIMESTAMP IS 'Timestamp de la operación CDC (epoch millis)';

-- ============================================================================
-- 6. OTORGAR PERMISOS
-- ============================================================================

-- El usuario castor_facturacion ya es el dueño de la tabla
-- Si necesitas dar acceso a otros usuarios:
-- GRANT SELECT, INSERT, UPDATE, DELETE ON CLIENTES TO otro_usuario;

-- ============================================================================
-- 7. CREAR VISTA PARA CONSULTAS SIMPLIFICADAS
-- ============================================================================

-- Vista que excluye los metadatos de CDC y muestra solo clientes activos
CREATE OR REPLACE VIEW V_CLIENTES_ACTIVOS AS
SELECT
    ID,
    NOMBRE,
    NIT,
    EMAIL,
    TELEFONO,
    DIRECCION,
    ACTIVO,
    FECHA_CREACION,
    FECHA_ACTUALIZACION
FROM CLIENTES_CDC
WHERE (CDC_OPERATION IS NULL OR CDC_OPERATION != 'DELETE')
AND ACTIVO = 1;

COMMENT ON VIEW V_CLIENTES_ACTIVOS IS 'Vista de clientes activos sin metadatos CDC';

-- Vista completa sin metadatos CDC
CREATE OR REPLACE VIEW V_CLIENTES AS
SELECT
    ID,
    NOMBRE,
    NIT,
    EMAIL,
    TELEFONO,
    DIRECCION,
    ACTIVO,
    FECHA_CREACION,
    FECHA_ACTUALIZACION
FROM CLIENTES_CDC
WHERE CDC_OPERATION IS NULL OR CDC_OPERATION != 'DELETE';

COMMENT ON VIEW V_CLIENTES IS 'Vista de todos los clientes sin metadatos CDC';

-- ============================================================================
-- 8. CREAR VISTA DE AUDITORÍA CDC
-- ============================================================================

-- Vista para ver el historial de cambios CDC
CREATE OR REPLACE VIEW V_CLIENTES_CDC_AUDIT AS
SELECT
    ID,
    NOMBRE,
    NIT,
    EMAIL,
    ACTIVO,
    CDC_OPERATION AS OPERACION,
    TO_TIMESTAMP('1970-01-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS') +
        NUMTODSINTERVAL(CDC_TIMESTAMP/1000, 'SECOND') AS FECHA_CAMBIO
FROM CLIENTES_CDC
WHERE CDC_TIMESTAMP IS NOT NULL
ORDER BY CDC_TIMESTAMP DESC;

COMMENT ON VIEW V_CLIENTES_CDC_AUDIT IS 'Vista de auditoría de cambios CDC en clientes';

-- ============================================================================
-- 9. PROCEDIMIENTOS ALMACENADOS ÚTILES
-- ============================================================================

-- Procedimiento para limpiar registros eliminados (soft delete)
CREATE OR REPLACE PROCEDURE SP_CLEAN_DELETED_CLIENTES(
    P_DAYS_OLD IN NUMBER DEFAULT 30
) AS
    V_DELETED_COUNT NUMBER;
BEGIN
    DELETE FROM CLIENTES_CDC
    WHERE CDC_OPERATION = 'DELETE'
    AND CDC_TIMESTAMP < (
        SELECT (EXTRACT(EPOCH FROM SYSTIMESTAMP) * 1000) - (P_DAYS_OLD * 24 * 60 * 60 * 1000)
        FROM DUAL
    );

    V_DELETED_COUNT := SQL%ROWCOUNT;
    COMMIT;

    DBMS_OUTPUT.PUT_LINE('Registros eliminados: ' || V_DELETED_COUNT);
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Error: ' || SQLERRM);
        RAISE;
END;
/

-- Procedimiento para obtener estadísticas de replicación
CREATE OR REPLACE PROCEDURE SP_CDC_STATS AS
    V_TOTAL NUMBER;
    V_ACTIVOS NUMBER;
    V_INACTIVOS NUMBER;
    V_INSERTS NUMBER;
    V_UPDATES NUMBER;
    V_DELETES NUMBER;
    V_LAST_CHANGE TIMESTAMP;
BEGIN
    SELECT COUNT(*) INTO V_TOTAL FROM CLIENTES_CDC;

    SELECT COUNT(*) INTO V_ACTIVOS
    FROM CLIENTES_CDC WHERE ACTIVO = 1 AND (CDC_OPERATION IS NULL OR CDC_OPERATION != 'DELETE');

    SELECT COUNT(*) INTO V_INACTIVOS
    FROM CLIENTES_CDC WHERE ACTIVO = 0 AND (CDC_OPERATION IS NULL OR CDC_OPERATION != 'DELETE');

    SELECT COUNT(*) INTO V_INSERTS
    FROM CLIENTES_CDC WHERE CDC_OPERATION = 'INSERT' OR CDC_OPERATION IS NULL;

    SELECT COUNT(*) INTO V_UPDATES
    FROM CLIENTES_CDC WHERE CDC_OPERATION = 'UPDATE';

    SELECT COUNT(*) INTO V_DELETES
    FROM CLIENTES_CDC WHERE CDC_OPERATION = 'DELETE';

    SELECT MAX(
        TO_TIMESTAMP('1970-01-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS') +
        NUMTODSINTERVAL(CDC_TIMESTAMP/1000, 'SECOND')
    ) INTO V_LAST_CHANGE
    FROM CLIENTES_CDC WHERE CDC_TIMESTAMP IS NOT NULL;

    DBMS_OUTPUT.PUT_LINE('=== ESTADÍSTICAS DE REPLICACIÓN CDC ===');
    DBMS_OUTPUT.PUT_LINE('Total de registros: ' || V_TOTAL);
    DBMS_OUTPUT.PUT_LINE('Clientes activos: ' || V_ACTIVOS);
    DBMS_OUTPUT.PUT_LINE('Clientes inactivos: ' || V_INACTIVOS);
    DBMS_OUTPUT.PUT_LINE('Inserts: ' || V_INSERTS);
    DBMS_OUTPUT.PUT_LINE('Updates: ' || V_UPDATES);
    DBMS_OUTPUT.PUT_LINE('Deletes: ' || V_DELETES);
    DBMS_OUTPUT.PUT_LINE('Último cambio: ' || TO_CHAR(V_LAST_CHANGE, 'YYYY-MM-DD HH24:MI:SS'));
END;
/

-- ============================================================================
-- 10. INSERTAR DATOS DE PRUEBA (OPCIONAL)
-- ============================================================================

-- Estos datos serán sobrescritos cuando lleguen los datos de PostgreSQL
-- Estructura debe coincidir con PostgreSQL
/*
INSERT INTO CLIENTES_CDC (ID, NOMBRE, NIT, EMAIL, TELEFONO, DIRECCION, ACTIVO, FECHA_CREACION, FECHA_ACTUALIZACION)
VALUES (1, 'Juan Pérez', '900123456-7', 'juan.perez@example.com', '+57 300 1234567', 'Calle 123 #45-67', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO CLIENTES_CDC (ID, NOMBRE, NIT, EMAIL, TELEFONO, DIRECCION, ACTIVO, FECHA_CREACION, FECHA_ACTUALIZACION)
VALUES (2, 'María García', '900654321-0', 'maria.garcia@example.com', '+57 310 7654321', 'Carrera 45 #12-34', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

COMMIT;
*/

-- ============================================================================
-- 11. VERIFICAR CREACIÓN
-- ============================================================================

-- Mostrar información de la tabla
SELECT
    TABLE_NAME,
    NUM_ROWS,
    BLOCKS,
    LAST_ANALYZED
FROM USER_TABLES
WHERE TABLE_NAME = 'CLIENTES_CDC';

-- Mostrar columnas de la tabla
SELECT
    COLUMN_NAME,
    DATA_TYPE,
    DATA_LENGTH,
    NULLABLE,
    COLUMN_ID
FROM USER_TAB_COLUMNS
WHERE TABLE_NAME = 'CLIENTES_CDC'
ORDER BY COLUMN_ID;

-- Mostrar índices de la tabla
SELECT
    INDEX_NAME,
    INDEX_TYPE,
    UNIQUENESS,
    STATUS
FROM USER_INDEXES
WHERE TABLE_NAME = 'CLIENTES_CDC';

-- ============================================================================
-- CONSULTAS ÚTILES PARA MONITOREO
-- ============================================================================

-- Ver últimos cambios replicados
/*
SELECT
    ID,
    NOMBRE,
    NIT,
    EMAIL,
    ACTIVO,
    CDC_OPERATION,
    TO_CHAR(
        TO_TIMESTAMP('1970-01-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS') +
        NUMTODSINTERVAL(CDC_TIMESTAMP/1000, 'SECOND'),
        'YYYY-MM-DD HH24:MI:SS'
    ) AS FECHA_CAMBIO
FROM CLIENTES_CDC
WHERE CDC_TIMESTAMP IS NOT NULL
ORDER BY CDC_TIMESTAMP DESC
FETCH FIRST 10 ROWS ONLY;
*/

-- Ver distribución de operaciones CDC
/*
SELECT
    NVL(CDC_OPERATION, 'ORIGINAL') AS OPERACION,
    COUNT(*) AS CANTIDAD
FROM CLIENTES_CDC
GROUP BY CDC_OPERATION
ORDER BY CANTIDAD DESC;
*/

-- Ver clientes activos vs inactivos
/*
SELECT
    CASE ACTIVO
        WHEN 1 THEN 'Activo'
        WHEN 0 THEN 'Inactivo'
        ELSE 'Desconocido'
    END AS ESTADO,
    COUNT(*) AS TOTAL_CLIENTES
FROM CLIENTES_CDC
WHERE CDC_OPERATION IS NULL OR CDC_OPERATION != 'DELETE'
GROUP BY ACTIVO
ORDER BY TOTAL_CLIENTES DESC;
*/

-- ============================================================================
-- NOTAS IMPORTANTES
-- ============================================================================

-- 1. El conector JDBC Sink puede crear automáticamente la tabla si
--    auto.create=true, pero es mejor crearla manualmente
--
-- 2. La estructura debe coincidir con PostgreSQL para evitar errores
--    de mapeo de tipos de datos
--
-- 3. Los campos CDC_* son opcionales y sirven para auditoría
--    Si no los necesitas, puedes omitirlos
--
-- 4. Oracle es case-sensitive para nombres entre comillas, pero
--    por defecto convierte todo a MAYÚSCULAS
--
-- 5. El conector usa UPSERT (INSERT o UPDATE) basado en la PRIMARY KEY
--    Asegúrate de que la PK coincida con PostgreSQL
--
-- 6. Para producción, considera particionamiento de la tabla si
--    esperas grandes volúmenes de datos
--
-- 7. Monitorea el rendimiento de los índices y ajusta según necesidad

-- ============================================================================
-- TROUBLESHOOTING
-- ============================================================================

-- Si el conector no puede insertar datos:
-- 1. Verifica que la tabla existe: SELECT * FROM USER_TABLES WHERE TABLE_NAME = 'CLIENTES_CDC';
-- 2. Verifica los permisos del usuario castor_facturacion
-- 3. Verifica que los tipos de datos coinciden EXACTAMENTE con PostgreSQL
-- 4. Revisa los logs del conector: docker logs castor-debezium-connect o kubectl logs deployment/debezium-connect
-- 5. Verifica que el nombre de la tabla en el conector coincide: table.name.format=CLIENTES_CDC

-- Si hay problemas de encoding de caracteres:
-- ALTER SESSION SET NLS_CHARACTERSET = 'AL32UTF8';

-- Si hay problemas de timezone:
-- ALTER SESSION SET TIME_ZONE = 'UTC';

-- ============================================================================

SET SERVEROUTPUT ON;

BEGIN
    DBMS_OUTPUT.PUT_LINE('==================================');
    DBMS_OUTPUT.PUT_LINE('Oracle CDC Setup completado');
    DBMS_OUTPUT.PUT_LINE('==================================');
    DBMS_OUTPUT.PUT_LINE('');
    DBMS_OUTPUT.PUT_LINE('Tabla CLIENTES_CDC creada exitosamente');
    DBMS_OUTPUT.PUT_LINE('Estructura: ID, NOMBRE, NIT, EMAIL, TELEFONO, DIRECCION, ACTIVO');
    DBMS_OUTPUT.PUT_LINE('Vistas creadas: V_CLIENTES, V_CLIENTES_ACTIVOS, V_CLIENTES_CDC_AUDIT');
    DBMS_OUTPUT.PUT_LINE('Procedimientos creados: SP_CLEAN_DELETED_CLIENTES, SP_CDC_STATS');
    DBMS_OUTPUT.PUT_LINE('');
    DBMS_OUTPUT.PUT_LINE('Siguiente paso:');
    DBMS_OUTPUT.PUT_LINE('  Docker: cd debezium && ./register-connectors.sh');
    DBMS_OUTPUT.PUT_LINE('  Kubernetes: kubectl port-forward svc/debezium-connect 8083:8083 -n facturacion');
    DBMS_OUTPUT.PUT_LINE('              cd debezium && ./register-connectors.sh');
    DBMS_OUTPUT.PUT_LINE('');
END;
/

-- Ejecutar estadísticas iniciales
EXEC SP_CDC_STATS;
