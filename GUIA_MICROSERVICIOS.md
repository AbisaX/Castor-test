# Guía de Microservicios – Sistema de Facturación Castor

Esta guía resume la implementación técnica de cada componente, su estructura y comandos útiles para desarrollo y operación.

---

## Índice
1. [Clientes Service](#1-clientes-service)
2. [Facturación Service](#2-facturación-service)
3. [Tax Calculator Service](#3-tax-calculator-service)
4. [API Gateway](#4-api-gateway)
5. [Client Node.js](#5-client-nodejs)
6. [Infraestructura de soporte](#6-infraestructura-de-soporte)
7. [CDC con Debezium](#7-cdc-con-debezium)
8. [Kubernetes](#8-kubernetes)
9. [Comandos rápidos](#9-comandos-rápidos)

---

## 1. Clientes Service

- **Ubicación**: `clientes-service/`
- **Stack**: Spring Boot 3.2, Java 17, PostgreSQL, Resilience4j, Micrometer.
- **Arquitectura**:
  - Dominio puro (`domain/`) con `Cliente` como agregado root y value objects (`ClienteId`, `NombreCliente`, `Nit`, `Email`).
  - Puertos `domain/port/in` y `domain/port/out`.
  - Casos de uso en `application/service/ClienteService.java`.
  - Adaptadores REST (`infrastructure/adapter/in/rest`) con DTOs y mappers.
  - Persistencia JPA (`adapter/out/persistence`) mapeada a PostgreSQL.
  - Configuración (`infrastructure/config`) para OpenAPI, database y resiliencia.
- **Endpoints**:
  - `POST /api/v1/clientes`
  - `GET /api/v1/clientes/{id}`
  - `GET /api/v1/clientes?page=0&size=20`
  - `PUT /api/v1/clientes/{id}`
  - `DELETE /api/v1/clientes/{id}`
- **Características**:
  - Validaciones defensivas de paginación, longitud y formato.
  - Documentación OpenAPI vía anotaciones.
  - Actuator + Prometheus y trazas hacia Zipkin.
  - Dockerfile multi-stage con health check.
- **Pruebas**:
  - 49 pruebas documentadas en `TESTS-VALIDATION.md`.
  - Mockito + TestContainers (PostgreSQL) + JaCoCo.

---

## 2. Facturación Service

- **Ubicación**: `facturacion-service/`
- **Stack**: Spring Boot 3.2, Java 17, Oracle XE, WebClient, Resilience4j.
- **Arquitectura**:
  - Dominio (`domain/`) con `Factura`, `ItemFactura` y value objects (`FacturaId`, `NumeroFactura`, `Dinero`, `Porcentaje`, `Cantidad`).
  - Puertos en `domain/port` para repositorios y servicios externos.
  - Aplicación (`application/service/FacturaService`) para reglas de negocio.
  - Adaptadores:
    - REST: `infrastructure/adapter/in/rest` (DTOs, paginación, swagger).
    - Persistencia Oracle: `adapter/out/persistence/oracle` (mapper, entidades, repositorio).
    - Servicios externos: `ClienteValidationAdapter`, `TaxCalculatorAdapter` con Circuit Breaker + Retry.
  - Configuración (`infrastructure/config`) para WebClient, Resilience y atributos de cálculo.
- **Endpoints**:
  - `POST /api/v1/facturas`
  - `GET /api/v1/facturas/{id}`
  - `GET /api/v1/facturas`
  - `GET /api/v1/facturas/cliente/{clienteId}`
- **Integraciones**:
  - Consulta al `clientes-service` (pendientes endpoints `HEAD /clientes/{id}` y `GET /clientes/{id}/activo`).
  - Consumo del `tax-calculator-service` antes de persistir totales.
- **Pruebas**:
  - 70 pruebas (MockMvc, WireMock, TestContainers Oracle) documentadas en `TESTS-SUMMARY.md`.
  - Scripts `run-tests.sh/.bat` para ejecutar suites completas.

---

## 3. Tax Calculator Service

- **Ubicación**: `tax-calculator-service/`
- **Stack**: FastAPI, Python 3.11, Pydantic, pytest.
- **Características**:
  - Endpoints `POST /calcular`, `GET /health`, `GET /`.
  - Validaciones automáticas: cantidades >0, porcentajes 0‑100, descripciones 1‑500 caracteres.
  - Configuración centralizada en `config.py` (pydantic-settings).
  - Middleware CORS, GZip y logging estructurado.
  - Dockerfile multi-stage y manifiestos `k8s/python-service/`.
- **Testing**:
  - `pytest`, `pytest-cov`, `black`, `flake8`, `mypy`.
  - `pytest.ini` habilita coverage >80%.

---

## 4. API Gateway

- **Ubicación**: `api-gateway/`
- **Stack**: Spring Cloud Gateway, Resilience4j, Micrometer Tracing.
- **Rutas**:
  - `/api/v1/clientes/**` → `clientes-service`
  - `/api/v1/facturas/**` → `facturacion-service`
  - `/api/v1/tax-calculator/**` → `tax-calculator-service`
- **Filtros globales**:
  - Logging con trace IDs, métricas personalizadas (`gateway.requests.*`), autenticación básica (lista para ampliación).
  - Rate limiting (token bucket) por ruta.
  - Circuit breaker, retry y timeouts configurables mediante `application.yml`.
  - Fallbacks y respuestas amistosas cuando algún servicio está inactivo.
- **Observabilidad**:
  - Micrometer Tracing + Zipkin.
  - Endpoints Actuator (`/actuator/health`, `/actuator/metrics`, `/actuator/circuitbreakers`).
  - Configuración Prometheus (`prometheus.yml`).

---

## 5. Client Node.js

- **Ubicación**: `client-nodejs/`
- **Stack**: Node.js 18, axios, colors.
- **Uso**:
  - `npm start` para ejecutar 8 pruebas funcionales (crear cliente, leer, listar, crear factura, validar totales, etc.).
  - Scripts Docker (`npm run docker:*`) para correr la suite dentro de contenedores.
- **Configuración**:
  - `.env.example` expone `API_URL` (por defecto `http://localhost:8080`).
  - `Dockerfile` multi-stage y `README.md` con troubleshooting.

---

## 6. Infraestructura de soporte

| Componente | Ubicación | Descripción |
|------------|-----------|-------------|
| PostgreSQL | `docker-compose*.yml` / `scripts/postgres` | Base de datos para clientes (triggers, datos semilla, replicación lógica). |
| Oracle XE | `docker-compose*.yml` / `scripts/oracle` | Base para facturas con procedimientos PL/SQL (`validar_cliente_activo`). |
| Zipkin | Docker Compose & `k8s/zipkin` | Recepción de trazas desde servicios Spring. |
| SonarQube | Docker Compose | Análisis de calidad (ver `SONARQUBE_GUIDE.md`). |
| Kafka/Schema Registry | `docker-compose*.yml` | Transporte de eventos CDC. |
| Debezium | `debezium/` | Scripts y conectores para replicar cambios Postgres→Kafka→Oracle. |

---

## 7. CDC con Debezium

- `docker-compose-debezium.yml` levanta Postgres, Oracle, Kafka, Connect y microservicios.
- `debezium/postgres-setup.sql` y `debezium/oracle-setup.sql` configuran roles, slots y vistas necesarias.
- Scripts:
  - `deploy-connectors.sh` / `.bat`: despliega conectores.
  - `register-connectors.*`: registra el source Postgres y sink Oracle.
  - `check-connectors.*`: verifica estado.
- Conectores JSON:
  - `debezium/connectors/postgres-source-connector.json`
  - `debezium/connectors/oracle-sink-connector.json`

---

## 8. Kubernetes

- **Namespace**: `facturacion` (definido en `k8s/namespace.yaml`).
- **Manifiestos**: cada carpeta (`clientes-service/`, `facturacion-service/`, `tax-calculator-service/`, `api-gateway/`, `postgres/`, `oracle/`, `kafka/`, `zipkin/`) contiene Deployment, Service, ConfigMaps y Secrets necesarios.
- **Scripts**:
  - `k8s/deploy-all.sh` / `.bat`: despliegue completo.
  - `k8s/validate-deployment.sh` / `.bat`: verifica pods, services y endpoints.
- **Observabilidad**: se expone `api-gateway` vía Service tipo `ClusterIP` (usa port-forward o Ingress según necesidad).

---

## 9. Comandos rápidos

```bash
make build            # Compila clientes, facturación y gateway
make test             # Ejecuta pruebas Java
make python-test      # Ejecuta pytest
make client-test      # Corre el cliente Node.js
make docker-up        # Levanta todo con Docker Compose
make k8s-deploy       # Aplica manifiestos Kubernetes
make sonar            # Envía análisis a SonarQube (requiere SONAR_TOKEN)
```

Para más detalles funcionales revisa `FUNCIONALIDADES_SISTEMA.md`; para pasos rápidos, `QUICK_START.md`; y para calidad, `SONARQUBE_GUIDE.md`.
