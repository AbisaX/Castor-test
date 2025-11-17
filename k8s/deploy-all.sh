#!/bin/bash
#
# Script de despliegue completo para Kubernetes
# Sistema de Facturación - Proyecto Castor
#
# Uso: ./deploy-all.sh
#

set -e  # Exit on error

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Funciones de utilidad
print_header() {
    echo -e "${BLUE}======================================${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}======================================${NC}"
    echo ""
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

# Verificar que kubectl está instalado
check_kubectl() {
    if ! command -v kubectl &> /dev/null; then
        print_error "kubectl no está instalado o no está en el PATH"
        exit 1
    fi
    print_success "kubectl encontrado: $(kubectl version --client --short 2>/dev/null || kubectl version --client)"
}

# Verificar conexión al cluster
check_cluster() {
    if ! kubectl cluster-info &> /dev/null; then
        print_error "No se puede conectar al cluster de Kubernetes"
        print_info "Verifica tu configuración de kubectl"
        exit 1
    fi
    print_success "Conectado al cluster: $(kubectl config current-context)"
}

# Esperar a que los pods estén ready
wait_for_pods() {
    local label=$1
    local namespace=$2
    local timeout=${3:-300}  # Default 5 minutos

    print_info "Esperando a que los pods con label '$label' estén ready..."

    kubectl wait --for=condition=ready pod \
        -l "$label" \
        -n "$namespace" \
        --timeout="${timeout}s" 2>/dev/null || {
        print_warning "Timeout esperando pods. Verificando manualmente..."
        kubectl get pods -l "$label" -n "$namespace"
    }
}

# Main script
main() {
    print_header "DESPLIEGUE COMPLETO - SISTEMA DE FACTURACIÓN"

    # 1. Verificaciones previas
    print_header "1. VERIFICACIONES PREVIAS"
    check_kubectl
    check_cluster

    # 2. Crear Namespace
    print_header "2. CREANDO NAMESPACE"
    kubectl apply -f namespace.yaml
    print_success "Namespace 'facturacion' creado/actualizado"
    echo ""

    # 3. Desplegar Bases de Datos
    print_header "3. DESPLEGANDO BASES DE DATOS"

    print_info "Desplegando PostgreSQL..."
    kubectl apply -f postgres/postgres-configmap.yaml
    kubectl apply -f postgres/postgres-init-script-configmap.yaml
    kubectl apply -f postgres/postgres-secret.yaml
    kubectl apply -f postgres/postgres-pvc.yaml
    kubectl apply -f postgres/postgres-deployment.yaml
    kubectl apply -f postgres/postgres-service.yaml
    print_success "PostgreSQL desplegado"

    print_info "Desplegando Oracle..."
    kubectl apply -f oracle/oracle-configmap.yaml
    kubectl apply -f oracle/oracle-init-script-configmap.yaml
    kubectl apply -f oracle/oracle-secret.yaml
    kubectl apply -f oracle/oracle-pvc.yaml
    kubectl apply -f oracle/oracle-deployment.yaml
    kubectl apply -f oracle/oracle-service.yaml
    print_success "Oracle desplegado"

    # Esperar a que las bases de datos estén listas
    print_info "Esperando a que PostgreSQL esté listo (puede tomar hasta 2 minutos)..."
    wait_for_pods "app=postgres" "facturacion" 180

    print_info "Esperando a que Oracle esté listo (puede tomar hasta 10 minutos)..."
    wait_for_pods "app=oracle" "facturacion" 600

    echo ""

    # 4. Desplegar Infraestructura (Tracing)
    print_header "4. DESPLEGANDO INFRAESTRUCTURA"

    print_info "Desplegando Zipkin..."
    kubectl apply -f zipkin/
    print_success "Zipkin desplegado"

    wait_for_pods "app=zipkin" "facturacion" 120
    echo ""

    # 5. Desplegar Kafka y CDC (opcional pero incluido)
    print_header "5. DESPLEGANDO KAFKA Y CDC (Debezium)"

    print_info "Desplegando Zookeeper..."
    kubectl apply -f kafka/zookeeper-deployment.yaml
    kubectl apply -f kafka/zookeeper-service.yaml
    wait_for_pods "app=zookeeper" "facturacion" 180

    print_info "Desplegando Kafka..."
    kubectl apply -f kafka/kafka-deployment.yaml
    kubectl apply -f kafka/kafka-service.yaml
    wait_for_pods "app=kafka" "facturacion" 180

    print_info "Desplegando Debezium Connect..."
    kubectl apply -f kafka/debezium-deployment.yaml
    kubectl apply -f kafka/debezium-service.yaml
    wait_for_pods "app=debezium-connect" "facturacion" 180

    print_success "Kafka y Debezium desplegados"
    echo ""

    # 6. Desplegar Tax Calculator Service
    print_header "6. DESPLEGANDO TAX CALCULATOR SERVICE"
    kubectl apply -f python-service/
    print_success "Tax Calculator Service desplegado"
    wait_for_pods "app=python-service" "facturacion" 120
    echo ""

    # 7. Desplegar Microservicios
    print_header "7. DESPLEGANDO MICROSERVICIOS"

    print_info "Desplegando Clientes Service..."
    kubectl apply -f clientes-service/
    wait_for_pods "app=clientes-service" "facturacion" 180
    print_success "Clientes Service desplegado"

    print_info "Desplegando Facturacion Service..."
    kubectl apply -f facturacion-service/
    wait_for_pods "app=facturacion-service" "facturacion" 240
    print_success "Facturacion Service desplegado"

    echo ""

    # 8. Desplegar API Gateway
    print_header "8. DESPLEGANDO API GATEWAY"
    kubectl apply -f api-gateway/
    wait_for_pods "app=api-gateway" "facturacion" 180
    print_success "API Gateway desplegado"
    echo ""

    # 9. Mostrar resumen
    print_header "DESPLIEGUE COMPLETADO"
    print_success "Todos los componentes han sido desplegados exitosamente"
    echo ""

    print_info "Recursos desplegados:"
    kubectl get all -n facturacion
    echo ""

    print_info "Para acceder al API Gateway:"
    echo "  kubectl get svc api-gateway -n facturacion"
    echo ""

    print_info "Para ver logs de un servicio:"
    echo "  kubectl logs -f deployment/clientes-service -n facturacion"
    echo ""

    print_info "Para port-forward al API Gateway:"
    echo "  kubectl port-forward svc/api-gateway 8080:8080 -n facturacion"
    echo ""

    print_warning "NOTA: Para configurar Debezium CDC, ejecuta:"
    echo "  cd ../debezium"
    echo "  kubectl port-forward svc/debezium-connect 8083:8083 -n facturacion"
    echo "  ./register-connectors.sh (or .bat)"
    echo ""
}

# Manejo de errores
trap 'print_error "Error en línea $LINENO. Saliendo..."; exit 1' ERR

# Ejecutar script principal
main

print_header "FIN DEL DESPLIEGUE"
