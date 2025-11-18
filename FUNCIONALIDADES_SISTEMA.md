# Sistema de Facturación Castor – Funcionalidades y Arquitectura

## 1. Resumen General

- **Proyecto**: Plataforma empresarial de facturación para clientes corporativos.
- **Versión**: 1.0.0 (Noviembre 2025).
- **Arquitectura**: Microservicios basados en Domain‑Driven Design (DDD) y Arquitectura Hexagonal.
- **Lenguajes**: Java 17 (Spring Boot 3.2) y Python 3.11 (FastAPI).
- **Persistencia**: PostgreSQL para clientes y Oracle para facturas.
- **Orquestación**: Docker Compose para entornos locales y manifiestos Kubernetes para despliegues avanzados.
- **Observabilidad**: Resilience4j, Micrometer, Zipkin, Actuator, métricas Prometheus y logging estructurado.

---

## 2. Vista de Arquitectura

### 2.1 Microservicios Principales

| Servicio | Stack | Puerto | Responsabilidades | Repositorio |
|----------|-------|--------|-------------------|-------------|
| `clientes-service` | Spring Boot 3.2 (Java 17) | 8081 | Gestión completa de clientes (CRUD, activación/inactivación, paginación, validaciones) | `clientes-service/` |
| `facturacion-service` | Spring Boot 3.2 (Java 17) | 8082 | Creación y consulta de facturas, validación de clientes activos, integración con Oracle y servicios externos | `facturacion-service/` |
| `tax-calculator-service` | FastAPI (Python 3.11) | 5000 | Cálculo asincrónico de impuestos y descuentos para ítems de factura | `tax-calculator-service/` |
| `api-gateway` | Spring Cloud Gateway | 8080 | Punto de entrada único, ruteo, rate limiting, circuit breakers y trazabilidad | `api-gateway/` |
| `client-nodejs` | Node.js 18 | CLI | Cliente de pruebas funcionales end-to-end, ejecuta 8 flujos completos contra el gateway | `client-nodejs/` |

### 2.2 Componentes de Soporte

- **Bases de datos**: `postgres` (clientes) y `oracle` (facturas) con scripts versionados en `scripts/`.
- **Infra de resiliencia**: Kafka, Zookeeper, Schema Registry y Debezium (`debezium/`) para Change Data Capture.
- **Observabilidad**: Zipkin, SonarQube, Actuator y métricas Prometheus.
- **Kubernetes**: Manifiestos en `k8s/` para cada microservicio, bases de datos, Kafka y componentes de telemetría.

---

## 3. Detalle de Microservicios

### 3.1 Clientes Service (`clientes-service/`)

- **Dominio**: `Cliente` como agregado root, con value objects `ClienteId`, `NombreCliente`, `Nit`, `Email`.
- **Arquitectura Hexagonal**: puertos en `domain/port`, casos de uso en `application/service/ClienteService.java`, adaptadores REST en `infrastructure/adapter/in/rest` y persistencia JPA en `adapter/out/persistence`.
- **Endpoints expuestos**:
  - `POST /api/v1/clientes`
  - `GET /api/v1/clientes/{id}`
  - `GET /api/v1/clientes?page=0&size=20&sortBy=nombre`
  - `PUT /api/v1/clientes/{id}`
  - `DELETE /api/v1/clientes/{id}`
- **Características**:
  - Validaciones defensivas y límites configurables de paginación.
  - Documentación Swagger/OpenAPI (`@Operation`, `@Tag`) y Actuator habilitado.
  - ResilienceConfig (`infrastructure/config/ResilienceConfig.java`) lista para circuit breakers de cliente si se requieren dependencias externas.
- **Pruebas y calidad**:
  - 49 pruebas distribuidas entre dominio, REST y persistencia (ver `clientes-service/TESTS-VALIDATION.md`).
  - TestContainers con PostgreSQL, Mockito + AssertJ, JaCoCo >70%.
  - Dockerfile multi-stage con health check.

### 3.2 Facturación Service (`facturacion-service/`)

- **Dominio**: `Factura` e `ItemFactura` con value objects `FacturaId`, `NumeroFactura`, `Dinero`, `Cantidad`, `Porcentaje`.
- **Integraciones**:
  - `ClienteValidationAdapter` consulta `clientes-service` (`HEAD /api/v1/clientes/{id}` y `GET /api/v1/clientes/{id}/activo` planificados) con Resilience4j + caching.
  - `TaxCalculatorAdapter` consume `tax-calculator-service` mediante WebClient.
