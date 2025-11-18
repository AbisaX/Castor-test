# Guía de Inicio Rápido

Pon en marcha toda la plataforma en minutos siguiendo cualquiera de estas rutas. Todas las opciones asumen que estás en la raíz del repositorio `Castor/`.

---

## Opción 1 · Docker Compose (recomendada)

### Requisitos
- Docker Desktop 4.x+
- Docker Compose v2

### Pasos

```bash
# 1. Clonar el repositorio
git clone <repository-url>
cd Castor

# 2. Construir y levantar todo
docker-compose up --build -d

# 3. Verificar el estado (en otra terminal)
docker-compose ps

# 4. Esperar a que Oracle, Kafka y Zipkin estén saludables (2-4 min)
docker-compose logs -f oracle zipkin kafka
```

### Pruebas rápidas

```bash
# Probar API Gateway
curl http://localhost:8080/actuator/health

# Crear cliente
curl -X POST http://localhost:8080/api/v1/clientes \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Empresa Demo","nit":"900999888-1","email":"demo@empresa.com"}'

# Listar facturas
curl http://localhost:8080/api/v1/facturas?page=0&size=10
```

### Cliente Node.js (smoke tests)

```bash
cd client-nodejs
npm install
npm start
```

### URLs importantes
- API Gateway: `http://localhost:8080`
- Clientes Service: `http://localhost:8081/swagger-ui.html`
- Facturación Service: `http://localhost:8082/swagger-ui.html`
- Tax Calculator: `http://localhost:5000/docs`
- Zipkin: `http://localhost:9411`
- SonarQube: `http://localhost:9000` (admin/admin)

Para detener todo: `docker-compose down` o `docker-compose down -v` si quieres limpiar volúmenes.

---

## Opción 2 · Kubernetes con Minikube

### Requisitos
- Minikube 1.31+ con al menos 8GB RAM
- kubectl 1.28+
- Docker o container runtime compatible

### Pasos

```bash
# 1. Iniciar Minikube con recursos suficientes
minikube start --cpus=4 --memory=8192 --addons=ingress

# 2. Construir imágenes dentro de Minikube
eval $(minikube docker-env)
docker build -t clientes-service:latest ./clientes-service
docker build -t facturacion-service:latest ./facturacion-service
docker build -t tax-calculator-service:latest ./tax-calculator-service
docker build -t api-gateway:latest ./api-gateway

# 3. Desplegar todos los manifiestos
cd k8s
./deploy-all.sh        # o deploy-all.bat en Windows

# 4. Monitorear
kubectl get pods -n facturacion -w

# 5. Exponer API Gateway (si no usas ingress)
kubectl port-forward svc/api-gateway 8080:8080 -n facturacion
```

### Validar

```bash
curl http://localhost:8080/api/v1/clientes
kubectl get svc -n facturacion
```

Para desmontar: `./validate-deployment.sh --delete` o `kubectl delete -f k8s -R`.

---

## Opción 3 · Desarrollo local por servicio

### Requisitos
- Java 17 + Maven 3.9.x
- Python 3.11 + pip
- Node.js 18 + npm
- Docker (para bases de datos)

### Pasos sugeridos

```bash
# 1. Bases de datos locales
docker run -d --name postgres-dev \
  -e POSTGRES_DB=facturacion_db \
  -e POSTGRES_USER=castor_user \
  -e POSTGRES_PASSWORD=castor_pass \
  -p 5432:5432 postgres:15-alpine

docker run -d --name oracle-dev \
  -e ORACLE_PASSWORD=oracle \
  -e APP_USER=castor_facturacion \
  -e APP_USER_PASSWORD=castor_pass \
  -p 1521:1521 gvenzl/oracle-xe:21-slim-faststart

# 2. Ejecutar scripts SQL
psql -h localhost -U castor_user -d facturacion_db -f scripts/postgres/init.sql
# Para Oracle usa sqlplus o Docker exec con scripts/oracle/*

# 3. Iniciar servicios Java
cd clientes-service && mvn spring-boot:run
cd facturacion-service && mvn spring-boot:run
cd api-gateway && mvn spring-boot:run

# 4. Ejecutar microservicio Python
cd tax-calculator-service
pip install -r requirements.txt
uvicorn main:app --reload --port 5000

# 5. Cliente de prueba
cd client-nodejs
npm install
API_URL=http://localhost:8080 npm start
```

Usa perfiles `dev` o variables `SPRING_DATASOURCE_*` para apuntar a tus bases locales.

---

## Verificaciones rápidas

1. **Clientes Service**  
   `curl http://localhost:8081/actuator/health`

2. **Facturación Service**  
   `curl http://localhost:8082/actuator/health`

3. **Tax Calculator**  
   `curl http://localhost:5000/health`

4. **Gateway**  
   `curl http://localhost:8080/api/v1/clientes?page=0&size=5`

5. **Zipkin**  
   Abrir `http://localhost:9411` y verificar trazas cuando hagas peticiones.

---

## Comandos útiles del Makefile

```bash
make help          # Lista de comandos
make build         # Compila los servicios Java
make test          # Ejecuta pruebas de clientes y facturación
make python-test   # Ejecuta pytest en tax-calculator
make client-test   # Corre el cliente Node.js
make docker-up     # docker-compose up -d
make k8s-deploy    # Aplica manifiestos Kubernetes
make sonar         # Envia resultados a SonarQube
```

---

## Troubleshooting

| Problema | Sugerencias |
|----------|-------------|
| Oracle tarda en iniciar | Revisa `docker-compose logs oracle`, puede tardar 2-3 minutos. |
| Cliente Node falla | Verifica `API_URL`, puertos y que `api-gateway` esté sano. |
| Kafka/Debezium no arranca | Asegura memoria suficiente (>6GB) y puertos libres 2181/9092. |
| Zipkin vacío | Confirma `MANAGEMENT_ZIPKIN_TRACING_ENDPOINT` en los servicios y genera tráfico. |
| Tests Java lentos | Usa `mvn -T 2C` o `mvnw` y habilita reutilización de contenedores en `~/.testcontainers.properties`. |

---

## Próximos pasos

1. Lee el README general para entender la arquitectura completa.
2. Revisa `FUNCIONALIDADES_SISTEMA.md` para ver el desglose de capacidades.
3. Explora `GUIA_MICROSERVICIOS.md` para información detallada por servicio.
4. Ejecuta los pipelines de pruebas (`make test`, `pytest`, `npm start`).
5. Publica los resultados en SonarQube siguiendo `SONARQUBE_GUIDE.md`.

¡Listo! Con estas instrucciones tienes todo lo necesario para correr, validar y extender el Sistema de Facturación Castor.
