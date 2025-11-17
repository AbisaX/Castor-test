#!/bin/bash

###############################################################################
# Script: deploy-connectors.sh
# Descripción: Despliega los conectores de Debezium (PostgreSQL Source y Oracle Sink)
# Uso: ./deploy-connectors.sh
###############################################################################

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuración
DEBEZIUM_CONNECT_URL="http://localhost:8083"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
POSTGRES_CONNECTOR_CONFIG="${SCRIPT_DIR}/postgres-connector.json"
ORACLE_CONNECTOR_CONFIG="${SCRIPT_DIR}/oracle-sink-connector.json"

# Timeout para health check (segundos)
HEALTH_CHECK_TIMEOUT=60
HEALTH_CHECK_INTERVAL=5

###############################################################################
# Funciones
###############################################################################

print_header() {
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

# Verificar que Kafka Connect está disponible
check_kafka_connect() {
    print_info "Verificando disponibilidad de Kafka Connect..."

    local elapsed=0
    while [ $elapsed -lt $HEALTH_CHECK_TIMEOUT ]; do
        if curl -sf "${DEBEZIUM_CONNECT_URL}/" > /dev/null 2>&1; then
            print_success "Kafka Connect está disponible"
            return 0
        fi

        echo -n "."
        sleep $HEALTH_CHECK_INTERVAL
        elapsed=$((elapsed + HEALTH_CHECK_INTERVAL))
    done

    print_error "Kafka Connect no está disponible después de ${HEALTH_CHECK_TIMEOUT}s"
    print_info "Verifica que el contenedor debezium-connect esté ejecutándose:"
    print_info "  docker ps | grep debezium-connect"
    print_info "  docker logs castor-debezium-connect"
    return 1
}

# Verificar que el archivo de configuración existe
check_config_file() {
    local config_file=$1
    local connector_name=$2

    if [ ! -f "$config_file" ]; then
        print_error "No se encuentra el archivo de configuración: $config_file"
        return 1
    fi

    print_success "Archivo de configuración encontrado: $connector_name"
    return 0
}

# Verificar si un conector ya existe
connector_exists() {
    local connector_name=$1

    if curl -sf "${DEBEZIUM_CONNECT_URL}/connectors/${connector_name}" > /dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

# Eliminar conector existente
delete_connector() {
    local connector_name=$1

    print_warning "Eliminando conector existente: $connector_name"

    if curl -sf -X DELETE "${DEBEZIUM_CONNECT_URL}/connectors/${connector_name}" > /dev/null 2>&1; then
        print_success "Conector eliminado: $connector_name"
        sleep 3  # Esperar a que se complete la eliminación
        return 0
    else
        print_error "Error al eliminar conector: $connector_name"
        return 1
    fi
}

# Desplegar conector
deploy_connector() {
    local config_file=$1
    local connector_name=$2

    print_info "Desplegando conector: $connector_name"

    # Intentar desplegar el conector
    local response=$(curl -s -w "\n%{http_code}" -X POST \
        -H "Content-Type: application/json" \
        --data @"$config_file" \
        "${DEBEZIUM_CONNECT_URL}/connectors")

    local http_code=$(echo "$response" | tail -n 1)
    local body=$(echo "$response" | sed '$d')

    if [ "$http_code" -eq 201 ] || [ "$http_code" -eq 200 ]; then
        print_success "Conector desplegado exitosamente: $connector_name"
        return 0
    else
        print_error "Error al desplegar conector: $connector_name"
        print_error "HTTP Code: $http_code"
        print_error "Response: $body"
        return 1
    fi
}

# Verificar estado del conector
check_connector_status() {
    local connector_name=$1

    print_info "Verificando estado del conector: $connector_name"

    local status=$(curl -s "${DEBEZIUM_CONNECT_URL}/connectors/${connector_name}/status")

    # Extraer estado del conector
    local connector_state=$(echo "$status" | grep -o '"state":"[^"]*"' | head -1 | cut -d'"' -f4)

    if [ "$connector_state" = "RUNNING" ]; then
        print_success "Conector en estado RUNNING: $connector_name"

        # Verificar tareas
        local task_count=$(echo "$status" | grep -o '"state":"RUNNING"' | wc -l)
        print_info "Tareas ejecutándose: $task_count"

        return 0
    else
        print_warning "Conector en estado: $connector_state"
        print_info "Estado completo:"
        echo "$status" | jq '.' 2>/dev/null || echo "$status"
        return 1
    fi
}

# Listar todos los conectores
list_connectors() {
    print_info "Conectores desplegados:"

    local connectors=$(curl -s "${DEBEZIUM_CONNECT_URL}/connectors")
    echo "$connectors" | jq '.' 2>/dev/null || echo "$connectors"
}

###############################################################################
# Main
###############################################################################

main() {
    print_header "DEBEZIUM CDC - DESPLIEGUE DE CONECTORES"

    # 1. Verificar que Kafka Connect está disponible
    if ! check_kafka_connect; then
        exit 1
    fi

    echo ""
    print_header "VERIFICANDO ARCHIVOS DE CONFIGURACIÓN"

    # 2. Verificar archivos de configuración
    check_config_file "$POSTGRES_CONNECTOR_CONFIG" "PostgreSQL Source Connector" || exit 1
    check_config_file "$ORACLE_CONNECTOR_CONFIG" "Oracle Sink Connector" || exit 1

    echo ""
    print_header "DESPLEGANDO CONECTOR POSTGRESQL SOURCE"

    # 3. Desplegar PostgreSQL Source Connector
    local postgres_connector_name="postgres-clientes-source"

    if connector_exists "$postgres_connector_name"; then
        print_warning "El conector ya existe: $postgres_connector_name"
        read -p "¿Desea reemplazarlo? (s/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Ss]$ ]]; then
            delete_connector "$postgres_connector_name" || exit 1
            deploy_connector "$POSTGRES_CONNECTOR_CONFIG" "$postgres_connector_name" || exit 1
        else
            print_info "Conservando conector existente: $postgres_connector_name"
        fi
    else
        deploy_connector "$POSTGRES_CONNECTOR_CONFIG" "$postgres_connector_name" || exit 1
    fi

    # Esperar un poco antes de verificar el estado
    sleep 5
    check_connector_status "$postgres_connector_name"

    echo ""
    print_header "DESPLEGANDO CONECTOR ORACLE SINK"

    # 4. Desplegar Oracle Sink Connector
    local oracle_connector_name="oracle-clientes-sink"

    print_warning "IMPORTANTE: Asegúrate de que el JDBC Driver de Oracle esté instalado en Debezium Connect"
    print_info "Para instalar el driver Oracle JDBC:"
    print_info "  docker exec -it castor-debezium-connect bash"
    print_info "  cd /kafka/connect"
    print_info "  wget https://download.oracle.com/otn-pub/otn_software/jdbc/ojdbc8.jar"
    print_info "  exit"
    print_info "  docker restart castor-debezium-connect"
    echo ""

    read -p "¿Continuar con el despliegue del Oracle Sink Connector? (s/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Ss]$ ]]; then
        if connector_exists "$oracle_connector_name"; then
            print_warning "El conector ya existe: $oracle_connector_name"
            read -p "¿Desea reemplazarlo? (s/N): " -n 1 -r
            echo
            if [[ $REPLY =~ ^[Ss]$ ]]; then
                delete_connector "$oracle_connector_name" || exit 1
                deploy_connector "$ORACLE_CONNECTOR_CONFIG" "$oracle_connector_name" || exit 1
            else
                print_info "Conservando conector existente: $oracle_connector_name"
            fi
        else
            deploy_connector "$ORACLE_CONNECTOR_CONFIG" "$oracle_connector_name" || exit 1
        fi

        # Esperar un poco antes de verificar el estado
        sleep 5
        check_connector_status "$oracle_connector_name"
    else
        print_info "Despliegue de Oracle Sink Connector cancelado"
    fi

    echo ""
    print_header "RESUMEN DE CONECTORES"
    list_connectors

    echo ""
    print_header "COMANDOS ÚTILES"
    echo "Ver estado de todos los conectores:"
    echo "  curl http://localhost:8083/connectors | jq '.'"
    echo ""
    echo "Ver estado de un conector específico:"
    echo "  curl http://localhost:8083/connectors/${postgres_connector_name}/status | jq '.'"
    echo ""
    echo "Ver topics de Kafka:"
    echo "  docker exec castor-kafka kafka-topics --bootstrap-server localhost:9092 --list"
    echo ""
    echo "Consumir mensajes de un topic:"
    echo "  docker exec castor-kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic castor.public.clientes --from-beginning"
    echo ""
    echo "Kafka UI: http://localhost:8090"
    echo ""

    print_success "Despliegue completado"
}

# Ejecutar main
main "$@"
