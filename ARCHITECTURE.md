# Arquitectura del Sistema de Facturación Castor

Esta guía describe cómo se organiza la solución en microservicios, cómo se aplica Arquitectura Hexagonal y cómo interactúan los componentes de infraestructura y observabilidad.

---

## 1. Visión Global

```
Clientes (CLI / Integraciones)
          │ HTTP/JSON
          ▼
┌──────────────────────────────┐
│ API Gateway (Spring Cloud)   │
│ - Rate limiting              │
│ - Circuit breaker / Retry    │
│ - Tracing + Metrics          │
└────────────┬─────────────────┘
             │
 ┌───────────┼────────────┐
 │           │            │
 ▼           ▼            ▼
Clientes   Facturación   Tax Calculator
Service    Service       (FastAPI)
└─────┘    └───────┘     └─────────┘
  │           │             │
  │           │             │
PostgreSQL   Oracle      (stateless)
  │           │
  └───────────┴───── Kafka + Debezium (CDC opcional)

Observabilidad: Zipkin + Actuator + Micrometer
Calidad: SonarQube + JaCoCo + pytest-cov
```

---

## 2. Principios Clave

1. **Domain-Driven Design**: Cada bounded context (Clientes y Facturación) vive en su propio microservicio con agregados, value objects y puertos.
2. **Arquitectura Hexagonal**: Dominio independiente, puertos definen contratos, adaptadores implementan detalles (REST, persistencia, servicios externos).
3. **Estrategia Políglota**: Java 17 para servicios de negocio, Python 3.11 para cálculo especializado, Node.js 18 para cliente de pruebas.
4. **Bases de datos dedicadas**: PostgreSQL para clientes, Oracle para facturas. Integración cruzada vía HTTP y, opcionalmente, Debezium.
5. **Resiliencia y Observabilidad** desde el diseño: circuit breakers, retries, logging estructurado, Zipkin, métricas Prometheus.

---

## 3. Detalle por Capa

### 3.1 Dominio (Clientes/Facturación)

- Entidades y value objects se ubican en `domain/` y `domain/valueobject/`.
- No existen anotaciones de framework en el dominio (solo Java puro).
- Reglas de negocio:
  - Clientes: activación/inactivación, validación de NIT, paginación acotada.
  - Facturas: 1..100 ítems, cálculo de totales, verificación de cliente activo.

### 3.2 Puertos (Ports)

- **Entrada (`domain/port/in`)**: define casos de uso (`ClienteUseCase`, `FacturaUseCase`).
- **Salida (`domain/port/out`)**: repositorios y servicios externos (`ClienteRepositoryPort`, `ClienteValidationPort`, `TaxCalculatorPort`).

### 3.3 Aplicación

- `application/service` implementa los puertos de entrada orquestando entidades y adaptadores.
- Gestiona transacciones, validaciones y reglas cross-cutting (p.ej. paginación).

### 3.4 Infraestructura

- **Adaptadores REST**: controladores (`Controller`), DTOs y mappers.
- **Adaptadores de persistencia**: JPA/Hibernate para PostgreSQL y Oracle.
- **Adaptadores externos**: WebClient para comunicarse con otros servicios (clientes-service y tax-calculator).
- **Configuración**: Beans de Resilience4j, WebClient, OpenAPI, caches, etc.

---

## 4. Microservicios

### 4.1 Clientes Service

- Arquitectura Hexagonal completa.
- Exposición REST con validaciones `@Validated`.
- ResilienceConfig preparado para llamadas externas futuras.
- Observabilidad via Actuator + Zipkin.

### 4.2 Facturación Service

- Aplicación de DDD + Hexagonal.
- Adaptadores con Circuit Breaker, Retry, cache y fallback.
- Persistencia Oracle con mappers y repositorios específicos.
- Integración con `clientes-service` (estado del cliente) y `tax-calculator-service`.

### 4.3 Tax Calculator Service

- FastAPI + Pydantic + Decimal para cálculos determinísticos.
- Independiente de bases de datos (stateless).
- Documentación automática (Swagger/ReDoc).

### 4.4 API Gateway

- Spring Cloud Gateway como BFF/API Gateway.
- Filtros globales: logging, métricas, trazado, rate limiting.
- Filtros por ruta: circuit breaker, retry, timeout, fallback.
- Configuración externa para URLs de backend (`CLIENTES_SERVICE_URL`, etc.).

### 4.5 Client Node.js

- CLI de validación end-to-end.
- Útil para smoke tests después de cada despliegue.

---

## 5. Infraestructura y Datos

### 5.1 Bases de datos

- **PostgreSQL**: tablas `clientes`, triggers de auditoría, vistas `clientes_activos`.
- **Oracle**: tablas `facturas`, `items_factura`, procedimientos PL/SQL (validación de cliente activo), secuencias y vistas de reporte.

### 5.2 CDC (Cambios en tiempo real)

- Debezium captura cambios en PostgreSQL (`wal_level=logical`).
- Kafka + Connect + Schema Registry transportan eventos hacia Oracle u otros consumidores.
- Scripts y conectores en `debezium/`.

### 5.3 Observabilidad

- Zipkin recibe spans desde servicios Spring y desde el Gateway.
- Actuator expone métricas (`/actuator/metrics`, `/actuator/prometheus`).
- FastAPI expone logs estructurados y health checks.

---

## 6. Despliegue

### 6.1 Docker Compose

- `docker-compose.yml` levanta bases de datos, Kafka, Debezium, Zipkin, SonarQube, y todos los servicios.
- Health checks (`wget`/`curl`) aseguran que dependencias estén listas antes de iniciar servicios de negocio.
- `docker-compose-debezium.yml` agrega scripts y volúmenes de CDC.

### 6.2 Kubernetes

- Manifiestos en `k8s/` para cada componente (Deployments, Services, ConfigMaps, Secrets).
- Scripts `deploy-all.*` y `validate-deployment.*`.
- Recomendación: usar Minikube o clusters con al menos 8GB RAM para entornos locales.

### 6.3 Makefile

`make build`, `make test`, `make docker-up`, `make k8s-deploy`, `make sonar`, etc. automatizan tareas comunes.

---

## 7. Seguridad y Roadmap

- **Actual**: servicios abiertos para facilitar pruebas locales.
- **Pendiente**:
  1. Autenticación (JWT) y autorización en el API Gateway.
  2. Endpoints `HEAD /api/v1/clientes/{id}` y `GET /api/v1/clientes/{id}/activo` para alinear `ClienteValidationAdapter`.
  3. Dashboards Prometheus/Grafana.
  4. Automatización CI/CD (ejecución de `make test`, `pytest`, `npm start`, SonarQube).

---

## 8. Referencias

- [FUNCIONALIDADES_SISTEMA.md](FUNCIONALIDADES_SISTEMA.md) – descripción funcional consolidada.
- [GUIA_MICROSERVICIOS.md](GUIA_MICROSERVICIOS.md) – detalle técnico por servicio.
- [QUICK_START.md](QUICK_START.md) – pasos para levantar la plataforma.
- [SONARQUBE_GUIDE.md](SONARQUBE_GUIDE.md) – análisis de calidad.

Esta arquitectura permite evolucionar cada microservicio de forma independiente, mantener dominios limpios y agregar capacidades (seguridad, orquestación, CDC) sin sacrificar mantenibilidad.
