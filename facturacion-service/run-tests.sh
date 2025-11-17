#!/bin/bash

# Script para ejecutar la suite de tests del servicio de facturacion
# Autor: Claude Code
# Fecha: 2025-11-16

set -e

echo "========================================"
echo "Suite de Tests - Facturacion Service"
echo "========================================"
echo ""

# Verificar que Maven esta instalado
if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven no esta instalado o no esta en el PATH"
    echo "Por favor, instale Maven desde https://maven.apache.org/"
    exit 1
fi

# Verificar que Docker esta corriendo (necesario para TestContainers)
if ! docker info &> /dev/null; then
    echo "ADVERTENCIA: Docker no esta corriendo"
    echo "Los tests de integracion con Oracle (TestContainers) fallaran"
    echo "Por favor, inicie Docker"
    echo ""
    read -p "Desea continuar de todas formas? (s/n): " continue
    if [ "$continue" != "s" ] && [ "$continue" != "S" ]; then
        exit 1
    fi
fi

echo "Opcion 1: Ejecutar todos los tests"
echo "Opcion 2: Solo tests unitarios (sin Docker)"
echo "Opcion 3: Solo tests de integracion (con Docker)"
echo "Opcion 4: Generar reporte de cobertura"
echo "Opcion 5: Verificar umbral de cobertura (70%)"
echo ""

read -p "Seleccione una opcion (1-5): " opcion

case $opcion in
    1)
        echo ""
        echo "Ejecutando TODOS los tests..."
        echo ""
        mvn clean test
        ;;
    2)
        echo ""
        echo "Ejecutando tests UNITARIOS..."
        echo ""
        mvn test -Dtest=FacturaServiceTest,FacturaControllerTest,ClienteValidationAdapterTest,TaxCalculatorAdapterTest
        ;;
    3)
        echo ""
        echo "Ejecutando tests de INTEGRACION (requiere Docker)..."
        echo ""
        mvn test -Dtest=FacturaRepositoryAdapterTest
        ;;
    4)
        echo ""
        echo "Generando reporte de cobertura..."
        echo ""
        mvn clean test jacoco:report
        echo ""
        echo "Reporte generado en: target/site/jacoco/index.html"

        # Abrir reporte en navegador
        if command -v xdg-open &> /dev/null; then
            xdg-open target/site/jacoco/index.html
        elif command -v open &> /dev/null; then
            open target/site/jacoco/index.html
        else
            echo "Abra manualmente el archivo target/site/jacoco/index.html"
        fi
        ;;
    5)
        echo ""
        echo "Verificando umbral de cobertura (70%)..."
        echo ""
        mvn clean test jacoco:check
        ;;
    *)
        echo "Opcion invalida"
        exit 1
        ;;
esac

echo ""
echo "========================================"
echo "Ejecucion completada"
echo "========================================"
