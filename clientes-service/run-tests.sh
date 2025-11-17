#!/bin/bash
# ============================================================================
# Script para ejecutar tests del Clientes Service
# ============================================================================

set -e

echo ""
echo "╔══════════════════════════════════════════════════════════════════════╗"
echo "║          EJECUTANDO SUITE DE TESTS - CLIENTES SERVICE                ║"
echo "╚══════════════════════════════════════════════════════════════════════╝"
echo ""

# Verificar que Maven esta instalado
if ! command -v mvn &> /dev/null; then
    echo "[ERROR] Maven no esta instalado o no esta en el PATH"
    echo "Por favor instale Maven desde https://maven.apache.org/download.cgi"
    exit 1
fi

echo "[INFO] Maven encontrado: $(mvn --version | head -n 1)"
echo ""

# Funcion para mostrar el menu
show_menu() {
    echo "Seleccione una opcion:"
    echo ""
    echo "1. Ejecutar TODOS los tests"
    echo "2. Ejecutar solo tests unitarios (ClienteServiceTest)"
    echo "3. Ejecutar solo tests de API (ClienteControllerTest)"
    echo "4. Ejecutar solo tests de integracion (ClienteRepositoryAdapterTest)"
    echo "5. Ejecutar tests con reporte de cobertura (JaCoCo)"
    echo "6. Verificar cobertura minima (70%)"
    echo "7. Salir"
    echo ""
}

# Funcion para mostrar resultado
show_result() {
    if [ $? -eq 0 ]; then
        echo ""
        echo "╔══════════════════════════════════════════════════════════════════════╗"
        echo "║                        ✅ TESTS EXITOSOS                              ║"
        echo "╚══════════════════════════════════════════════════════════════════════╝"
        echo ""
    else
        echo ""
        echo "╔══════════════════════════════════════════════════════════════════════╗"
        echo "║                        ❌ TESTS FALLIDOS                              ║"
        echo "╚══════════════════════════════════════════════════════════════════════╝"
        echo ""
    fi
}

# Loop principal
while true; do
    show_menu
    read -p "Ingrese opcion (1-7): " opcion

    case $opcion in
        1)
            echo ""
            echo "[INFO] Ejecutando TODOS los tests..."
            echo ""
            mvn clean test
            show_result
            ;;
        2)
            echo ""
            echo "[INFO] Ejecutando tests unitarios (ClienteServiceTest)..."
            echo ""
            mvn test -Dtest=ClienteServiceTest
            show_result
            ;;
        3)
            echo ""
            echo "[INFO] Ejecutando tests de API (ClienteControllerTest)..."
            echo ""
            mvn test -Dtest=ClienteControllerTest
            show_result
            ;;
        4)
            echo ""
            echo "[INFO] Ejecutando tests de integracion (ClienteRepositoryAdapterTest)..."
            echo "[NOTA] Requiere Docker en ejecucion para TestContainers"
            echo ""
            mvn test -Dtest=ClienteRepositoryAdapterTest
            show_result
            ;;
        5)
            echo ""
            echo "[INFO] Ejecutando tests con reporte de cobertura..."
            echo ""
            mvn clean test jacoco:report
            echo ""
            echo "[INFO] Reporte de cobertura generado en: target/site/jacoco/index.html"
            echo ""
            # Intentar abrir el reporte en el navegador
            if [[ "$OSTYPE" == "darwin"* ]]; then
                open target/site/jacoco/index.html 2>/dev/null || true
            elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
                xdg-open target/site/jacoco/index.html 2>/dev/null || true
            fi
            read -p "Presione ENTER para continuar..."
            ;;
        6)
            echo ""
            echo "[INFO] Verificando cobertura minima (70%)..."
            echo ""
            mvn clean verify
            show_result
            ;;
        7)
            echo ""
            echo "Saliendo..."
            exit 0
            ;;
        *)
            echo "[ERROR] Opcion invalida"
            echo ""
            ;;
    esac
done
