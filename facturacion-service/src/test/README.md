# Suite de Tests - Facturacion Service

## Descripción

Suite completa de tests para el microservicio de facturación con cobertura >70%.

## Estructura de Tests

```
src/test/java/com/castor/facturacion/
├── application/service/
│   └── FacturaServiceTest.java                    (Tests unitarios - Mockito)
├── infrastructure/adapter/
│   ├── in/rest/
│   │   └── FacturaControllerTest.java            (Tests de API - MockMvc)
│   └── out/
│       ├── persistence/oracle/
│       │   └── FacturaRepositoryAdapterTest.java (Tests de integración - TestContainers)
│       └── external/
│           ├── ClienteValidationAdapterTest.java (Tests con WireMock)
│           └── TaxCalculatorAdapterTest.java     (Tests con WireMock)
└── resources/
    └── application-test.yml                       (Configuración para tests)
```

## Tecnologías Utilizadas

### Testing Frameworks
- **JUnit 5**: Framework principal de testing
- **Mockito**: Framework de mocking para tests unitarios
- **AssertJ**: Assertions fluidas y expresivas
- **Spring Boot Test**: Integración con Spring Boot

### Testing de Integración
- **TestContainers**: Containers Docker para tests de integración
  - Oracle XE 21 (gvenzl/oracle-xe:21-slim-faststart)
- **WireMock**: Mock de servicios HTTP externos
- **MockMvc**: Testing de controllers REST

## Tests por Componente

### 1. FacturaServiceTest (15 tests)
**Ubicación**: `application/service/FacturaServiceTest.java`

Tests unitarios del servicio de aplicación con Mockito:
- ✅ Creación de facturas con cliente activo
- ✅ Validación de cliente inactivo
- ✅ Validación de cliente no existente
- ✅ Validación de factura inválida
- ✅ Obtención de factura por ID
- ✅ Listado de facturas con paginación
- ✅ Listado de facturas por cliente
- ✅ Anulación de facturas
- ✅ Circuit breaker y fallback
- ✅ Cálculo de totales

**Características**:
- Usa `@ExtendWith(MockitoExtension.class)`
- Mocks de `FacturaRepositoryPort`, `ClienteValidationPort`, `TaxCalculatorPort`
- BDDMockito (given/when/then)
- AssertJ para assertions

### 2. FacturaControllerTest (15 tests)
**Ubicación**: `infrastructure/adapter/in/rest/FacturaControllerTest.java`

Tests de API REST con MockMvc:
- ✅ POST /api/v1/facturas - 201 Created
- ✅ Validación de entrada - 400 Bad Request
- ✅ GET /api/v1/facturas/{id} - 200 OK / 404 Not Found
- ✅ GET /api/v1/facturas - Paginación
- ✅ GET /api/v1/facturas/cliente/{id} - Facturas por cliente
- ✅ Paginación defensiva (page, size, sort)
- ✅ Validación de formato JSON (snake_case)

**Características**:
- Usa `@WebMvcTest(FacturaController.class)`
- MockMvc para simular peticiones HTTP
- Validación de status codes HTTP
- Validación de JSON responses

### 3. FacturaRepositoryAdapterTest (12 tests)
**Ubicación**: `infrastructure/adapter/out/persistence/oracle/FacturaRepositoryAdapterTest.java`

Tests de integración con Oracle usando TestContainers:
- ✅ Guardar factura con items
- ✅ Buscar factura por ID
- ✅ Buscar facturas por cliente
- ✅ Eliminar factura (cascade)
- ✅ Actualizar factura
- ✅ Paginación
- ✅ Facturas con múltiples items
- ✅ Constraint UNIQUE en número de factura
- ✅ Fecha de creación automática

**Características**:
- Usa `@DataJpaTest` y `@Testcontainers`
- Oracle XE 21 en Docker (gvenzl/oracle-xe:21-slim-faststart)
- Configuración dinámica con `@DynamicPropertySource`
- `spring.jpa.hibernate.ddl-auto=create-drop`

**Requisitos**:
- Docker instalado y corriendo
- Mínimo 4GB RAM disponibles para container

### 4. ClienteValidationAdapterTest (15 tests)
**Ubicación**: `infrastructure/adapter/out/external/ClienteValidationAdapterTest.java`

Tests con WireMock para simular servicio de clientes:
- ✅ Cliente activo retorna true
- ✅ Cliente inactivo retorna false
- ✅ Cliente no encontrado (404)
- ✅ Servicio no disponible (500)
- ✅ Timeout
- ✅ Verificación de existencia de cliente
- ✅ Múltiples clientes
- ✅ Respuesta con formato inválido
- ✅ Error de conexión

**Características**:
- WireMock Server en puerto dinámico
- Simulación de respuestas HTTP
- Testing de circuit breaker y fallback
- Testing de retry

