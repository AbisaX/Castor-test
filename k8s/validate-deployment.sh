#!/bin/bash
# Script de validación de despliegue de Kubernetes
# Sistema de Facturación - Proyecto Castor

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Contadores
TOTAL_CHECKS=0
PASSED_CHECKS=0
FAILED_CHECKS=0
WARNING_CHECKS=0

# Namespace
NAMESPACE="facturacion"

echo -e "${BLUE}======================================"
echo "  VALIDACIÓN DE DESPLIEGUE K8S"
echo "  Sistema de Facturación"
echo "======================================${NC}"
echo ""

# Función para imprimir resultado
print_result() {
    local status=$1
    local message=$2
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))

    case $status in
        "OK")
            echo -e "${GREEN}[✓]${NC} $message"
            PASSED_CHECKS=$((PASSED_CHECKS + 1))
            ;;
        "FAIL")
            echo -e "${RED}[✗]${NC} $message"
            FAILED_CHECKS=$((FAILED_CHECKS + 1))
            ;;
        "WARN")
            echo -e "${YELLOW}[!]${NC} $message"
            WARNING_CHECKS=$((WARNING_CHECKS + 1))
            ;;
    esac
}

# 1. Verificar kubectl
echo -e "${BLUE}1. Verificando kubectl...${NC}"
if command -v kubectl &> /dev/null; then
    KUBECTL_VERSION=$(kubectl version --client --short 2>/dev/null || kubectl version --client 2>&1 | head -n1)
    print_result "OK" "kubectl instalado: $KUBECTL_VERSION"
else
    print_result "FAIL" "kubectl no está instalado"
    exit 1
fi
echo ""

# 2. Verificar conexión al cluster
echo -e "${BLUE}2. Verificando conexión al cluster...${NC}"
if kubectl cluster-info &> /dev/null; then
    CLUSTER_INFO=$(kubectl cluster-info 2>&1 | head -n1)
    print_result "OK" "Conectado al cluster"
else
    print_result "FAIL" "No se puede conectar al cluster de Kubernetes"
    exit 1
fi
echo ""

# 3. Verificar namespace
echo -e "${BLUE}3. Verificando namespace...${NC}"
if kubectl get namespace $NAMESPACE &> /dev/null; then
    print_result "OK" "Namespace '$NAMESPACE' existe"
else
    print_result "FAIL" "Namespace '$NAMESPACE' no existe"
    exit 1
fi
echo ""

# 4. Verificar Pods
echo -e "${BLUE}4. Verificando Pods...${NC}"
PODS=$(kubectl get pods -n $NAMESPACE --no-headers 2>/dev/null | wc -l)
if [ $PODS -eq 0 ]; then
    print_result "FAIL" "No hay pods desplegados"
else
    print_result "OK" "Total de pods: $PODS"

    # Verificar pods por estado
    RUNNING=$(kubectl get pods -n $NAMESPACE --no-headers 2>/dev/null | grep -c "Running" || echo "0")
    PENDING=$(kubectl get pods -n $NAMESPACE --no-headers 2>/dev/null | grep -c "Pending" || echo "0")
    FAILED=$(kubectl get pods -n $NAMESPACE --no-headers 2>/dev/null | grep -c -E "Error|CrashLoopBackOff|ImagePullBackOff" || echo "0")

    print_result "OK" "Pods Running: $RUNNING"
    if [ $PENDING -gt 0 ]; then
        print_result "WARN" "Pods Pending: $PENDING"
    fi
    if [ $FAILED -gt 0 ]; then
        print_result "FAIL" "Pods Failed: $FAILED"
    fi
fi
echo ""

# 5. Verificar Deployments
echo -e "${BLUE}5. Verificando Deployments...${NC}"
DEPLOYMENTS=("postgres" "oracle" "zipkin" "zookeeper" "kafka" "debezium-connect" "python-service" "clientes-service" "facturacion-service" "api-gateway")

for deployment in "${DEPLOYMENTS[@]}"; do
    if kubectl get deployment $deployment -n $NAMESPACE &> /dev/null; then
        READY=$(kubectl get deployment $deployment -n $NAMESPACE -o jsonpath='{.status.readyReplicas}' 2>/dev/null || echo "0")
        DESIRED=$(kubectl get deployment $deployment -n $NAMESPACE -o jsonpath='{.spec.replicas}' 2>/dev/null || echo "0")

        if [ "$READY" == "$DESIRED" ] && [ "$READY" != "0" ]; then
            print_result "OK" "Deployment '$deployment': $READY/$DESIRED ready"
        else
            print_result "WARN" "Deployment '$deployment': $READY/$DESIRED ready"
        fi
    else
        print_result "WARN" "Deployment '$deployment' no encontrado"
    fi
done
echo ""

# 6. Verificar Services
echo -e "${BLUE}6. Verificando Services...${NC}"
SERVICES=$(kubectl get services -n $NAMESPACE --no-headers 2>/dev/null | wc -l)
if [ $SERVICES -eq 0 ]; then
    print_result "FAIL" "No hay servicios desplegados"
else
    print_result "OK" "Total de servicios: $SERVICES"
fi

EXPECTED_SERVICES=("postgres-service" "oracle-service" "zipkin-service" "zookeeper-service" "kafka-service" "debezium-connect" "python-service" "clientes-service" "facturacion-service" "api-gateway")

for service in "${EXPECTED_SERVICES[@]}"; do
    if kubectl get service $service -n $NAMESPACE &> /dev/null; then
        CLUSTER_IP=$(kubectl get service $service -n $NAMESPACE -o jsonpath='{.spec.clusterIP}' 2>/dev/null)
        print_result "OK" "Service '$service' disponible ($CLUSTER_IP)"
    else
        print_result "WARN" "Service '$service' no encontrado"
    fi
