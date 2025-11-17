# Kubernetes Deployment Guide

## Descripción

Este directorio contiene los manifests de Kubernetes para desplegar el sistema completo de facturación en un cluster de Kubernetes.

## Arquitectura

El sistema está compuesto por los siguientes servicios:

### Bases de Datos
- **PostgreSQL**: Base de datos principal (facturacion_db) con CDC habilitado
  - Configurado con `wal_level=logical` para Debezium
  - Scripts de inicialización automática mediante ConfigMap
- **Oracle XE**: Base de datos de réplica para facturas
  - Oracle Express Edition 21 con inicio rápido
  - Scripts de inicialización automática con PL/SQL

### Microservicios
- **clientes-service**: Gestión de clientes (Java/Spring Boot)
- **facturacion-service**: Gestión de facturas (Java/Spring Boot)
- **tax-calculator-service** (python-service): Cálculo de impuestos (Python/FastAPI)
- **api-gateway**: API Gateway con rate limiting, circuit breakers y tracing

### Infraestructura
- **Zipkin**: Distributed tracing
- **Zookeeper**: Coordinación para Kafka
- **Kafka**: Message broker para CDC
- **Debezium Connect**: Change Data Capture

## Estructura de Directorios

```
k8s/
├── namespace.yaml                     # Namespace principal
├── deploy-all.sh                      # Script de despliegue completo (Linux/Mac)
├── deploy-all.bat                     # Script de despliegue completo (Windows)
├── validate-deployment.sh             # Script de validación (Linux/Mac)
├── validate-deployment.bat            # Script de validación (Windows)
├── postgres/                          # PostgreSQL manifests
│   ├── postgres-configmap.yaml        # Configuración con CDC habilitado
│   ├── postgres-init-script-configmap.yaml  # Scripts SQL de inicialización
│   ├── postgres-secret.yaml
│   ├── postgres-pvc.yaml
│   ├── postgres-deployment.yaml       # Deployment con wal_level=logical
│   └── postgres-service.yaml
├── oracle/                            # Oracle manifests
│   ├── oracle-configmap.yaml          # Configuración con APP_USER
│   ├── oracle-init-script-configmap.yaml    # Scripts SQL/PL-SQL de inicialización
│   ├── oracle-secret.yaml
│   ├── oracle-pvc.yaml
│   ├── oracle-deployment.yaml         # Deployment con gvenzl/oracle-xe
│   └── oracle-service.yaml
├── clientes-service/                  # Clientes Service manifests
│   ├── clientes-configmap.yaml
│   ├── clientes-secret.yaml
│   ├── clientes-deployment.yaml
│   └── clientes-service.yaml
├── facturacion-service/               # Facturacion Service manifests
│   ├── facturacion-configmap.yaml
│   ├── facturacion-secret.yaml
│   ├── facturacion-deployment.yaml
│   └── facturacion-service.yaml
├── api-gateway/                       # API Gateway manifests
│   ├── api-gateway-configmap.yaml
│   ├── api-gateway-deployment.yaml
│   └── api-gateway-service.yaml
├── python-service/                    # Tax Calculator manifests
│   ├── python-deployment.yaml
│   └── python-service.yaml
├── zipkin/                            # Zipkin manifests
│   ├── zipkin-deployment.yaml
│   └── zipkin-service.yaml
├── kafka/                             # Kafka & CDC manifests
│   ├── zookeeper-deployment.yaml
│   ├── zookeeper-service.yaml
│   ├── kafka-deployment.yaml
│   ├── kafka-service.yaml
│   ├── debezium-deployment.yaml
│   └── debezium-service.yaml
└── README.md                          # Esta documentación
```

## Requisitos Previos

1. Cluster de Kubernetes (local con Minikube/Kind o cloud con GKE/EKS/AKS)
2. kubectl instalado y configurado
3. Imágenes Docker construidas y disponibles:
   - `clientes-service:latest`
   - `facturacion-service:latest`
   - `api-gateway:latest`
   - `tax-calculator-service:latest`

## Construcción de Imágenes

Antes de desplegar, construir las imágenes Docker:

```bash
# Clientes Service
cd clientes-service
docker build -t clientes-service:latest .

# Facturacion Service
cd ../facturacion-service
docker build -t facturacion-service:latest .

# API Gateway
cd ../api-gateway
docker build -t api-gateway:latest .

# Tax Calculator Service
cd ../tax-calculator-service
docker build -t tax-calculator-service:latest .
```

