# Debezium CDC (Change Data Capture)

## Descripción

Este directorio contiene la configuración de **Debezium** para implementar **Change Data Capture (CDC)** entre PostgreSQL y Oracle. El sistema captura cambios en tiempo real de la tabla `clientes` en PostgreSQL y los replica automáticamente a Oracle.

## Arquitectura

```
PostgreSQL (clientes)  →  Debezium Source  →  Kafka  →  Debezium Sink  →  Oracle (clientes_cdc)
```

### Componentes

1. **Zookeeper**: Coordinación para Kafka
2. **Kafka**: Message broker que almacena los eventos de cambio
3. **Debezium Connect**: Plataforma de conectores CDC
4. **PostgreSQL Source Connector**: Captura cambios de PostgreSQL
5. **Oracle JDBC Sink Connector**: Escribe cambios en Oracle

## Estructura de Archivos

```
debezium/
├── connectors/
│   ├── postgres-source-connector.json  # Configuración del conector de origen
│   └── oracle-sink-connector.json      # Configuración del conector de destino
├── register-connectors.sh              # Script de registro (Linux/Mac)
├── register-connectors.bat             # Script de registro (Windows)
└── README.md                           # Esta documentación
```

## Requisitos Previos

1. PostgreSQL configurado con replicación lógica (`wal_level=logical`)
2. Tabla `clientes` existente en PostgreSQL
3. Usuario Oracle con permisos para crear tablas
4. Docker y Docker Compose instalados

## Instalación y Configuración

### 1. Levantar el Stack Completo

```bash
# Desde el directorio raíz del proyecto
docker-compose up -d
```

Esto levantará:
- PostgreSQL (puerto 5432)
- Oracle (puerto 1521)
- Zookeeper (puerto 2181)
- Kafka (puertos 9092, 29092)
- Debezium Connect (puerto 8083)
- Servicios de aplicación

### 2. Verificar que Debezium Connect está Listo

```bash
# Verificar estado
curl http://localhost:8083/

# Listar plugins disponibles
curl http://localhost:8083/connector-plugins
```

### 3. Registrar los Conectores

**Linux/Mac:**
```bash
cd debezium
chmod +x register-connectors.sh
./register-connectors.sh
```

**Windows:**
```cmd
cd debezium
register-connectors.bat
```

El script automáticamente:
- Espera a que Debezium Connect esté listo
- Registra el conector de origen (PostgreSQL)
- Registra el conector de destino (Oracle)
- Muestra el estado de cada conector

## Configuración de Conectores

### PostgreSQL Source Connector

**Archivo:** `connectors/postgres-source-connector.json`

**Características principales:**
- Captura cambios de la tabla `public.clientes`
- Usa el plugin `pgoutput` (nativo de PostgreSQL 10+)
- Crea una publicación llamada `dbz_publication`
- Usa el slot de replicación `debezium_slot`
- Modo snapshot inicial: captura datos existentes al inicio
- Transforma los eventos para extraer solo el nuevo estado del registro

**Tópico Kafka generado:**
- `castor.public.clientes`

### Oracle JDBC Sink Connector

**Archivo:** `connectors/oracle-sink-connector.json`

**Características principales:**
- Lee eventos del tópico `castor.public.clientes`
- Modo insert: `upsert` (INSERT o UPDATE según la clave primaria)
- Manejo de deletes habilitado
- Crea automáticamente la tabla `CLIENTES_CDC` en Oracle
- Renombra campos a mayúsculas (convención Oracle)
- Batch size: 100 registros
- Reintentos: 10 con backoff de 3 segundos

## Monitoreo

### Ver Conectores Registrados

```bash
curl http://localhost:8083/connectors
```

### Estado de un Conector

```bash
# PostgreSQL Source
curl http://localhost:8083/connectors/postgres-clientes-source/status

# Oracle Sink
curl http://localhost:8083/connectors/oracle-clientes-sink/status
```

Respuesta esperada:
```json
{
  "name": "postgres-clientes-source",
  "connector": {
    "state": "RUNNING",
    "worker_id": "debezium-connect:8083"
  },
  "tasks": [
    {
      "id": 0,
      "state": "RUNNING",
      "worker_id": "debezium-connect:8083"
    }
  ]
}
```

### Listar Tópicos de Kafka

```bash
docker exec castor-kafka kafka-topics --bootstrap-server localhost:9092 --list
```

Tópicos esperados:
- `castor.public.clientes` (datos de clientes)
- `debezium_configs` (configuración de Debezium)
- `debezium_offsets` (posiciones de lectura)
- `debezium_statuses` (estados de conectores)

