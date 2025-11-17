# Clientes Service

Microservicio de gestión de clientes con **arquitectura hexagonal** y **Domain-Driven Design (DDD)**.

## Características

- **Arquitectura Hexagonal** (Ports & Adapters)
- **Domain-Driven Design** (DDD)
  - Value Objects inmutables
  - Aggregates puros sin anotaciones de framework
  - Bounded Context bien definido
- **Spring Boot 3.2.0**
- **Paginación** con validación defensiva
- **Swagger/OpenAPI** - Documentación interactiva
- **Resilience4j** - Circuit Breaker, Retry, Time Limiter
- **Micrometer + Zipkin** - Trazabilidad distribuida
- **PostgreSQL** - Base de datos
- **JUnit + Mockito + TestContainers** - Testing completo
- **JaCoCo** - Cobertura de código >70%

## Estructura del Proyecto

```
clientes-service/
├── src/main/java/com/castor/clientes/
│   ├── domain/                    # Capa de dominio (DDD)
│   │   ├── Cliente.java          # Aggregate Root
│   │   ├── valueobject/          # Value Objects
│   │   │   ├── ClienteId.java
│   │   │   ├── NombreCliente.java
│   │   │   ├── Nit.java
│   │   │   └── Email.java
│   │   └── port/
│   │       ├── in/               # Puertos de entrada (Use Cases)
│   │       └── out/              # Puertos de salida (Repositories)
│   ├── application/              # Capa de aplicación
│   │   └── service/
│   │       └── ClienteService.java
│   └── infrastructure/           # Capa de infraestructura
│       ├── adapter/
│       │   ├── in/rest/         # REST Controllers, DTOs
│       │   └── out/persistence/ # JPA Repositories, Entities
│       ├── config/              # Configuración
│       └── exception/           # Manejo de excepciones
└── ClientesServiceApplication.java
```

## Instalación

```bash
# Compilar
mvn clean package

# Ejecutar tests
mvn test

# Generar reporte de cobertura
mvn jacoco:report

# Análisis con SonarQube
mvn sonar:sonar
```

## Ejecución

### Modo Desarrollo

```bash
mvn spring-boot:run
```

### Con Docker

```bash
# Construir imagen
docker build -t clientes-service:1.0.0 .

# Ejecutar contenedor
docker run -p 8081:8081 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/facturacion_db \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=postgres123 \
  clientes-service:1.0.0
```

## Documentación API

Una vez ejecutando, acceder a:

- **Swagger UI**: http://localhost:8081/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8081/api-docs
- **Actuator Health**: http://localhost:8081/actuator/health
- **Metrics**: http://localhost:8081/actuator/metrics
- **Prometheus**: http://localhost:8081/actuator/prometheus

## Endpoints

### POST /api/v1/clientes

Crear nuevo cliente.

**Request:**
```json
{
  "nombre": "Acme Corporation S.A.",
  "nit": "900123456-7",
  "email": "contacto@acme.com",
  "telefono": "+57 310 1234567",
  "direccion": "Calle 123 #45-67, Bogotá",
  "activo": true
}
```

**Response:** `201 Created`
```json
{
  "id": 1,
  "nombre": "Acme Corporation S.A.",
  "nit": "900123456-7",
  "email": "contacto@acme.com",
  "telefono": "+57 310 1234567",
  "direccion": "Calle 123 #45-67, Bogotá",
  "activo": true,
  "fechaCreacion": "2024-01-15T10:30:00",
  "fechaActualizacion": "2024-01-15T10:30:00"
}
```

### GET /api/v1/clientes?page=0&size=20

Listar clientes con paginación.

**Query Parameters:**
- `page`: Número de página (default: 0)
- `size`: Tamaño de página, máximo 100 (default: 20)
- `sortBy`: Campo para ordenar (default: id)
- `sortDirection`: ASC o DESC (default: ASC)

**Response:** `200 OK`
```json
{
  "content": [...],
  "totalElements": 50,
  "totalPages": 3,
  "size": 20,
  "number": 0
}
```

### GET /api/v1/clientes/{id}

Obtener cliente por ID.

**Response:** `200 OK`

### PUT /api/v1/clientes/{id}

Actualizar cliente.

**Response:** `200 OK`

### DELETE /api/v1/clientes/{id}

Eliminar cliente.

**Response:** `204 No Content`

## Validaciones

- **Nombre**: 3-200 caracteres
- **NIT**: 9-15 caracteres, solo números y guiones
- **Email**: Formato válido RFC 5322
- **Teléfono**: Máximo 20 caracteres (opcional)
- **Dirección**: Máximo 255 caracteres (opcional)
- **NIT único**: No puede haber dos clientes con el mismo NIT

## Resiliencia

### Circuit Breaker

- **Ventana**: 10 llamadas
- **Umbral de falla**: 50%
- **Tiempo en abierto**: 10 segundos
- **Llamadas en semi-abierto**: 3

### Retry

- **Intentos máximos**: 3
- **Tiempo de espera**: 500ms
- **Backoff exponencial**: 2x

### Time Limiter

- **Timeout**: 3 segundos

## Variables de Entorno

| Variable | Default | Descripción |
|----------|---------|-------------|
| `SERVER_PORT` | 8081 | Puerto del servicio |
| `SPRING_DATASOURCE_URL` | jdbc:postgresql://localhost:5432/facturacion_db | URL de PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | postgres | Usuario de BD |
| `SPRING_DATASOURCE_PASSWORD` | postgres123 | Contraseña de BD |
| `MANAGEMENT_ZIPKIN_TRACING_ENDPOINT` | http://localhost:9411/api/v2/spans | Endpoint de Zipkin |

## Testing

```bash
# Tests unitarios
mvn test

# Tests de integración con TestContainers
mvn verify

# Cobertura de código
mvn jacoco:report
# Ver reporte en: target/site/jacoco/index.html
```

## Domain-Driven Design

### Value Objects

El dominio usa Value Objects inmutables para garantizar invariantes:

```java
// Crear cliente con Value Objects
NombreCliente nombre = NombreCliente.of("Acme Corp");
Nit nit = Nit.of("900123456-7");
Email email = Email.of("contacto@acme.com");

Cliente cliente = Cliente.crear(nombre, nit, email, telefono, direccion);
```

### Comportamientos de Dominio

```java
// Activar/desactivar cliente
cliente.activar();
cliente.desactivar();

// Verificar si puede operar
if (cliente.puedeRealizarOperaciones()) {
    // ...
}

// Actualizar información
cliente.actualizar(nuevoNombre, nuevoEmail, nuevoTelefono, nuevaDireccion);
```

## Arquitectura Hexagonal

- **Dominio**: Lógica de negocio pura, sin dependencias externas
- **Puertos**: Interfaces que definen contratos
  - `ClienteUseCase`: Puerto de entrada (casos de uso)
  - `ClienteRepositoryPort`: Puerto de salida (persistencia)
- **Adaptadores**: Implementaciones concretas
  - `ClienteController`: Adaptador REST de entrada
  - `ClienteRepositoryAdapter`: Adaptador JPA de salida

## Próximos Pasos

- [ ] Implementar autenticación JWT
- [ ] Agregar eventos de dominio
- [ ] Implementar CQRS
- [ ] Cache con Redis
- [ ] Rate limiting por cliente

## Licencia

Desarrollado para Castor - Sistema de Facturación
