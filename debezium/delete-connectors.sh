#!/bin/bash

###############################################################################
# Script: delete-connectors.sh
# Descripción: Elimina los conectores de Debezium
# Uso: ./delete-connectors.sh [nombre-conector]
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
    if ! curl -sf "${DEBEZIUM_CONNECT_URL}/" > /dev/null 2>&1; then
        print_error "Kafka Connect no está disponible en ${DEBEZIUM_CONNECT_URL}"
        print_info "Verifica que el contenedor debezium-connect esté ejecutándose"
        return 1
    fi
    return 0
}

# Listar todos los conectores
list_connectors() {
    local connectors=$(curl -s "${DEBEZIUM_CONNECT_URL}/connectors")

    if [ -z "$connectors" ] || [ "$connectors" = "[]" ]; then
        return 1
    fi

    echo "$connectors" | jq -r '.[]' 2>/dev/null
    return 0
}

# Verificar si un conector existe
connector_exists() {
    local connector_name=$1

    if curl -sf "${DEBEZIUM_CONNECT_URL}/connectors/${connector_name}" > /dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

# Eliminar conector
delete_connector() {
    local connector_name=$1
    local force=${2:-false}

    if ! connector_exists "$connector_name"; then
        print_error "El conector no existe: $connector_name"
        return 1
    fi

    # Confirmar eliminación si no es forzado
    if [ "$force" = false ]; then
        print_warning "Está a punto de eliminar el conector: $connector_name"
        read -p "¿Está seguro? (s/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Ss]$ ]]; then
            print_info "Eliminación cancelada"
            return 0
        fi
    fi

    # Eliminar el conector
    local response=$(curl -s -w "\n%{http_code}" -X DELETE "${DEBEZIUM_CONNECT_URL}/connectors/${connector_name}")
    local http_code=$(echo "$response" | tail -n 1)

    if [ "$http_code" -eq 204 ] || [ "$http_code" -eq 200 ]; then
        print_success "Conector eliminado exitosamente: $connector_name"

        # Esperar a que se complete la eliminación
        sleep 2

        # Verificar que ya no existe
        if ! connector_exists "$connector_name"; then
            print_success "Confirmado: El conector ya no existe"
        else
            print_warning "El conector aún aparece en la lista, puede tardar unos segundos en eliminarse completamente"
        fi

        return 0
    else
        print_error "Error al eliminar conector: $connector_name"
        print_error "HTTP Code: $http_code"
        local body=$(echo "$response" | sed '$d')
        print_error "Response: $body"
        return 1
    fi
}

# Pausar conector
pause_connector() {
    local connector_name=$1

    if ! connector_exists "$connector_name"; then
        print_error "El conector no existe: $connector_name"
        return 1
    fi

    print_info "Pausando conector: $connector_name"

    local response=$(curl -s -w "\n%{http_code}" -X PUT "${DEBEZIUM_CONNECT_URL}/connectors/${connector_name}/pause")
    local http_code=$(echo "$response" | tail -n 1)

    if [ "$http_code" -eq 202 ] || [ "$http_code" -eq 200 ]; then
        print_success "Conector pausado exitosamente: $connector_name"
        return 0
    else
        print_error "Error al pausar conector: $connector_name"
        print_error "HTTP Code: $http_code"
        return 1
    fi
}

# Reanudar conector
resume_connector() {
    local connector_name=$1

    if ! connector_exists "$connector_name"; then
        print_error "El conector no existe: $connector_name"
        return 1
    fi

    print_info "Reanudando conector: $connector_name"

    local response=$(curl -s -w "\n%{http_code}" -X PUT "${DEBEZIUM_CONNECT_URL}/connectors/${connector_name}/resume")
    local http_code=$(echo "$response" | tail -n 1)

    if [ "$http_code" -eq 202 ] || [ "$http_code" -eq 200 ]; then
        print_success "Conector reanudado exitosamente: $connector_name"
        return 0
    else
        print_error "Error al reanudar conector: $connector_name"
        print_error "HTTP Code: $http_code"
        return 1
    fi
}

# Reiniciar conector
restart_connector() {
    local connector_name=$1

    if ! connector_exists "$connector_name"; then
        print_error "El conector no existe: $connector_name"
        return 1
    fi

    print_info "Reiniciando conector: $connector_name"

    local response=$(curl -s -w "\n%{http_code}" -X POST "${DEBEZIUM_CONNECT_URL}/connectors/${connector_name}/restart")
    local http_code=$(echo "$response" | tail -n 1)

    if [ "$http_code" -eq 204 ] || [ "$http_code" -eq 200 ]; then
        print_success "Conector reiniciado exitosamente: $connector_name"
        return 0
    else
        print_error "Error al reiniciar conector: $connector_name"
        print_error "HTTP Code: $http_code"
        return 1
    fi
}

# Eliminar todos los conectores
delete_all_connectors() {
    print_header "ELIMINAR TODOS LOS CONECTORES"

    local connectors=$(list_connectors)

    if [ -z "$connectors" ]; then
        print_warning "No hay conectores para eliminar"
        return 0
    fi

    print_warning "Conectores actuales:"
    echo "$connectors" | while read -r connector; do
        echo -e "  ${RED}●${NC} $connector"
    done

    echo ""
    print_warning "Está a punto de eliminar TODOS los conectores"
    read -p "¿Está seguro? (s/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Ss]$ ]]; then
        print_info "Eliminación cancelada"
        return 0
    fi

    echo "$connectors" | while read -r connector; do
        if [ ! -z "$connector" ]; then
            delete_connector "$connector" true
        fi
    done

    print_success "Todos los conectores han sido eliminados"
}

# Mostrar ayuda
show_help() {
    echo "Uso: $0 [opciones] [nombre-conector]"
    echo ""
    echo "Opciones:"
    echo "  -d, --delete [nombre]   Eliminar un conector específico"
    echo "  -a, --all               Eliminar todos los conectores"
    echo "  -p, --pause [nombre]    Pausar un conector"
    echo "  -r, --resume [nombre]   Reanudar un conector pausado"
    echo "  -R, --restart [nombre]  Reiniciar un conector"
    echo "  -l, --list              Listar conectores actuales"
    echo "  -h, --help              Mostrar esta ayuda"
    echo ""
    echo "Ejemplos:"
    echo "  $0 --delete postgres-clientes-source"
    echo "  $0 --all"
    echo "  $0 --pause postgres-clientes-source"
    echo "  $0 --restart oracle-clientes-sink"
    echo ""
    echo "Atajos:"
    echo "  $0 [nombre]             Eliminar conector especificado"
}

###############################################################################
# Main
###############################################################################

main() {
    # Verificar que Kafka Connect está disponible
    if ! check_kafka_connect; then
        exit 1
    fi

    # Si no hay argumentos, mostrar ayuda
    if [ $# -eq 0 ]; then
        show_help
        exit 0
    fi

    # Procesar argumentos
    case "$1" in
        -d|--delete)
            if [ -z "$2" ]; then
                print_error "Debe especificar el nombre del conector"
                exit 1
            fi
            delete_connector "$2"
            ;;
        -a|--all)
            delete_all_connectors
            ;;
        -p|--pause)
            if [ -z "$2" ]; then
                print_error "Debe especificar el nombre del conector"
                exit 1
            fi
            pause_connector "$2"
            ;;
        -r|--resume)
            if [ -z "$2" ]; then
                print_error "Debe especificar el nombre del conector"
                exit 1
            fi
            resume_connector "$2"
            ;;
        -R|--restart)
            if [ -z "$2" ]; then
                print_error "Debe especificar el nombre del conector"
                exit 1
            fi
            restart_connector "$2"
            ;;
        -l|--list)
            print_header "CONECTORES ACTUALES"
            local connectors=$(list_connectors)
            if [ -z "$connectors" ]; then
                print_warning "No hay conectores desplegados"
            else
                echo "$connectors" | while read -r connector; do
                    echo -e "${GREEN}●${NC} $connector"
                done
            fi
            ;;
        -h|--help)
            show_help
            ;;
        *)
            # Atajo: si se pasa directamente el nombre, intentar eliminar
            delete_connector "$1"
            ;;
    esac
}

# Ejecutar main
main "$@"