### Ver Mensajes en un Tópico

```bash
docker exec castor-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic castor.public.clientes \
  --from-beginning \
  --max-messages 10
```

### Ver Logs de Debezium Connect

```bash
docker logs castor-debezium-connect -f
```

## Verificación de Funcionamiento

### 1. Insertar Cliente en PostgreSQL

```sql
-- Conectarse a PostgreSQL
psql -h localhost -U castor_user -d facturacion_db

-- Insertar cliente
INSERT INTO clientes (nombre, nit, email, telefono, direccion, activo, fecha_creacion, fecha_actualizacion)
VALUES ('Test CDC', '900111222-3', 'cdc@test.com', '3001112233', 'Calle Test', true, NOW(), NOW());
```

### 2. Verificar en Kafka

```bash
# Ver el último mensaje
docker exec castor-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic castor.public.clientes \
  --from-beginning \
  --max-messages 1
```

### 3. Verificar en Oracle

```sql
-- Conectarse a Oracle
sqlplus castor_facturacion/castor_pass@localhost:1521/XE

-- Verificar datos replicados
SELECT * FROM CLIENTES_CDC WHERE EMAIL = 'cdc@test.com';
```

## Troubleshooting

### Conector no se inicia

```bash
# Ver detalles del error
curl http://localhost:8083/connectors/postgres-clientes-source/status

# Ver logs
docker logs castor-debezium-connect --tail 100
```

**Problemas comunes:**
1. PostgreSQL no tiene `wal_level=logical` configurado
2. Tabla no existe en PostgreSQL
3. Permisos insuficientes en Oracle
4. Kafka no está listo

### Reiniciar un Conector

```bash
# Pausar
curl -X PUT http://localhost:8083/connectors/postgres-clientes-source/pause

# Reanudar
curl -X PUT http://localhost:8083/connectors/postgres-clientes-source/resume

# Reiniciar
curl -X POST http://localhost:8083/connectors/postgres-clientes-source/restart
```

### Eliminar y Re-registrar un Conector

```bash
# Eliminar
curl -X DELETE http://localhost:8083/connectors/postgres-clientes-source

# Re-registrar
curl -X POST -H "Content-Type: application/json" \
  --data @connectors/postgres-source-connector.json \
  http://localhost:8083/connectors
```

### Resetear Slot de Replicación PostgreSQL

Si el slot de replicación se corrompe o necesita reiniciarse:

```sql
-- Conectarse a PostgreSQL como superusuario
psql -h localhost -U castor_user -d facturacion_db

-- Ver slots activos
SELECT * FROM pg_replication_slots;

-- Eliminar slot (solo si el conector está detenido)
SELECT pg_drop_replication_slot('debezium_slot');
```

Luego re-registrar el conector source.

### Limpiar Tópicos de Kafka

```bash
# Eliminar un tópico específico
docker exec castor-kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --delete \
  --topic castor.public.clientes

# Listar para verificar
docker exec castor-kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --list
```

## Consideraciones de Producción

### Seguridad

1. **Credenciales**: Usar secrets de Docker/Kubernetes en lugar de texto plano
2. **Encriptación**: Configurar SSL/TLS para Kafka y Debezium Connect
3. **Autenticación**: Habilitar SASL para Kafka

### Alta Disponibilidad

1. **Kafka**: Configurar múltiples brokers (replication factor > 1)
2. **Debezium Connect**: Ejecutar múltiples workers en modo distribuido
3. **Zookeeper**: Ensemble de 3+ nodos

### Performance

1. **Batch Size**: Ajustar `batch.size` según volumen de datos
2. **Paralelismo**: Incrementar `tasks.max` para mayor throughput
3. **Compresión**: Habilitar compresión en Kafka (`compression.type=gzip`)

### Retención de Datos

```bash
# Configurar retención de tópico (7 días)
docker exec castor-kafka kafka-configs \
  --bootstrap-server localhost:9092 \
  --entity-type topics \
  --entity-name castor.public.clientes \
  --alter \
  --add-config retention.ms=604800000
```

## Recursos Adicionales

- [Debezium Documentation](https://debezium.io/documentation/)
- [PostgreSQL Connector](https://debezium.io/documentation/reference/stable/connectors/postgresql.html)
- [JDBC Sink Connector](https://debezium.io/documentation/reference/stable/connectors/jdbc.html)
- [Kafka Connect](https://kafka.apache.org/documentation/#connect)

## Soporte

Para problemas o preguntas:
1. Revisar logs de Debezium Connect
2. Verificar estado de conectores
3. Consultar documentación oficial de Debezium
4. Revisar issues en el repositorio del proyecto
