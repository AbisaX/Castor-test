@echo off
REM Script para ejecutar la suite de tests del servicio de facturacion
REM Autor: Claude Code
REM Fecha: 2025-11-16

echo ========================================
echo Suite de Tests - Facturacion Service
echo ========================================
echo.

REM Verificar que Maven esta instalado
where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Maven no esta instalado o no esta en el PATH
    echo Por favor, instale Maven desde https://maven.apache.org/
    pause
    exit /b 1
)

REM Verificar que Docker esta corriendo (necesario para TestContainers)
docker info >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ADVERTENCIA: Docker no esta corriendo
    echo Los tests de integracion con Oracle (TestContainers) fallaran
    echo Por favor, inicie Docker Desktop
    echo.
    echo Desea continuar de todas formas? (S/N)
    set /p continue=
    if /i not "%continue%"=="S" exit /b 1
)

echo Opcion 1: Ejecutar todos los tests
echo Opcion 2: Solo tests unitarios (sin Docker)
echo Opcion 3: Solo tests de integracion (con Docker)
echo Opcion 4: Generar reporte de cobertura
echo Opcion 5: Verificar umbral de cobertura (70%%)
echo.

set /p opcion="Seleccione una opcion (1-5): "

if "%opcion%"=="1" (
    echo.
    echo Ejecutando TODOS los tests...
    echo.
    mvn clean test
) else if "%opcion%"=="2" (
    echo.
    echo Ejecutando tests UNITARIOS...
    echo.
    mvn test -Dtest=FacturaServiceTest,FacturaControllerTest,ClienteValidationAdapterTest,TaxCalculatorAdapterTest
) else if "%opcion%"=="3" (
    echo.
    echo Ejecutando tests de INTEGRACION (requiere Docker)...
    echo.
    mvn test -Dtest=FacturaRepositoryAdapterTest
) else if "%opcion%"=="4" (
    echo.
    echo Generando reporte de cobertura...
    echo.
    mvn clean test jacoco:report
    echo.
    echo Reporte generado en: target\site\jacoco\index.html
    echo Abriendo reporte en navegador...
    start target\site\jacoco\index.html
) else if "%opcion%"=="5" (
    echo.
    echo Verificando umbral de cobertura (70%%)...
    echo.
    mvn clean test jacoco:check
) else (
    echo Opcion invalida
    pause
    exit /b 1
)

echo.
echo ========================================
echo Ejecucion completada
echo ========================================
pause