### Para Minikube

Si usa Minikube, asegúrese de usar el daemon de Docker de Minikube:

```bash
eval $(minikube docker-env)
# Luego construir las imágenes
```

## Despliegue

### Opción 1: Despliegue Automático (Recomendado)

Se incluyen scripts que despliegan todo el sistema automáticamente con la secuencia correcta y tiempos de espera apropiados.

#### Linux/Mac

```bash
cd k8s
chmod +x deploy-all.sh
./deploy-all.sh
```

#### Windows

```batch
cd k8s
deploy-all.bat
```

El script automático realiza:
1. Verificación de kubectl y conexión al cluster
2. Creación del namespace
3. Despliegue de PostgreSQL con CDC habilitado (espera 60s)
4. Despliegue de Oracle XE (espera 120s para inicio completo)
5. Despliegue de Zipkin para tracing (espera 30s)
6. Despliegue de Kafka ecosystem (Zookeeper → Kafka → Debezium)
7. Despliegue de Tax Calculator Service
8. Despliegue de microservicios (Clientes → Facturación)
9. Despliegue de API Gateway
10. Muestra resumen y comandos útiles

### Opción 2: Despliegue Manual

#### 1. Crear Namespace

```bash
kubectl apply -f namespace.yaml
```

#### 2. Desplegar Bases de Datos

```bash
# PostgreSQL (con CDC habilitado)
kubectl apply -f postgres/postgres-configmap.yaml
kubectl apply -f postgres/postgres-init-script-configmap.yaml
kubectl apply -f postgres/postgres-secret.yaml
kubectl apply -f postgres/postgres-pvc.yaml
kubectl apply -f postgres/postgres-deployment.yaml
kubectl apply -f postgres/postgres-service.yaml

# Oracle XE (con scripts de inicialización)
kubectl apply -f oracle/oracle-configmap.yaml
kubectl apply -f oracle/oracle-init-script-configmap.yaml
kubectl apply -f oracle/oracle-secret.yaml
kubectl apply -f oracle/oracle-pvc.yaml
kubectl apply -f oracle/oracle-deployment.yaml
kubectl apply -f oracle/oracle-service.yaml

# Esperar a que estén ready
kubectl wait --for=condition=ready pod -l app=postgres -n facturacion --timeout=300s
kubectl wait --for=condition=ready pod -l app=oracle -n facturacion --timeout=600s
```

**Nota importante**: PostgreSQL está configurado con `wal_level=logical` para CDC. Oracle puede tardar 5-10 minutos en estar completamente listo.

### 3. Desplegar Infraestructura

```bash
# Zipkin (Tracing)
kubectl apply -f zipkin/

# Kafka & CDC
kubectl apply -f kafka/zookeeper-deployment.yaml
kubectl apply -f kafka/zookeeper-service.yaml
kubectl wait --for=condition=ready pod -l app=zookeeper -n facturacion --timeout=300s

kubectl apply -f kafka/kafka-deployment.yaml
kubectl apply -f kafka/kafka-service.yaml
kubectl wait --for=condition=ready pod -l app=kafka -n facturacion --timeout=300s

kubectl apply -f kafka/debezium-deployment.yaml
kubectl apply -f kafka/debezium-service.yaml
```

### 4. Desplegar Tax Calculator Service

```bash
kubectl apply -f python-service/
kubectl wait --for=condition=ready pod -l app=python-service -n facturacion --timeout=300s
```

### 5. Desplegar Microservicios

```bash
# Clientes Service
kubectl apply -f clientes-service/
kubectl wait --for=condition=ready pod -l app=clientes-service -n facturacion --timeout=300s

# Facturacion Service
kubectl apply -f facturacion-service/
kubectl wait --for=condition=ready pod -l app=facturacion-service -n facturacion --timeout=300s
```

### 6. Desplegar API Gateway

```bash
kubectl apply -f api-gateway/
kubectl wait --for=condition=ready pod -l app=api-gateway -n facturacion --timeout=300s
```

## Configuración de Bases de Datos

### PostgreSQL - Configuración CDC

PostgreSQL está configurado automáticamente para Change Data Capture (CDC) con los siguientes parámetros:

- `wal_level=logical` - Habilita replicación lógica
- `max_wal_senders=10` - Máximo de procesos de envío WAL
- `max_replication_slots=10` - Máximo de slots de replicación

La base de datos se llama `facturacion_db` y usa el usuario `castor_user`. Los scripts de inicialización se ejecutan automáticamente desde el ConfigMap [postgres-init-script-configmap.yaml](postgres/postgres-init-script-configmap.yaml).