- **Endpoints expuestos**:
  - `POST /api/v1/facturas`
  - `GET /api/v1/facturas/{id}`
  - `GET /api/v1/facturas?page=0&size=20`
  - `GET /api/v1/facturas/cliente/{clienteId}`
- **Características**:
  - Persistencia Oracle via adaptadores en `infrastructure/adapter/out/persistence/oracle`.
  - Resilience4j (Circuit Breaker + Retry), cache, y Micrometer + Zipkin.
  - Scripts PL/SQL en `scripts/oracle/` (incluye `validar_cliente_activo`).
- **Pruebas**:
  - 70 pruebas (MockMvc, TestContainers Oracle, WireMock) documentadas en `facturacion-service/TESTS-SUMMARY.md`.
  - Scripts `run-tests.sh`/`.bat` para ejecutar unitarios, integración y cobertura.

### 3.3 Tax Calculator Service (`tax-calculator-service/`)

- **API**:
  - `POST /calcular`: calcula subtotal, impuestos, descuentos y total para cada ítem.
  - `GET /health` y `GET /` para health/info.
- **Características**:
  - Pydantic para validaciones (cantidad >0, porcentajes 0‑100, descripciones 1‑500 caracteres).
  - Logging estructurado, configuración con `config.py` (pydantic-settings), middleware CORS y GZip.
  - Dockerfile multi-stage y manifiestos en `k8s/python-service/`.
- **Testing**:
  - `pytest`, `pytest-cov`, `black`, `flake8`, `mypy` (ver `README.md` y `pytest.ini`).
  - Más de 80% de cobertura con `test_main.py` y `test_config.py`.

### 3.4 API Gateway (`api-gateway/`)

- **Funciones clave**:
  - Rutas `/api/v1/clientes/**`, `/api/v1/facturas/**`, `/api/v1/tax-calculator/**`.
  - Rate limiting (token bucket), circuit breakers, retries, timeouts por ruta.
  - Logging con trace IDs, métricas personalizadas (`gateway.requests.*`), trazas hacia Zipkin.
- **Documentación**:
  - Detalle de arquitectura en `api-gateway/ARCHITECTURE.md`, `API_ROUTES.md` y `PROJECT_STATUS.md`.
  - Prometheus scrape config (`prometheus.yml`) y scripts `run.*`.

### 3.5 Client Node.js (`client-nodejs/`)

- **Objetivo**: ejecutar smoke tests completos tras cada despliegue (crear cliente, leer, listar, crear factura, validar totales, etc.).
- **Ejecución**:
  - `npm start` para correr pruebas locales.
  - Scripts Docker (`npm run docker:build`, `npm run docker:run`, `npm run docker:run:network`).
- **Configuración**: `.env.example` define `API_URL` (por defecto `http://localhost:8080`).

---

## 4. Infraestructura Compartida

### 4.1 Docker Compose

- `docker-compose.yml` levanta PostgreSQL, Oracle, Zipkin, SonarQube, Kafka, Zookeeper, Schema Registry, Kafka Connect, Debezium UI, y todos los microservicios.
- Health checks configurados (`wget`/`curl`) permiten dependencias controladas.
- Variables sensibles parametrizadas via `SPRING_DATASOURCE_*`, `CLIENTE_SERVICE_BASE_URL`, `TAX_CALCULATOR_BASE_URL`, `MANAGEMENT_ZIPKIN_TRACING_ENDPOINT`, etc.

### 4.2 Change Data Capture (CDC)

- Carpeta `debezium/` con scripts de despliegue (`deploy-connectors.sh`, `register-connectors.*`), SQL auxiliares y definiciones JSON de conectores (PostgreSQL source, Oracle sink).
- `docker-compose-debezium.yml` orquesta el pipeline completo (bases de datos, Kafka, Connect, Debezium UI y microservicios).

### 4.3 Kubernetes

