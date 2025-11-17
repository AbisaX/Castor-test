#!/bin/bash

###############################################################################
# Script: check-connectors.sh
# Descripción: Verifica el estado de los conectores de Debezium
# Uso: ./check-connectors.sh [nombre-conector]
###############################################################################

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuración
DEBEZIUM_CONNECT_URL="http://localhost:8083"

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
    echo -e "${CYAN}ℹ${NC} $1"
}

# Verificar que Kafka Connect está disponible
check_kafka_connect() {
    if ! curl -sf "${DEBEZIUM_CONNECT_URL}/" > /dev/null 2>&1; then
        print_error "Kafka Connect no está disponible en ${DEBEZIUM_CONNECT_URL}"
        print_info "Verifica que el contenedor debezium-connect esté ejecutándose"
        return 1
    fi
    return 0
}

# Listar todos los conectores
list_all_connectors() {
    print_header "CONECTORES DESPLEGADOS"

    local connectors=$(curl -s "${DEBEZIUM_CONNECT_URL}/connectors")

    if [ -z "$connectors" ] || [ "$connectors" = "[]" ]; then
        print_warning "No hay conectores desplegados"
        return 0
    fi

    echo "$connectors" | jq -r '.[]' 2>/dev/null | while read -r connector; do
        echo -e "${GREEN}●${NC} $connector"
    done

    echo ""
    local count=$(echo "$connectors" | jq '. | length' 2>/dev/null)
    print_info "Total de conectores: $count"
}

# Obtener estado detallado de un conector
get_connector_status() {
    local connector_name=$1

    print_header "ESTADO DEL CONECTOR: $connector_name"

    local status=$(curl -s "${DEBEZIUM_CONNECT_URL}/connectors/${connector_name}/status")

    if [ -z "$status" ]; then
        print_error "No se pudo obtener el estado del conector: $connector_name"
        return 1
    fi

    # Extraer información clave
    local connector_state=$(echo "$status" | jq -r '.connector.state' 2>/dev/null)
    local worker_id=$(echo "$status" | jq -r '.connector.worker_id' 2>/dev/null)

    # Estado del conector
    echo -e "${CYAN}Estado del Conector:${NC}"
    if [ "$connector_state" = "RUNNING" ]; then
        print_success "Estado: RUNNING"
    elif [ "$connector_state" = "FAILED" ]; then
        print_error "Estado: FAILED"
    else
        print_warning "Estado: $connector_state"
    fi
    echo -e "  Worker ID: $worker_id"

    # Estado de las tareas
    echo ""
    echo -e "${CYAN}Estado de las Tareas:${NC}"

    local tasks=$(echo "$status" | jq -r '.tasks[]' 2>/dev/null)
    if [ -z "$tasks" ]; then
        print_warning "No hay tareas ejecutándose"
    else
        echo "$status" | jq -r '.tasks[] | "  Task \(.id): \(.state) (Worker: \(.worker_id))"' 2>/dev/null
    fi

    # Errores si existen
    echo ""
    local connector_trace=$(echo "$status" | jq -r '.connector.trace' 2>/dev/null)
    local task_trace=$(echo "$status" | jq -r '.tasks[].trace' 2>/dev/null)

    if [ "$connector_trace" != "null" ] && [ ! -z "$connector_trace" ]; then
        echo -e "${RED}Errores del Conector:${NC}"
        echo "$connector_trace"
        echo ""
    fi

    if [ "$task_trace" != "null" ] && [ ! -z "$task_trace" ]; then
        echo -e "${RED}Errores de Tareas:${NC}"
        echo "$task_trace"
        echo ""
    fi

    # JSON completo
    echo ""
    echo -e "${CYAN}Estado Completo (JSON):${NC}"
    echo "$status" | jq '.' 2>/dev/null || echo "$status"
}

# Obtener configuración de un conector
get_connector_config() {
    local connector_name=$1

    print_header "CONFIGURACIÓN DEL CONECTOR: $connector_name"

    local config=$(curl -s "${DEBEZIUM_CONNECT_URL}/connectors/${connector_name}")

    if [ -z "$config" ]; then
        print_error "No se pudo obtener la configuración del conector: $connector_name"
        return 1
    fi

    echo "$config" | jq '.' 2>/dev/null || echo "$config"
}

# Obtener métricas de un conector
get_connector_metrics() {
    local connector_name=$1

    print_header "MÉTRICAS DEL CONECTOR: $connector_name"

    # Obtener topics del conector
    local topics=$(curl -s "${DEBEZIUM_CONNECT_URL}/connectors/${connector_name}/topics")
    echo -e "${CYAN}Topics:${NC}"
    echo "$topics" | jq '.' 2>/dev/null || echo "$topics"

    echo ""

    # Obtener tareas del conector
    local tasks=$(curl -s "${DEBEZIUM_CONNECT_URL}/connectors/${connector_name}/tasks")
    echo -e "${CYAN}Tareas:${NC}"
    echo "$tasks" | jq '.' 2>/dev/null || echo "$tasks"
}