done
echo ""

# 7. Verificar PVCs
echo -e "${BLUE}7. Verificando Persistent Volume Claims...${NC}"
PVCS=$(kubectl get pvc -n $NAMESPACE --no-headers 2>/dev/null | wc -l)
if [ $PVCS -eq 0 ]; then
    print_result "WARN" "No hay PVCs desplegados"
else
    print_result "OK" "Total de PVCs: $PVCS"

    BOUND=$(kubectl get pvc -n $NAMESPACE --no-headers 2>/dev/null | grep -c "Bound" || echo "0")
    if [ $BOUND -eq $PVCS ]; then
        print_result "OK" "Todos los PVCs están Bound ($BOUND/$PVCS)"
    else
        print_result "WARN" "PVCs Bound: $BOUND/$PVCS"
    fi
fi
echo ""

# 8. Verificar ConfigMaps
echo -e "${BLUE}8. Verificando ConfigMaps...${NC}"
EXPECTED_CONFIGMAPS=("postgres-config" "postgres-init-script" "oracle-config" "oracle-init-script" "clientes-config" "facturacion-config" "api-gateway-config")

CONFIGMAP_COUNT=0
for cm in "${EXPECTED_CONFIGMAPS[@]}"; do
    if kubectl get configmap $cm -n $NAMESPACE &> /dev/null; then
        CONFIGMAP_COUNT=$((CONFIGMAP_COUNT + 1))
    fi
done

print_result "OK" "ConfigMaps encontrados: $CONFIGMAP_COUNT/${#EXPECTED_CONFIGMAPS[@]}"
echo ""

# 9. Verificar Secrets
echo -e "${BLUE}9. Verificando Secrets...${NC}"
EXPECTED_SECRETS=("postgres-secret" "oracle-secret" "clientes-secret" "facturacion-secret")

SECRET_COUNT=0
for secret in "${EXPECTED_SECRETS[@]}"; do
    if kubectl get secret $secret -n $NAMESPACE &> /dev/null; then
        SECRET_COUNT=$((SECRET_COUNT + 1))
    fi
done

print_result "OK" "Secrets encontrados: $SECRET_COUNT/${#EXPECTED_SECRETS[@]}"
echo ""

# 10. Verificar health de servicios (si están running)
echo -e "${BLUE}10. Verificando health endpoints...${NC}"

check_service_health() {
    local service=$1
    local port=$2
    local path=$3

    # Verificar si el pod existe y está running
    POD=$(kubectl get pods -n $NAMESPACE -l app=$service --field-selector=status.phase=Running -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)

    if [ -n "$POD" ]; then
        # Intentar curl al health endpoint
        HEALTH=$(kubectl exec -n $NAMESPACE $POD -- curl -s -o /dev/null -w "%{http_code}" http://localhost:$port$path 2>/dev/null || echo "000")

        if [ "$HEALTH" == "200" ]; then
            print_result "OK" "$service health check passed (HTTP $HEALTH)"
        else
            print_result "WARN" "$service health check returned HTTP $HEALTH"
        fi
    else
        print_result "WARN" "$service pod no está Running, no se puede verificar health"
    fi
}

# Java services con actuator/health
check_service_health "clientes-service" "8081" "/actuator/health"
check_service_health "facturacion-service" "8082" "/actuator/health"
check_service_health "api-gateway" "8080" "/actuator/health"

# Python service
check_service_health "python-service" "5000" "/health"

echo ""

# 11. Verificar logs recientes para errores
echo -e "${BLUE}11. Verificando logs recientes para errores críticos...${NC}"

check_recent_errors() {
    local service=$1

    POD=$(kubectl get pods -n $NAMESPACE -l app=$service -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)

    if [ -n "$POD" ]; then
        ERRORS=$(kubectl logs -n $NAMESPACE $POD --tail=50 2>/dev/null | grep -i -E "error|exception|fatal" | grep -v -i "no error" | wc -l || echo "0")

        if [ $ERRORS -eq 0 ]; then
            print_result "OK" "$service: No hay errores recientes en logs"
        else
            print_result "WARN" "$service: $ERRORS errores encontrados en logs recientes"
        fi
    else
        print_result "WARN" "$service: Pod no encontrado, no se pueden revisar logs"
    fi
}

check_recent_errors "postgres"
check_recent_errors "clientes-service"
check_recent_errors "facturacion-service"
check_recent_errors "api-gateway"

echo ""

# Resumen final
echo -e "${BLUE}======================================"
echo "  RESUMEN DE VALIDACIÓN"
echo "======================================${NC}"
echo ""
echo -e "Total de verificaciones: ${BLUE}$TOTAL_CHECKS${NC}"
echo -e "Pasadas: ${GREEN}$PASSED_CHECKS${NC}"
echo -e "Advertencias: ${YELLOW}$WARNING_CHECKS${NC}"
echo -e "Fallidas: ${RED}$FAILED_CHECKS${NC}"
echo ""

if [ $FAILED_CHECKS -eq 0 ]; then
    if [ $WARNING_CHECKS -eq 0 ]; then
        echo -e "${GREEN}✓ El despliegue está completamente funcional${NC}"
        exit 0
    else
        echo -e "${YELLOW}! El despliegue está mayormente funcional con algunas advertencias${NC}"
        exit 0
    fi
else
    echo -e "${RED}✗ El despliegue tiene problemas que requieren atención${NC}"
    echo ""
    echo "Para más detalles, ejecute:"
    echo "  kubectl get all -n $NAMESPACE"
    echo "  kubectl describe pods -n $NAMESPACE"
    echo "  kubectl logs -n $NAMESPACE <pod-name>"
    exit 1
fi