- `k8s/` incluye manifestos por componente: `clientes-service`, `facturacion-service`, `tax-calculator-service`, `api-gateway`, bases de datos, Kafka, Zipkin.
- Scripts `deploy-all.*` y `validate-deployment.*` automatizan despliegue/validación.
- `k8s/README.md` documenta requisitos, órdenes y comandos de diagnóstico.

### 4.4 Scripts SQL

- `scripts/postgres/init.sql`: tablas, índices, triggers y datos semilla para clientes.
- `scripts/oracle/*.sql`: tablas, secuencias, triggers, vistas y procedimiento `validar_cliente_activo`.
- Documentación resumida en `scripts/README.md`.

---

## 5. Observabilidad y Resiliencia

- **Resilience4j**: circuit breakers, retries y time limiters en adaptadores externos (`facturacion-service` y `api-gateway`).
- **Micrometer + Zipkin**: trazas distribuidas mediante `MANAGEMENT_ZIPKIN_TRACING_ENDPOINT`.
- **Actuator**: `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus` habilitados en los servicios Spring.
- **FastAPI**: métricas nativas y logging estructurado.
- **Rate Limiting y Fallbacks**: implementados en `api-gateway`.
- **Health checks Docker**: definidos para cada contenedor (bases de datos, servicios, Zipkin, etc.).

---

## 6. Testing y Calidad

| Servicio | Frameworks | Cobertura/Detalles |
|----------|------------|--------------------|
| `clientes-service` | JUnit 5, Mockito, AssertJ, TestContainers (PostgreSQL) | 49 pruebas, cobertura >70%, documentación en `TESTS-VALIDATION.md`. |
| `facturacion-service` | JUnit 5, MockMvc, TestContainers (Oracle), WireMock | 70 pruebas, cobertura >70%, ver `TESTS-SUMMARY.md`. |
| `tax-calculator-service` | pytest, pytest-cov, black, flake8, mypy | Cobertura >80%, validaciones completas. |
| `api-gateway` | Spring Boot tests (configurados en `pom.xml`) | Health checks y filtros cubiertos en pipelines CI/CD. |
| `client-nodejs` | Tests funcionales manuales via CLI | Ejecuta 8 flujos que validan la plataforma extremo a extremo. |

**SonarQube**: disponible en Docker Compose; las guías en `SONARQUBE_GUIDE.md` explican cómo enviar reportes desde `clientes-service`, `facturacion-service` y `api-gateway`.

---

## 7. Operación y Despliegue

### 7.1 Makefile

`make help` expone comandos para compilar, probar, analizar con SonarQube, levantar Docker Compose, desplegar en Kubernetes, ejecutar el cliente Node y correr tests Python.

### 7.2 Quick Start

`QUICK_START.md` describe tres caminos:
1. **Docker Compose**: `docker-compose up --build -d` y pruebas vía `client-nodejs`.
2. **Kubernetes**: Minikube + `k8s/deploy-all.*`.
3. **Desarrollo local**: ejecutar cada microservicio individualmente (Java, Python y Node).

### 7.3 Scripts auxiliares

- `run-tests.sh`/`.bat` en cada microservicio Java para ejecutar suites completas.
- `k8s/deploy-all.*` y `k8s/validate-deployment.*` para entornos cluster.
- `debezium/*.sh` para registrar y verificar conectores CDC.

---

## 8. Roadmap Inmediato

1. **Endpoints de estado en clientes-service**: exponer `HEAD /api/v1/clientes/{id}` y `GET /api/v1/clientes/{id}/activo` para alinearse con `ClienteValidationAdapter`.
2. **Seguridad**: incorporar autenticación/autorización (por ejemplo JWT) desde `api-gateway`.
3. **Observabilidad avanzada**: dashboards Prometheus/Grafana reutilizando métricas actuales.
4. **Automatización CI/CD**: ejecutar `make test`, `pytest`, `npm start` y análisis SonarQube en pipelines.
5. **CDC productivo**: automatizar despliegue de conectores Debezium en ambientes compartidos.

---

## 9. Conclusión

La versión 1.0.0 del Sistema de Facturación Castor entrega una plataforma completa con microservicios especializados, integración con bases de datos heterogéneas, resiliencia y observabilidad incorporadas, pruebas exhaustivas y herramientas listas para despliegue en Docker o Kubernetes. Toda la documentación técnica y operativa se centraliza en este archivo para reflejar las funcionalidades reales del sistema.
