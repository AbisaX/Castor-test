# Scripts de Inicialización de Bases de Datos

Este directorio contiene los scripts SQL para inicializar las bases de datos PostgreSQL y Oracle.

## PostgreSQL (Clientes)

### Ejecutar script manualmente

```bash
psql -U castor_user -d facturacion_db -f postgres/init.sql
```

### Ejecutar en Docker

```bash
docker exec -i postgres-container psql -U castor_user -d facturacion_db < postgres/init.sql
```

### Lo que hace el script

- Crea la tabla `clientes` con todos los campos necesarios
- Agrega constraints de validación (email válido, campos no vacíos)
- Crea índices para optimizar consultas
- Crea trigger para actualización automática de `fecha_actualizacion`
- Inserta 5 clientes de prueba (4 activos, 1 inactivo)
- Crea vista de `clientes_activos`

## Oracle (Facturas)

### Ejecutar script manualmente

```bash
sqlplus system/oracle@localhost:1521/ORCLCDB @oracle/init.sql
```

### Ejecutar en Docker

```bash
docker exec -i oracle-container sqlplus system/oracle@ORCLCDB @/scripts/init.sql
```

### Lo que hace el script

- Crea secuencias para autoincrementar IDs
- Crea la tabla `facturas` con validaciones
- Crea la tabla `items_factura` con relación a facturas
- Crea triggers para autoincrementar IDs
- **Crea procedimiento almacenado PL/SQL `validar_cliente_activo`**
- Crea función para calcular totales
- Inserta 2 facturas de prueba con items
- Crea vista `vista_facturas_detalle` para reportes

## Procedimiento Almacenado PL/SQL

### `validar_cliente_activo`

Este procedimiento valida si un cliente existe y está activo antes de crear una factura.

**Parámetros:**
- `p_cliente_id` (IN): ID del cliente a validar
- `p_es_activo` (OUT): 1 si el cliente es válido y activo, 0 en caso contrario

**Ejemplo de uso:**

```sql
DECLARE
    v_resultado NUMBER;
BEGIN
    validar_cliente_activo(1, v_resultado);
    IF v_resultado = 1 THEN
        DBMS_OUTPUT.PUT_LINE('Cliente válido');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Cliente no válido');
    END IF;
END;
```

## Notas Importantes

1. Los scripts están diseñados para desarrollo. En producción, elimine las instrucciones `DROP TABLE`.

2. El procedimiento PL/SQL incluye una simulación de validación. En producción, debería usar un DB_LINK para conectarse a PostgreSQL.

3. Todos los scripts incluyen datos de prueba para facilitar el testing.

4. Los scripts son idempotentes (pueden ejecutarse múltiples veces).

## Estructura de Datos

### PostgreSQL - Tabla `clientes`

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | BIGSERIAL | ID autoincremental |
| nombre | VARCHAR(200) | Nombre del cliente |
| nit | VARCHAR(50) | NIT único |
| email | VARCHAR(100) | Email válido |
| telefono | VARCHAR(20) | Teléfono |
| direccion | VARCHAR(255) | Dirección |
| activo | BOOLEAN | Estado del cliente |
| fecha_creacion | TIMESTAMP | Fecha de creación |
| fecha_actualizacion | TIMESTAMP | Última actualización |

### Oracle - Tabla `facturas`

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | NUMBER | ID de secuencia |
| cliente_id | NUMBER | Referencia al cliente |
| numero_factura | VARCHAR2(50) | Número único |
| fecha_emision | TIMESTAMP | Fecha de emisión |
| subtotal | NUMBER(15,2) | Subtotal sin impuestos |
| impuestos | NUMBER(15,2) | Total de impuestos |
| descuentos | NUMBER(15,2) | Total de descuentos |
| total | NUMBER(15,2) | Total final |
| estado | VARCHAR2(20) | Estado de la factura |

### Oracle - Tabla `items_factura`

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | NUMBER | ID de secuencia |
| factura_id | NUMBER | ID de la factura |
| descripcion | VARCHAR2(500) | Descripción del item |
| cantidad | NUMBER | Cantidad |
| precio_unitario | NUMBER(15,2) | Precio por unidad |
| porcentaje_impuesto | NUMBER(5,2) | % de impuesto |
| porcentaje_descuento | NUMBER(5,2) | % de descuento |
