# Suite de Tests - Clientes Service

## Descripcion General

Suite completa de tests para el microservicio de gestion de clientes, implementada siguiendo las mejores practicas de testing en Spring Boot con arquitectura hexagonal y DDD.

## Estructura de Tests

```
src/test/
├── java/com/castor/clientes/
│   ├── application/service/
│   │   └── ClienteServiceTest.java              (13 tests unitarios)
│   └── infrastructure/
│       ├── adapter/in/rest/
│       │   └── ClienteControllerTest.java        (17 tests de API REST)
│       └── adapter/out/persistence/
│           └── ClienteRepositoryAdapterTest.java (19 tests de integracion)
└── resources/
    └── application-test.yml                      (Configuracion de tests)
```

**Total:** 49 tests | 1,372 lineas de codigo

## 1. ClienteServiceTest.java

### Descripcion
Tests unitarios para la capa de aplicacion (casos de uso) utilizando Mockito para aislar dependencias.

### Tecnologias
- **JUnit 5** (org.junit.jupiter)
- **Mockito 5.x** con @ExtendWith(MockitoExtension.class)
- **AssertJ** para assertions fluidas
- **BDDMockito** (given/when/then)

### Tests Implementados (13)
1. `testCrearCliente_Exitoso()` - Verifica creacion exitosa de cliente
2. `testCrearCliente_NitDuplicado_LanzaExcepcion()` - Valida unicidad de NIT
3. `testObtenerClientePorId_Existente()` - Busqueda exitosa por ID
4. `testObtenerClientePorId_NoExistente()` - Manejo de cliente inexistente
5. `testActualizarCliente_Exitoso()` - Actualizacion de datos
6. `testDesactivarCliente_Exitoso()` - Cambio de estado a inactivo
7. `testActivarCliente_Exitoso()` - Cambio de estado a activo
8. `testListarClientes_ConPaginacion()` - Paginacion de resultados
9. `testBuscarPorNit_Existente()` - Busqueda por NIT
10. `testEliminarCliente_Exitoso()` - Eliminacion exitosa
11. `testEliminarCliente_NoExistente_LanzaExcepcion()` - Error al eliminar inexistente
12. `testListarClientes_TamanioPaginaExcedido_LanzaExcepcion()` - Validacion de limite de paginacion
13. `testActualizarCliente_NitDuplicado_LanzaExcepcion()` - Validacion de NIT en actualizacion

### Patron AAA
Todos los tests siguen el patron Arrange-Act-Assert:
```java
// Arrange: Configurar mocks y datos de prueba
given(clienteRepository.existePorNit(nitValido)).willReturn(false);

// Act: Ejecutar el metodo bajo prueba
Cliente resultado = clienteService.crearCliente(clienteValido);

// Assert: Verificar el comportamiento esperado
assertThat(resultado).isNotNull();
then(clienteRepository).should().existePorNit(nitValido);
```

---

## 2. ClienteControllerTest.java

### Descripcion
Tests de API REST para el controlador, utilizando MockMvc para simular peticiones HTTP sin levantar el servidor completo.

### Tecnologias
- **@WebMvcTest** (Spring Boot Test Slice)
- **MockMvc** para simular peticiones HTTP
- **@MockBean** para ClienteUseCase
- **ObjectMapper** para serializar/deserializar JSON
- **Hamcrest Matchers** para validar respuestas JSON