### Oracle XE - Configuración

Oracle Express Edition 21 se configura automáticamente con:

- **SID**: XE
- **PDB**: XEPDB1
- **Usuario de aplicación**: castor_facturacion
- **Character Set**: AL32UTF8

Los scripts de inicialización (incluyendo procedimientos PL/SQL) se ejecutan automáticamente desde el ConfigMap [oracle-init-script-configmap.yaml](oracle/oracle-init-script-configmap.yaml).

**Nota**: Oracle puede tardar 5-10 minutos en iniciar completamente la primera vez. El StartupProbe está configurado para esperar hasta 20 minutos.

## Verificación del Despliegue

### Script de Validación Automatizada (Recomendado)

Se incluyen scripts de validación que verifican automáticamente el estado de todos los componentes:

#### Linux/Mac

```bash
cd k8s
chmod +x validate-deployment.sh
./validate-deployment.sh
```

#### Windows

```batch
cd k8s
validate-deployment.bat
```

El script de validación verifica:
- ✓ Instalación y conexión de kubectl
- ✓ Existencia del namespace
- ✓ Estado de todos los pods (Running/Pending/Failed)
- ✓ Estado de todos los deployments (replicas ready)
- ✓ Disponibilidad de todos los services
- ✓ Estado de PVCs (Bound)
- ✓ Existencia de ConfigMaps y Secrets
- ✓ Health endpoints de los microservicios
- ✓ Logs recientes para detectar errores críticos

El script proporciona un resumen con estadísticas de verificaciones pasadas, advertencias y fallidas.

### Verificación Manual

#### Ver todos los recursos

```bash
kubectl get all -n facturacion
```

### Ver pods

```bash
kubectl get pods -n facturacion -o wide
```

### Ver servicios

```bash
kubectl get services -n facturacion
```

### Ver logs de un pod

```bash
# Clientes Service
kubectl logs -f deployment/clientes-service -n facturacion

# Facturacion Service
kubectl logs -f deployment/facturacion-service -n facturacion

# API Gateway
kubectl logs -f deployment/api-gateway -n facturacion
```

### Verificar health de servicios

```bash
# Clientes Service
kubectl exec -it deployment/clientes-service -n facturacion -- curl http://localhost:8081/actuator/health

# Facturacion Service
kubectl exec -it deployment/facturacion-service -n facturacion -- curl http://localhost:8082/actuator/health

# API Gateway
kubectl exec -it deployment/api-gateway -n facturacion -- curl http://localhost:8080/actuator/health
```

## Acceso a los Servicios

### API Gateway (LoadBalancer)

```bash
# Obtener IP externa (cloud) o NodePort (minikube)
kubectl get svc api-gateway -n facturacion

# Minikube
minikube service api-gateway -n facturacion

# Cloud
API_GATEWAY_IP=$(kubectl get svc api-gateway -n facturacion -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
echo "API Gateway: http://${API_GATEWAY_IP}:8080"
```

### Zipkin UI

```bash
kubectl port-forward svc/zipkin-service 9411:9411 -n facturacion
# Abrir http://localhost:9411
```

### Debezium Connect

```bash
kubectl port-forward svc/debezium-connect 8083:8083 -n facturacion
# Acceder a http://localhost:8083
```

## Port Forwarding para Desarrollo

```bash
# API Gateway
kubectl port-forward svc/api-gateway 8080:8080 -n facturacion

# Clientes Service
kubectl port-forward svc/clientes-service 8081:8081 -n facturacion

# Facturacion Service
kubectl port-forward svc/facturacion-service 8082:8082 -n facturacion

# PostgreSQL
kubectl port-forward svc/postgres-service 5432:5432 -n facturacion

# Oracle
kubectl port-forward svc/oracle-service 1521:1521 -n facturacion

# Zipkin
kubectl port-forward svc/zipkin-service 9411:9411 -n facturacion
```

## Escalado

```bash
# Escalar Clientes Service
kubectl scale deployment clientes-service --replicas=3 -n facturacion

# Escalar Facturacion Service
kubectl scale deployment facturacion-service --replicas=3 -n facturacion

# Escalar API Gateway
kubectl scale deployment api-gateway --replicas=5 -n facturacion
```

## Configuración de CDC con Debezium

PostgreSQL ya está pre-configurado para CDC (Change Data Capture) con `wal_level=logical`. Después del despliegue completo, solo necesita registrar los conectores de Debezium:

