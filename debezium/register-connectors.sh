#!/bin/bash
#
# Script para registrar los conectores de Debezium
# Ejecutar después de que el stack esté completamente levantado
#

set -e

DEBEZIUM_URL="http://localhost:8083"
CONNECTORS_DIR="./connectors"

echo "======================================"
echo "  REGISTRO DE CONECTORES DEBEZIUM"
echo "======================================"
echo ""

# Función para esperar que Debezium Connect esté listo
wait_for_connect() {
    echo "Esperando a que Debezium Connect esté listo..."
    local max_attempts=30
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        if curl -s -f "${DEBEZIUM_URL}/" > /dev/null 2>&1; then
            echo "✓ Debezium Connect está listo"
            return 0
        fi
        echo "  Intento $attempt de $max_attempts..."
        sleep 5
        ((attempt++))
    done

    echo "✗ Error: Debezium Connect no está disponible"
    exit 1
}

# Función para verificar si un conector existe
connector_exists() {
    local connector_name=$1
    curl -s "${DEBEZIUM_URL}/connectors" | grep -q "\"${connector_name}\""
}

# Función para registrar un conector
register_connector() {
    local connector_file=$1
    local connector_name=$(jq -r '.name' "${connector_file}")

    echo ""
    echo "Registrando conector: ${connector_name}"
    echo "--------------------------------------"

    if connector_exists "${connector_name}"; then
        echo "⚠ El conector ya existe. Eliminando..."
        curl -X DELETE "${DEBEZIUM_URL}/connectors/${connector_name}" 2>/dev/null
        sleep 2
    fi

    local response=$(curl -s -X POST \
        -H "Content-Type: application/json" \
        --data @"${connector_file}" \
        "${DEBEZIUM_URL}/connectors")

    if echo "${response}" | grep -q "\"name\""; then
        echo "✓ Conector registrado exitosamente"
        echo "${response}" | jq '.'
    else
        echo "✗ Error al registrar conector"
        echo "${response}" | jq '.'
        return 1
    fi
}

# Función para verificar estado de un conector
check_connector_status() {
    local connector_name=$1
    echo ""
    echo "Estado del conector: ${connector_name}"
    echo "--------------------------------------"
    curl -s "${DEBEZIUM_URL}/connectors/${connector_name}/status" | jq '.'
}

# Main
wait_for_connect

echo ""
echo "Conectores disponibles para registrar:"
echo "--------------------------------------"
ls -1 "${CONNECTORS_DIR}"/*.json

# Registrar PostgreSQL Source Connector
if [ -f "${CONNECTORS_DIR}/postgres-source-connector.json" ]; then
    register_connector "${CONNECTORS_DIR}/postgres-source-connector.json"
    check_connector_status "postgres-clientes-source"
fi

# Esperar un momento antes de registrar el sink
sleep 5

# Registrar Oracle Sink Connector
if [ -f "${CONNECTORS_DIR}/oracle-sink-connector.json" ]; then
    register_connector "${CONNECTORS_DIR}/oracle-sink-connector.json"
    check_connector_status "oracle-clientes-sink"
fi

echo ""
echo "======================================"
echo "  REGISTRO COMPLETADO"
echo "======================================"
echo ""
echo "Comandos útiles:"
echo "  - Listar conectores: curl http://localhost:8083/connectors"
echo "  - Estado de conector: curl http://localhost:8083/connectors/<nombre>/status"
echo "  - Eliminar conector: curl -X DELETE http://localhost:8083/connectors/<nombre>"
echo "  - Ver tópicos de Kafka: docker exec castor-kafka kafka-topics --bootstrap-server localhost:9092 --list"
echo ""