### Tests Implementados (17)
1. `testCrearCliente_201Created()` - POST exitoso con codigo 201
2. `testCrearCliente_ValidacionFalla_400BadRequest()` - Validacion de campos obligatorios
3. `testCrearCliente_EmailInvalido_400BadRequest()` - Validacion de formato email
4. `testCrearCliente_NombreMuyCorto_400BadRequest()` - Validacion longitud minima
5. `testObtenerCliente_200Ok()` - GET exitoso con codigo 200
6. `testObtenerCliente_404NotFound()` - Error 404 para cliente inexistente
7. `testListarClientes_ConPaginacion_200Ok()` - GET de lista paginada
8. `testListarClientes_PaginacionInvalida_UsaDefaults()` - Valores por defecto
9. `testListarClientes_TamanioPaginaExcedido_AjustaAMaximo()` - Limite de paginacion
10. `testActualizarCliente_200Ok()` - PUT exitoso
11. `testActualizarCliente_404NotFound()` - Error 404 en actualizacion
12. `testActualizarCliente_DatosInvalidos_400BadRequest()` - Validacion en actualizacion
13. `testEliminarCliente_204NoContent()` - DELETE exitoso con codigo 204
14. `testEliminarCliente_404NotFound()` - Error 404 en eliminacion
15. `testListarClientes_DireccionOrdenamientoInvalida_UsaDefault()` - Validacion sort
16. `testListarClientes_PaginaNegativa_UsaDefault()` - Validacion pagina negativa
17. `testCrearCliente_NitMuyCorto_400BadRequest()` - Validacion NIT

### Validaciones de API REST
- Codigos HTTP correctos (200, 201, 204, 400, 404)
- Content-Type: application/json
- Estructura JSON de respuestas
- Validaciones de Bean Validation (@Valid)
- Manejo de errores

### Ejemplo de Test
```java
mockMvc.perform(post("/api/v1/clientes")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(requestValido)))
    .andDo(print())
    .andExpect(status().isCreated())
    .andExpect(jsonPath("$.id", is(1)))
    .andExpect(jsonPath("$.nombre", is("Acme Corporation S.A.")));
```

---

## 3. ClienteRepositoryAdapterTest.java

### Descripcion
Tests de integracion con base de datos real utilizando TestContainers para PostgreSQL 15, garantizando pruebas realistas de persistencia.

### Tecnologias
- **@DataJpaTest** (Spring Boot Test Slice para JPA)
- **@Testcontainers** con PostgreSQL 15
- **@Container** PostgreSQLContainer
- **EntityManager** para control transaccional
- **AssertJ** para assertions

### Configuracion TestContainers
```java
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
    .withDatabaseName("testdb")
    .withUsername("test")
    .withPassword("test");
```

### Tests Implementados (19)
1. `testGuardarCliente()` - Persistencia exitosa
2. `testBuscarPorId()` - Busqueda por ID
3. `testBuscarPorId_NoExistente()` - Optional.empty() para ID inexistente
4. `testBuscarPorNit()` - Busqueda por NIT
5. `testExistsByNit()` - Verificacion de existencia
6. `testActualizarCliente()` - Actualizacion de datos
7. `testEliminarCliente()` - Eliminacion fisica
8. `testListarTodos_ConPaginacion()` - Paginacion en base de datos
9. `testListarTodos_PaginaVacia()` - Lista vacia
10. `testBuscarPorId_IdNulo()` - Manejo de nulos
11. `testBuscarPorId_IdNuevo()` - Manejo de IDs sin persistir
12. `testBuscarPorNit_NitNulo()` - Manejo de NIT nulo
13. `testExistsByNit_NitNulo()` - Validacion con nulo
14. `testEliminar_IdNulo()` - No eliminar con ID nulo
15. `testEliminar_IdNuevo()` - No eliminar con ID nuevo
16. `testUnicidadNit()` - Constraint UNIQUE de NIT
17. `testPersistenciaFechas()` - Persistencia correcta de LocalDateTime

### Ventajas de TestContainers
- **Base de datos real:** PostgreSQL 15 Alpine
- **Aislamiento:** Contenedor nuevo por ejecucion
- **Realismo:** Valida constraints, indices, tipos de datos
- **CI/CD Ready:** Compatible con pipelines

### Limpieza de Datos
```java
@BeforeEach
void setUp() {
    jpaRepository.deleteAll();
    entityManager.flush();
    entityManager.clear();
}
```

---

## 4. application-test.yml

### Descripcion
Configuracion optimizada para ejecucion de tests con logging detallado y desactivacion de componentes innecesarios.

### Configuraciones Principales
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop  # Schema automatico
    show-sql: true           # Logging de SQL

