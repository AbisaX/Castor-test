# Guía de SonarQube – Análisis de Calidad

Esta guía explica cómo ejecutar análisis de código y cobertura para los microservicios Java del Sistema de Facturación Castor.

## 1. Contenido
1. Configuración inicial
2. Ejecución de análisis
3. Visualización de reportes
4. Métricas clave
5. Quality Gates y buenas prácticas
6. Troubleshooting y ejemplos CI

---

## 2. Configuración Inicial

1. **Iniciar SonarQube**
   - *Docker Compose*: `docker-compose up -d sonarqube`
   - *Docker standalone*: `docker run -d --name sonarqube -p 9000:9000 -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true sonarqube:10-community`
2. **Acceder**
   - URL: `http://localhost:9000`
   - Usuario: `admin`
   - Contraseña inicial: `admin` (SonarQube solicitará cambiarla).
3. **Crear proyectos**
   - Crea un proyecto por microservicio Java (`clientes-service`, `facturacion-service`, `api-gateway`).
   - Usa nombres descriptivos (ej. `Castor - Clientes Service`).
4. **Generar token**
   - Dentro de cada proyecto selecciona **Set up → Locally → Generate token**.
   - Guarda el token (lo usarás en los comandos `mvn ... sonar:sonar`).

---

## 3. Ejecutar Análisis

### 3.1 Servicios Java (Spring Boot)

```bash
# Clientes Service
cd clientes-service
mvn clean verify sonar:sonar \
  -Dsonar.projectKey=clientes-service \
  -Dsonar.projectName="Castor - Clientes Service" \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=YOUR_TOKEN_HERE

# Facturación Service
cd ../facturacion-service
mvn clean verify sonar:sonar \
  -Dsonar.projectKey=facturacion-service \
  -Dsonar.projectName="Castor - Facturacion Service" \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=YOUR_TOKEN_HERE

# API Gateway
cd ../api-gateway
mvn clean verify sonar:sonar \
  -Dsonar.projectKey=api-gateway \
  -Dsonar.projectName="Castor - API Gateway" \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=YOUR_TOKEN_HERE
```

### 3.2 Cobertura sin SonarQube

```bash
cd clientes-service && mvn clean test jacoco:report
cd facturacion-service && mvn clean test jacoco:report
cd api-gateway && mvn clean test jacoco:report

# Python
cd tax-calculator-service && pytest --cov=. --cov-report=xml
```

Los reportes JaCoCo se generan en `target/site/jacoco/index.html`.

---

## 4. Ver Reportes

### 4.1 JaCoCo local

```bash
cd clientes-service && start target/site/jacoco/index.html   # Windows
cd facturacion-service && open target/site/jacoco/index.html # macOS
cd api-gateway && xdg-open target/site/jacoco/index.html     # Linux
```

JaCoCo muestra cobertura por paquete, clase, líneas cubiertas/no cubiertas y ramas ejecutadas.

### 4.2 Dashboard SonarQube

1. Ir a `http://localhost:9000`
2. Seleccionar el proyecto correspondiente (`Castor - Clientes Service`, etc.)
3. Revisar:
   - Quality Gate
   - Bugs / Vulnerabilities / Code Smells
   - Coverage y duplicaciones

---

## 5. Métricas Clave y Quality Gates

| Métrica | Objetivo | Descripción |
|---------|----------|-------------|
| Coverage | >70% | Porcentaje de líneas cubiertas. |
| Bugs / Vulnerabilities | 0 críticos | Deben resolverse antes de mezclar. |
| Code Smells | Reducir continuamente | Priorizar severidad alta y media. |
| Duplications | <3% | Mantener código limpio. |

**Quality Gate recomendado**:
- Cobertura global >70%
- Nuevo código con cobertura >80%
- Sin bugs ni vulnerabilidades críticas en nuevo código
- Debt Ratio <5%

---

## 6. Buenas Prácticas

1. Ejecuta `make test-coverage` antes de cada push importante.
2. Usa `make sonar` para enviar resultados de clientes, facturación y gateway (puedes extender el comando para otros servicios).
3. No uses `-DskipTests` cuando quieras publicar cobertura.
4. Mantén JaCoCo habilitado en todos los `pom.xml`.
5. Genera reportes XML (`jacoco.xml`) cuando uses pipelines CI/CD.

---

## 7. Ejemplo CI/CD (GitHub Actions)

```yaml
name: Castor Quality
on: [push, pull_request]
jobs:
  java-services:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        service: [clientes-service, facturacion-service, api-gateway]
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run tests + coverage
        run: |
          cd ${{ matrix.service }}
          mvn clean verify
  python-service:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-python@v4
        with:
          python-version: '3.11'
      - name: Run pytest
        run: |
          cd tax-calculator-service
          pip install -r requirements.txt
          pytest --cov=. --cov-report=xml
```

Agrega un paso adicional para ejecutar `mvn ... sonar:sonar` con el token almacenado en `SONAR_TOKEN`.

---

## 8. Troubleshooting

| Problema | Solución |
|----------|----------|
| SonarQube tarda en iniciar | Asegura 2GB de RAM, revisa `docker logs sonarqube`. |
| Cobertura 0% en SonarQube | Verifica `jacoco.xml` y la ruta configurada en `pom.xml`. |
| Token inválido | Regenera desde SonarQube y actualiza el comando/pipeline. |
| Error `OutOfMemoryError` | Ejecuta `MAVEN_OPTS="-Xmx2g"` antes de `mvn`. |
| Reporte JaCoCo no existe | Asegura ejecutar `mvn test jacoco:report` y no omitir tests. |

---

## 9. Recursos

- [Documentación SonarQube](https://docs.sonarqube.org/)
- [JaCoCo](https://www.jacoco.org/jacoco/trunk/doc/)
- [Resilience4j](https://resilience4j.readme.io/)
- [Guía de Makefile (`make help`)](Makefile)

Con estos pasos puedes mantener la calidad de los microservicios Castor bajo control y demostrar evidencia de cobertura y análisis continuo.