```bash
# Port forward a Debezium Connect
kubectl port-forward svc/debezium-connect 8083:8083 -n facturacion

# En otra terminal, registrar conectores
cd ../debezium

# Linux/Mac
./register-connectors.sh

# Windows
register-connectors.bat
```

Los conectores sincronizarán automáticamente los datos de PostgreSQL (clientes) a Oracle (facturas) mediante Kafka.

## Actualización de Servicios

```bash
# Actualizar imagen
kubectl set image deployment/clientes-service \
  clientes-service=clientes-service:v2 \
  -n facturacion

# Verificar rollout
kubectl rollout status deployment/clientes-service -n facturacion

# Rollback si es necesario
kubectl rollout undo deployment/clientes-service -n facturacion
```

## Monitoreo

### Metrics (Prometheus)

Los servicios exponen métricas en `/actuator/prometheus`:

```bash
kubectl port-forward svc/api-gateway 8080:8080 -n facturacion
curl http://localhost:8080/actuator/prometheus
```

### Traces (Zipkin)

```bash
kubectl port-forward svc/zipkin-service 9411:9411 -n facturacion
# Abrir http://localhost:9411
```

### Logs centralizados

```bash
# Ver logs de todos los pods de un deployment
kubectl logs -f deployment/clientes-service -n facturacion --all-containers=true

# Ver logs de todos los pods con label
kubectl logs -l app=clientes-service -n facturacion --tail=100
```

## Troubleshooting

### Pod no inicia

```bash
# Describir pod
kubectl describe pod <pod-name> -n facturacion

# Ver eventos
kubectl get events -n facturacion --sort-by='.lastTimestamp'

# Ver logs
kubectl logs <pod-name> -n facturacion
```

### Service no accesible

```bash
# Verificar endpoints
kubectl get endpoints -n facturacion

# Verificar service
kubectl describe svc <service-name> -n facturacion

# Probar conectividad interna
kubectl run debug --image=curlimages/curl -it --rm -n facturacion -- sh
# Dentro del pod:
curl http://clientes-service:8081/actuator/health
```

### Base de datos no conecta

```bash
# Verificar que el pod de BD está ready
kubectl get pods -l app=postgres -n facturacion

# Verificar logs de BD
kubectl logs -l app=postgres -n facturacion

# Probar conexión desde pod de servicio
kubectl exec -it deployment/clientes-service -n facturacion -- bash
# Dentro del pod:
nc -zv postgres-service 5432
```

## Limpieza

```bash
# Eliminar todos los recursos
kubectl delete namespace facturacion

# O eliminar selectivamente
kubectl delete -f api-gateway/
kubectl delete -f facturacion-service/
kubectl delete -f clientes-service/
kubectl delete -f python-service/
kubectl delete -f kafka/
kubectl delete -f zipkin/
kubectl delete -f oracle/
kubectl delete -f postgres/
kubectl delete -f namespace.yaml
```

## Consideraciones de Producción

### Seguridad

1. **Secrets**: Usar Kubernetes Secrets o herramientas como Sealed Secrets, Vault
2. **RBAC**: Configurar roles y service accounts apropiados
3. **Network Policies**: Restringir comunicación entre pods
4. **Pod Security Standards**: Aplicar políticas de seguridad

### Alta Disponibilidad

1. **Múltiples réplicas**: Escalar deployments críticos
2. **Pod Disruption Budgets**: Asegurar disponibilidad durante mantenimiento
3. **Anti-affinity**: Distribuir pods en diferentes nodos
4. **Health checks**: Configurar liveness, readiness y startup probes

### Persistencia

1. **PersistentVolumes**: Usar StorageClass apropiado para cloud provider
2. **Backups**: Configurar backups automáticos de bases de datos
3. **StatefulSets**: Para bases de datos con alta disponibilidad

### Monitoreo

1. **Prometheus + Grafana**: Monitoreo de métricas
2. **ELK/EFK Stack**: Logs centralizados
3. **Jaeger/Zipkin**: Distributed tracing
4. **Alertas**: Configurar alertas para métricas críticas

## Recursos Adicionales

- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Spring Boot on Kubernetes](https://spring.io/guides/gs/spring-boot-kubernetes/)
- [Debezium on Kubernetes](https://debezium.io/documentation/reference/stable/operations/kubernetes.html)
- [Kafka on Kubernetes](https://kafka.apache.org/documentation/#k8s)