logging:
  level:
    com.castor.clientes: DEBUG
    org.hibernate.SQL: DEBUG

resilience4j:
  retry:
    configs:
      default:
        maxAttempts: 1       # Desactivar reintentos en tests
```

---

## Ejecucion de Tests

### Todos los tests
```bash
mvn clean test
```

### Test especifico
```bash
mvn test -Dtest=ClienteServiceTest
mvn test -Dtest=ClienteControllerTest
mvn test -Dtest=ClienteRepositoryAdapterTest
```

### Con reporte de cobertura (JaCoCo)
```bash
mvn clean test jacoco:report
```
El reporte se genera en: `target/site/jacoco/index.html`

### Verificar cobertura minima (70%)
```bash
mvn clean verify
```

---

## Cobertura Esperada

### Objetivo: > 70% (configurado en pom.xml)

**Estimacion de cobertura por capa:**
- **ClienteService:** ~90% (13 tests unitarios completos)
- **ClienteController:** ~85% (17 tests de API)
- **ClienteRepositoryAdapter:** ~95% (19 tests de integracion)

**Total estimado:** ~90% de cobertura de codigo

---

## Mejores Practicas Implementadas

### 1. Patron AAA (Arrange-Act-Assert)
Todos los tests siguen esta estructura clara:
```java
void test() {
    // Arrange
    // Act
    // Assert
}
```

### 2. Nomenclatura Descriptiva
```java
@DisplayName("Debe crear cliente exitosamente cuando los datos son validos")
void testCrearCliente_Exitoso()
```

### 3. Given-When-Then (BDD)
```java
given(repository.existePorNit(nit)).willReturn(false);
// when
Cliente result = service.crearCliente(cliente);
// then
assertThat(result).isNotNull();
then(repository).should().existePorNit(nit);
```

### 4. Assertions Fluidas (AssertJ)
```java
assertThat(cliente)
    .isNotNull()
    .extracting("nombre", "nit")
    .containsExactly("Acme Corp", "900123456-7");
```

### 5. Aislamiento de Tests
- Tests unitarios con mocks (sin dependencias externas)
- Tests de integracion con TestContainers (base de datos aislada)
- `@BeforeEach` para datos frescos en cada test

### 6. Cobertura de Casos de Borde
- Valores nulos
- Valores vacios
- Valores fuera de rango
- Excepciones esperadas
- Estados invalidos

---

## Dependencias Necesarias (pom.xml)

Ya incluidas en el proyecto:
```xml
<!-- JUnit 5 -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>

<!-- Mockito -->
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>

<!-- AssertJ -->
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <scope>test</scope>
</dependency>

<!-- TestContainers -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
```

---

## Troubleshooting

### TestContainers no encuentra Docker
```bash
# Verificar que Docker esta corriendo
docker ps

# En Windows, asegurar que Docker Desktop esta activo
```

### Tests de integracion muy lentos
TestContainers inicia un contenedor PostgreSQL. Primera ejecucion sera lenta (descarga imagen). Ejecuciones posteriores seran mas rapidas.

### Errores de compilacion
```bash
# Limpiar y compilar
mvn clean compile
```

---

## Mantenimiento

### Agregar nuevos tests
1. Seguir el patron AAA
2. Usar @DisplayName descriptivo
3. Probar casos exitosos y casos de error
4. Mantener cobertura > 70%

### Actualizar datos de prueba
Modificar metodos `@BeforeEach setUp()` en cada clase de test.

---

## Contribucion

Al agregar nuevos tests:
1. Mantener consistencia con tests existentes
2. Documentar con comentarios y @DisplayName
3. Verificar que pasan todos los tests: `mvn clean test`
4. Verificar cobertura: `mvn jacoco:report`

---

## Contacto y Soporte

Para preguntas sobre los tests, consultar:
- Documentacion de JUnit 5: https://junit.org/junit5/
- Documentacion de Mockito: https://javadoc.io/doc/org.mockito/mockito-core/
- Documentacion de AssertJ: https://assertj.github.io/doc/
- Documentacion de TestContainers: https://www.testcontainers.org/
