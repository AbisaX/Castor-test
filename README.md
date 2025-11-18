# Sistema de Facturación Castor

Plataforma empresarial de facturación basada en microservicios, con arquitectura hexagonal, DDD, orquestación Docker/Kubernetes y observabilidad integrada.

---

## Tabla de Contenidos
1. [Arquitectura General](#arquitectura-general)
2. [Tecnologías](#tecnologías)
3. [Estructura del Repositorio](#estructura-del-repositorio)
4. [Instalación y Ejecución](#instalación-y-ejecución)
5. [APIs y Flujos Principales](#apis-y-flujos-principales)
6. [Pruebas y Calidad](#pruebas-y-calidad)
7. [Observabilidad y Resiliencia](#observabilidad-y-resiliencia)
8. [Despliegue y Herramientas](#despliegue-y-herramientas)
9. [Roadmap y Próximos Pasos](#roadmap-y-próximos-pasos)

---

## Arquitectura General

```
┌──────────────────────┐       ┌──────────────────────────────┐
│ Client (Node.js CLI) │──────▶│ API Gateway (Spring Cloud)   │
└──────────────────────┘  HTTP ├────────────┬─────────────────┘
                               │            │
                               │            │
           ┌───────────────────▼─┐      ┌───▼────────────────┐
           │ clientes-service    │      │ facturacion-service │
           │ (Spring Boot / DDD) │      │ (Spring Boot / DDD) │
           └─────────────────────┘      └────────────────────┘
                     │                           │
                     │                           │
           ┌─────────▼────────┐        ┌─────────▼─────────┐
           │ PostgreSQL (CDC) │        │ Oracle (CDC + PL) │
           └──────────────────┘        └───────────────────┘

           ┌────────────────────────────────────────────────┐
           │ tax-calculator-service (FastAPI)               │
           └────────────────────────────────────────────────┘

Infraestructura compartida: Kafka + Debezium, Zipkin, SonarQube, Docker/K8s.
```

### Microservicios
- **clientes-service**: CRUD de clientes con value objects y validaciones defensivas.
- **facturacion-service**: emisión de facturas, integración con Oracle y servicios externos.
- **tax-calculator-service**: cálculo asincrónico de impuestos/descuentos.
- **api-gateway**: enrutamiento, rate limiting, circuit breakers y trazado distribuido.
- **client-nodejs**: cliente CLI que ejecuta pruebas end-to-end.

---

## Tecnologías

| Capa | Tecnologías |
|------|-------------|
| Backend Java | Spring Boot 3.2, Spring Cloud Gateway, Resilience4j, WebFlux |
| Python | FastAPI, Pydantic, Uvicorn, pytest |
| Front/CLI | Node.js 18, axios, colors |
| Bases de datos | PostgreSQL 15, Oracle XE 21 |
| Mensajería/CDC | Kafka, Zookeeper, Debezium, Schema Registry |
| Observabilidad | Micrometer, Zipkin, Actuator, Prometheus |
| Calidad | JUnit 5, Mockito, TestContainers, JaCoCo, pytest-cov, SonarQube |
| Infraestructura | Docker Compose, Kubernetes (manifiestos en `k8s/`), Makefile |

---

## Estructura del Repositorio

```
.
├── clientes-service/          # Microservicio de gestión de clientes
├── facturacion-service/       # Microservicio de facturas (Oracle)
├── tax-calculator-service/    # Microservicio Python
├── api-gateway/               # Spring Cloud Gateway
├── client-nodejs/             # Cliente CLI para pruebas end-to-end
├── scripts/                   # SQL para PostgreSQL/Oracle
├── debezium/                  # Conectores y scripts CDC
├── k8s/                       # Manifiestos Kubernetes
├── docker-compose.yml         # Plataforma completa
├── docker-compose-debezium.yml
├── FUNCIONALIDADES_SISTEMA.md # Documentación funcional central
├── GUIA_MICROSERVICIOS.md     # Guía técnica por servicio
├── QUICK_START.md             # Guía de inicio rápido
└── SONARQUBE_GUIDE.md         # Guía de análisis de calidad
```

---

## Instalación y Ejecución

Consulta `QUICK_START.md` para instrucciones detalladas. Resumen:

1. **Docker Compose**
   ```bash
   docker-compose up --build -d
   ```
   - API Gateway: `http://localhost:8080`
   - Clientes: `http://localhost:8081/swagger-ui.html`
   - Facturación: `http://localhost:8082/swagger-ui.html`
   - Zipkin: `http://localhost:9411`

2. **Kubernetes (Minikube)**
   - Construye imágenes (`clientes-service`, `facturacion-service`, `tax-calculator-service`, `api-gateway`).
   - Ejecuta `k8s/deploy-all.sh`.
   - Usa `k8s/validate-deployment.sh` para verificar.

3. **Desarrollo local**
   - Ejecuta los microservicios Java con `mvn spring-boot:run`.
   - Inicia FastAPI con `uvicorn main:app --reload`.
   - Corre el cliente Node con `npm start`.

---

## APIs y Flujos Principales

### Clientes Service (`http://localhost:8081/api/v1/clientes`)
- `POST /api/v1/clientes`
- `GET /api/v1/clientes/{id}`
- `GET /api/v1/clientes?page=0&size=20`
- `PUT /api/v1/clientes/{id}`
- `DELETE /api/v1/clientes/{id}`

### Facturación Service (`http://localhost:8082/api/v1/facturas`)
- `POST /api/v1/facturas`
- `GET /api/v1/facturas/{id}`
- `GET /api/v1/facturas?page=0&size=20`
- `GET /api/v1/facturas/cliente/{clienteId}`

### Tax Calculator (`http://localhost:5000`)
- `POST /calcular`
- `GET /health`

### API Gateway (`http://localhost:8080/api/v1/*`)
- Rutas `/clientes/**`, `/facturas/**`, `/tax-calculator/**` con filtros de resiliencia.

### Client Node.js
- Ejecuta automáticamente el flujo completo: crear cliente → obtener → listar → crear factura → validar respuestas → limpiar.

---

## Pruebas y Calidad

- **clientes-service**: 49 pruebas (dominio, REST, persistencia). Documentado en `clientes-service/TESTS-VALIDATION.md`.
- **facturacion-service**: 70 pruebas (MockMvc, WireMock, TestContainers Oracle). Ver `facturacion-service/TESTS-SUMMARY.md`.
- **tax-calculator-service**: `pytest` + cobertura >80%.
- **api-gateway**: pruebas de configuración y filtros incluidos en el proyecto.
- **client-nodejs**: smoke tests ejecutables via `npm start`.
- **SonarQube**: ver `SONARQUBE_GUIDE.md` para ejecutar `mvn ... sonar:sonar` en cada servicio Java.

**Makefile útil**:
```bash
make build         # Compila servicios Java
make test          # Pruebas de clientes y facturación
make python-test   # pytest (tax-calculator)
make client-test   # CLI Node.js
make docker-up     # docker-compose up -d
make k8s-deploy    # aplica manifiestos
make sonar         # envía análisis a SonarQube
```

---

## Observabilidad y Resiliencia

- **Resilience4j** en adaptadores REST del facturación-service y en filtros del API Gateway.
- **Micrometer + Zipkin** para trazabilidad distribuida (`MANAGEMENT_ZIPKIN_TRACING_ENDPOINT` ya configurado).
- **Actuator**: `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`.
- **Logging estructurado** en FastAPI y en el gateway (trace ID/Span ID).
- **Rate Limiting y Fallbacks** configurados en `api-gateway`.
- **Health checks Docker** para todos los contenedores (PostgreSQL, Oracle, Zipkin, servicios).

---

## Despliegue y Herramientas

- **Docker Compose** (`docker-compose*.yml`): entornos locales con bases de datos, Debezium, Zipkin, SonarQube y microservicios.
- **Debezium** (`debezium/`): scripts `deploy-connectors.*`, conectores JSON y SQL de inicialización.
- **Kubernetes** (`k8s/`): manifiestos por servicio, scripts `deploy-all.*` y `validate-deployment.*`.
- **Scripts SQL** (`scripts/`): inicialización de PostgreSQL y Oracle (incluye procedimiento `validar_cliente_activo`).
- **Documentación**: `FUNCIONALIDADES_SISTEMA.md`, `GUIA_MICROSERVICIOS.md`, `ARCHITECTURE.md`.

---

## Roadmap y Próximos Pasos

1. **Endpoints de estado en clientes-service** (`HEAD /api/v1/clientes/{id}` y `GET /api/v1/clientes/{id}/activo`) para alinear el `ClienteValidationAdapter`.
2. **Seguridad**: agregar autenticación/autorización (JWT) desde el API Gateway.
3. **Observabilidad avanzada**: dashboards Prometheus/Grafana.
4. **Automatización CI/CD**: ejecutar `make test`, `pytest`, `npm start` y SonarQube en pipelines.
5. **CDC productivo**: automatizar la publicación de conectores Debezium en ambientes compartidos.

---

## Referencias
- [FUNCIONALIDADES_SISTEMA.md](FUNCIONALIDADES_SISTEMA.md) – guía funcional completa.
- [GUIA_MICROSERVICIOS.md](GUIA_MICROSERVICIOS.md) – detalles técnicos por servicio.
- [QUICK_START.md](QUICK_START.md) – pasos para levantar el entorno.
- [SONARQUBE_GUIDE.md](SONARQUBE_GUIDE.md) – análisis de calidad.

Con esto tienes una vista completa del Sistema de Facturación Castor y todos los recursos necesarios para ejecutarlo, probarlo y extenderlo.