# Verificar conectividad con las bases de datos
check_database_connectivity() {
    print_header "VERIFICANDO CONECTIVIDAD CON BASES DE DATOS"

    # PostgreSQL
    echo -e "${CYAN}PostgreSQL:${NC}"
    if docker exec castor-postgres pg_isready -U castor_user -d facturacion_db > /dev/null 2>&1; then
        print_success "PostgreSQL está disponible"
    else
        print_error "PostgreSQL no está disponible"
    fi

    # Oracle
    echo -e "${CYAN}Oracle:${NC}"
    if docker exec castor-oracle healthcheck.sh > /dev/null 2>&1; then
        print_success "Oracle está disponible"
    else
        print_error "Oracle no está disponible"
    fi

    # Kafka
    echo -e "${CYAN}Kafka:${NC}"
    if docker exec castor-kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1; then
        print_success "Kafka está disponible"
    else
        print_error "Kafka no está disponible"
    fi
}

# Verificar topics de Kafka
check_kafka_topics() {
    print_header "TOPICS DE KAFKA"

    local topics=$(docker exec castor-kafka kafka-topics --bootstrap-server localhost:9092 --list 2>/dev/null)

    if [ -z "$topics" ]; then
        print_warning "No se encontraron topics"
        return 0
    fi

    echo "$topics" | grep -E "^castor\." | while read -r topic; do
        echo -e "${GREEN}●${NC} $topic"

        # Obtener información del topic
        local partitions=$(docker exec castor-kafka kafka-topics --bootstrap-server localhost:9092 --describe --topic "$topic" 2>/dev/null | grep "PartitionCount" | awk '{print $2}')
        echo "    Particiones: $partitions"
    done

    echo ""
    local count=$(echo "$topics" | grep -c "^castor\." || echo "0")
    print_info "Total de topics CDC: $count"
}

# Monitoreo continuo
monitor_connectors() {
    local interval=${1:-5}

    print_header "MONITOREO CONTINUO (Ctrl+C para detener)"
    print_info "Intervalo: ${interval}s"
    echo ""

    while true; do
        clear
        echo -e "${BLUE}$(date '+%Y-%m-%d %H:%M:%S')${NC}"
        echo ""

        list_all_connectors

        echo ""
        print_header "ESTADO DE CONECTORES"

        local connectors=$(curl -s "${DEBEZIUM_CONNECT_URL}/connectors" | jq -r '.[]' 2>/dev/null)

        echo "$connectors" | while read -r connector; do
            if [ ! -z "$connector" ]; then
                local state=$(curl -s "${DEBEZIUM_CONNECT_URL}/connectors/${connector}/status" | jq -r '.connector.state' 2>/dev/null)

                if [ "$state" = "RUNNING" ]; then
                    echo -e "${GREEN}✓${NC} $connector: $state"
                elif [ "$state" = "FAILED" ]; then
                    echo -e "${RED}✗${NC} $connector: $state"
                else
                    echo -e "${YELLOW}⚠${NC} $connector: $state"
                fi
            fi
        done

        sleep "$interval"
    done
}

# Mostrar ayuda
show_help() {
    echo "Uso: $0 [opciones] [nombre-conector]"
    echo ""
    echo "Opciones:"
    echo "  -l, --list              Listar todos los conectores"
    echo "  -s, --status [nombre]   Mostrar estado de un conector"
    echo "  -c, --config [nombre]   Mostrar configuración de un conector"
    echo "  -m, --metrics [nombre]  Mostrar métricas de un conector"
    echo "  -a, --all [nombre]      Mostrar toda la información de un conector"
    echo "  -d, --databases         Verificar conectividad con bases de datos"
    echo "  -t, --topics            Listar topics de Kafka"
    echo "  -w, --watch [segundos]  Monitoreo continuo (intervalo por defecto: 5s)"
    echo "  -h, --help              Mostrar esta ayuda"
    echo ""
    echo "Ejemplos:"
    echo "  $0 --list"
    echo "  $0 --status postgres-clientes-source"
    echo "  $0 --all postgres-clientes-source"
    echo "  $0 --watch 10"
}

###############################################################################
# Main
###############################################################################

main() {
    # Verificar que Kafka Connect está disponible
    if ! check_kafka_connect; then
        exit 1
    fi

    # Si no hay argumentos, mostrar lista de conectores
    if [ $# -eq 0 ]; then
        list_all_connectors
        echo ""
        check_database_connectivity
        echo ""
        check_kafka_topics
        exit 0
    fi

    # Procesar argumentos
    case "$1" in
        -l|--list)
            list_all_connectors
            ;;
        -s|--status)
            if [ -z "$2" ]; then
                print_error "Debe especificar el nombre del conector"
                exit 1
            fi
            get_connector_status "$2"
            ;;
        -c|--config)
            if [ -z "$2" ]; then
                print_error "Debe especificar el nombre del conector"
                exit 1
            fi
            get_connector_config "$2"
            ;;
        -m|--metrics)
            if [ -z "$2" ]; then
                print_error "Debe especificar el nombre del conector"
                exit 1
            fi
            get_connector_metrics "$2"
            ;;
        -a|--all)
            if [ -z "$2" ]; then
                print_error "Debe especificar el nombre del conector"
                exit 1
            fi
            get_connector_status "$2"
            echo ""
            get_connector_config "$2"
            echo ""
            get_connector_metrics "$2"
            ;;
        -d|--databases)
            check_database_connectivity
            ;;
        -t|--topics)
            check_kafka_topics
            ;;
        -w|--watch)
            monitor_connectors "${2:-5}"
            ;;
        -h|--help)
            show_help
            ;;
        *)
            print_error "Opción no válida: $1"
            echo ""
            show_help
            exit 1
            ;;
    esac
}

# Ejecutar main
main "$@"
