-- ============================================================================
-- Script: postgres-setup.sql
-- Descripción: Configuración de PostgreSQL para CDC con Debezium
-- Base de datos: facturacion_db
-- Usuario: castor_user
-- ============================================================================

-- Este script se ejecuta automáticamente al iniciar el contenedor PostgreSQL
-- si está montado en /docker-entrypoint-initdb.d/

-- ============================================================================
-- 1. VERIFICAR CONFIGURACIÓN DE REPLICACIÓN
-- ============================================================================

-- Verificar que wal_level está configurado correctamente
-- Debe ser 'logical' para que Debezium funcione
DO $$
BEGIN
    IF current_setting('wal_level') != 'logical' THEN
        RAISE NOTICE 'ADVERTENCIA: wal_level no está configurado como "logical"';
        RAISE NOTICE 'Para habilitar CDC, configura PostgreSQL con:';
        RAISE NOTICE '  wal_level = logical';
        RAISE NOTICE '  max_wal_senders = 10';
        RAISE NOTICE '  max_replication_slots = 10';
        RAISE NOTICE 'Ver docker-compose-debezium.yml para la configuración correcta';
    ELSE
        RAISE NOTICE 'wal_level configurado correctamente: %', current_setting('wal_level');
    END IF;
END $$;

-- ============================================================================
-- 2. CONFIGURAR REPLICA IDENTITY EN TABLA CLIENTES
-- ============================================================================

-- Replica Identity determina qué información se incluye en el WAL para updates/deletes
-- Opciones:
--   DEFAULT: Solo la clave primaria (por defecto)
--   FULL: Todos los campos (recomendado para CDC)
--   USING INDEX: Usar un índice específico
--   NOTHING: No replicar updates/deletes

\c facturacion_db;

-- Verificar si la tabla clientes existe
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = 'public'
               AND table_name = 'clientes') THEN

        -- Configurar REPLICA IDENTITY FULL para capturar todos los campos en updates/deletes
        ALTER TABLE public.clientes REPLICA IDENTITY FULL;

        RAISE NOTICE 'REPLICA IDENTITY configurado para tabla clientes';
        RAISE NOTICE 'Modo: FULL (todos los campos serán capturados en updates/deletes)';
    ELSE
        RAISE NOTICE 'ADVERTENCIA: La tabla public.clientes no existe todavía';
        RAISE NOTICE 'Ejecuta este comando después de crear la tabla:';
        RAISE NOTICE '  ALTER TABLE public.clientes REPLICA IDENTITY FULL;';
    END IF;
END $$;

-- ============================================================================
-- 3. CREAR USUARIO DE REPLICACIÓN (OPCIONAL)
-- ============================================================================

-- Si prefieres usar un usuario dedicado para Debezium (recomendado para producción)
-- Descomentar las siguientes líneas:

/*
-- Crear usuario específico para replicación
CREATE USER debezium_user WITH REPLICATION PASSWORD 'debezium_pass';

-- Otorgar permisos necesarios
GRANT CONNECT ON DATABASE facturacion_db TO debezium_user;
GRANT USAGE ON SCHEMA public TO debezium_user;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO debezium_user;
GRANT SELECT ON ALL SEQUENCES IN SCHEMA public TO debezium_user;

-- Otorgar permisos sobre tablas futuras
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT ON TABLES TO debezium_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT ON SEQUENCES TO debezium_user;

-- Otorgar permiso para crear slots de replicación
ALTER USER debezium_user WITH REPLICATION;

RAISE NOTICE 'Usuario debezium_user creado exitosamente';
RAISE NOTICE 'Actualiza postgres-connector.json con:';
RAISE NOTICE '  "database.user": "debezium_user"';
RAISE NOTICE '  "database.password": "debezium_pass"';
*/

-- ============================================================================
-- 4. INFORMACIÓN DE CONFIGURACIÓN
-- ============================================================================

-- Mostrar configuración actual relevante para CDC
SELECT
    name,
    setting,
    unit,
    context,
    short_desc
FROM pg_settings
WHERE name IN (
    'wal_level',
    'max_wal_senders',
    'max_replication_slots',
    'wal_sender_timeout',
    'max_slot_wal_keep_size'
)
ORDER BY name;

-- Mostrar slots de replicación existentes
SELECT
    slot_name,
    plugin,
    slot_type,
    database,
    active,
    restart_lsn
FROM pg_replication_slots;

-- Mostrar publicaciones existentes
SELECT
    pubname,
    puballtables,
    pubinsert,
    pubupdate,
    pubdelete,
    pubtruncate
FROM pg_publication;

-- ============================================================================
-- 5. COMANDOS ÚTILES PARA MONITOREO
-- ============================================================================

-- Ver la configuración de REPLICA IDENTITY de todas las tablas
/*
SELECT
    schemaname,
    tablename,
    CASE relreplident
        WHEN 'd' THEN 'DEFAULT (primary key)'
        WHEN 'n' THEN 'NOTHING'
        WHEN 'f' THEN 'FULL (all columns)'
        WHEN 'i' THEN 'INDEX'
    END AS replica_identity
FROM pg_class c
JOIN pg_namespace n ON n.oid = c.relnamespace
JOIN pg_tables t ON t.schemaname = n.nspname AND t.tablename = c.relname
WHERE schemaname = 'public'
ORDER BY tablename;
*/

-- Ver el tamaño del WAL
/*
SELECT
    pg_size_pretty(pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn)) AS replication_lag
FROM pg_replication_slots
WHERE slot_name = 'debezium_clientes_slot';
*/

-- Ver estadísticas de replicación
/*
SELECT * FROM pg_stat_replication;
*/

-- ============================================================================
-- NOTAS IMPORTANTES
-- ============================================================================

-- 1. Asegúrate de que PostgreSQL esté configurado con wal_level=logical
--    Ver docker-compose-debezium.yml para la configuración correcta
--
-- 2. La tabla debe tener una PRIMARY KEY para que Debezium pueda identificar
--    los registros de manera única
--
-- 3. REPLICA IDENTITY FULL captura todos los campos antes y después del cambio
--    Esto consume más espacio en el WAL pero proporciona información completa
--
-- 4. Los slots de replicación se crean automáticamente por Debezium
--    No es necesario crearlos manualmente
--
-- 5. Para producción, considera usar un usuario dedicado para Debezium
--    con permisos de solo lectura y replicación
--
-- 6. Monitorea el crecimiento del WAL regularmente para evitar problemas
--    de espacio en disco

-- ============================================================================
-- TROUBLESHOOTING
-- ============================================================================

-- Si Debezium no puede conectarse:
-- 1. Verifica que wal_level = logical
-- 2. Verifica que max_replication_slots >= 1
-- 3. Verifica que el usuario tiene permisos de replicación
-- 4. Verifica pg_hba.conf permite conexiones de replicación

-- Para eliminar un slot de replicación huérfano:
-- SELECT pg_drop_replication_slot('debezium_clientes_slot');

-- Para eliminar una publicación:
-- DROP PUBLICATION IF EXISTS dbz_publication;

-- ============================================================================

\echo '=================================='
\echo 'PostgreSQL CDC Setup completado'
\echo '=================================='
\echo ''
\echo 'Siguiente paso:'
\echo '  1. Inicia los servicios: docker-compose -f docker-compose-debezium.yml up -d'
\echo '  2. Despliega los conectores: cd debezium && ./deploy-connectors.sh'
\echo ''