### 5. TaxCalculatorAdapterTest (13 tests)
**Ubicación**: `infrastructure/adapter/out/external/TaxCalculatorAdapterTest.java`

Tests con WireMock para simular servicio Python de impuestos:
- ✅ Cálculo de impuestos y descuentos
- ✅ Servicio no disponible - fallback
- ✅ Timeout
- ✅ Respuesta vacía
- ✅ Múltiples items
- ✅ Verificación de request enviado
- ✅ Error de conexión
- ✅ JSON inválido
- ✅ Servicio lento dentro del timeout

**Características**:
- WireMock para simular API Python
- Testing de integración HTTP
- Validación de request/response JSON
- Testing de fallback methods

## Ejecutar Tests

### Todos los tests
```bash
mvn test
```

### Tests específicos
```bash
# Solo tests unitarios
mvn test -Dtest=FacturaServiceTest

# Solo tests de controller
mvn test -Dtest=FacturaControllerTest

# Solo tests de integración con Oracle
mvn test -Dtest=FacturaRepositoryAdapterTest

# Solo tests con WireMock
mvn test -Dtest=ClienteValidationAdapterTest,TaxCalculatorAdapterTest
```

### Con cobertura (JaCoCo)
```bash
mvn clean test jacoco:report
```

El reporte de cobertura se genera en: `target/site/jacoco/index.html`

## Configuración de Tests

### application-test.yml
Configuración específica para tests:
- Base de datos Oracle (TestContainers)
- URLs de servicios externos (WireMock)
- Circuit breaker configuración permisiva
- Retry con intentos reducidos
- Logging nivel DEBUG

### Propiedades Dinámicas
Los tests de integración usan `@DynamicPropertySource` para configurar dinámicamente:
- URL de base de datos Oracle (TestContainer)
- Credenciales de BD
- Configuración de Hibernate

## Métricas de Cobertura

### Objetivo: >70% de cobertura de líneas

**Cobertura esperada por capa**:
- Domain: 90% (lógica de negocio crítica)
- Application: 85% (servicios de aplicación)
- Infrastructure: 70% (adapters)

### Verificación de cobertura
```bash
mvn clean test jacoco:check
```

Falla el build si la cobertura es <70% según configuración en `pom.xml`.

## Troubleshooting

### TestContainers no arranca Oracle
**Problema**: Error al iniciar container Oracle

**Soluciones**:
1. Verificar que Docker está corriendo
2. Verificar RAM disponible (mínimo 4GB)
3. Verificar que puedes descargar imagen: `docker pull gvenzl/oracle-xe:21-slim-faststart`
4. Limpiar containers: `docker system prune -a`

### WireMock - Port already in use
**Problema**: Puerto ya está en uso

**Solución**: Los tests usan puertos dinámicos, verificar que `setUp()` y `tearDown()` se ejecutan correctamente.

### Tests lentos
**Problema**: Tests tardan mucho tiempo

**Soluciones**:
- TestContainers: Activar reuse en `~/.testcontainers.properties`:
  ```properties
  testcontainers.reuse.enable=true
  ```
- Ejecutar tests en paralelo:
  ```bash
  mvn test -T 4
  ```

## Best Practices

### Tests Unitarios (Mockito)
✅ Usar BDDMockito (given/when/then)
✅ Verificar interacciones con `then().should()`
✅ Usar AssertJ para assertions fluidas
✅ Nombrar tests descriptivamente: `test[Scenario]_[When]_[Then]`

### Tests de Integración
✅ Limpiar base de datos en `@BeforeEach`
✅ Usar transacciones para rollback automático
✅ Verificar persistencia con queries adicionales
✅ No depender del orden de ejecución

### Tests con WireMock
✅ Iniciar/detener server en `@BeforeEach/@AfterEach`
✅ Usar puertos dinámicos
✅ Verificar peticiones con `verify()`
✅ Simular errores y timeouts

## Continuous Integration

### GitHub Actions / Jenkins
```yaml
- name: Run tests
  run: mvn clean test

- name: Generate coverage report
  run: mvn jacoco:report

- name: Check coverage threshold
  run: mvn jacoco:check
```

### SonarQube
```bash
mvn clean verify sonar:sonar \
  -Dsonar.projectKey=facturacion-service \
  -Dsonar.host.url=http://localhost:9000
```

## Recursos Adicionales

- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [TestContainers Documentation](https://www.testcontainers.org/)
- [WireMock Documentation](https://wiremock.org/docs/)
- [AssertJ Documentation](https://assertj.github.io/doc/)

## Contacto y Soporte

Para preguntas o issues relacionados con los tests, contactar al equipo de QA o abrir un issue en el repositorio.
