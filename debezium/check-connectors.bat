@echo off
REM ============================================================================
REM Script: check-connectors.bat
REM Descripción: Verifica el estado de los conectores de Debezium (Windows)
REM Uso: check-connectors.bat [opciones]
REM ============================================================================

setlocal enabledelayedexpansion

set DEBEZIUM_CONNECT_URL=http://localhost:8083

REM Verificar que curl esté disponible
where curl >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [X] curl no esta disponible en el sistema
    echo Instala curl desde: https://curl.se/windows/
    exit /b 1
)

REM Procesar argumentos
if "%1"=="" goto :default_action
if /I "%1"=="-l" goto :list_connectors
if /I "%1"=="--list" goto :list_connectors
if /I "%1"=="-s" goto :status_connector
if /I "%1"=="--status" goto :status_connector
if /I "%1"=="-d" goto :check_databases
if /I "%1"=="--databases" goto :check_databases
if /I "%1"=="-t" goto :check_topics
if /I "%1"=="--topics" goto :check_topics
if /I "%1"=="-h" goto :show_help
if /I "%1"=="--help" goto :show_help
goto :show_help

REM ============================================================================
REM Default action: mostrar resumen
REM ============================================================================
:default_action
call :check_kafka_connect
if %ERRORLEVEL% NEQ 0 exit /b 1

echo.
echo ======================================
echo   CONECTORES DESPLEGADOS
echo ======================================
call :list_all_connectors

echo.
echo ======================================
echo   CONECTIVIDAD CON BASES DE DATOS
echo ======================================
call :check_database_connectivity

echo.
echo ======================================
echo   TOPICS DE KAFKA
echo ======================================
call :check_kafka_topics

goto :end

REM ============================================================================
REM Verificar que Kafka Connect está disponible
REM ============================================================================
:check_kafka_connect
curl -sf "%DEBEZIUM_CONNECT_URL%/" >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [X] Kafka Connect no esta disponible en %DEBEZIUM_CONNECT_URL%
    echo [i] Verifica que el contenedor debezium-connect este ejecutandose
    exit /b 1
)
echo [OK] Kafka Connect esta disponible
exit /b 0

REM ============================================================================
REM Listar todos los conectores
REM ============================================================================
:list_connectors
call :check_kafka_connect
if %ERRORLEVEL% NEQ 0 exit /b 1
echo.
echo ======================================
echo   CONECTORES DESPLEGADOS
echo ======================================
call :list_all_connectors
goto :end

:list_all_connectors
for /f "delims=" %%i in ('curl -s "%DEBEZIUM_CONNECT_URL%/connectors"') do set CONNECTORS=%%i

if "%CONNECTORS%"=="[]" (
    echo [!] No hay conectores desplegados
    exit /b 0
)

REM Eliminar corchetes y comillas
set CONNECTORS=%CONNECTORS:[=%
set CONNECTORS=%CONNECTORS:]=%
set CONNECTORS=%CONNECTORS:"=%

REM Mostrar cada conector
for %%c in (%CONNECTORS%) do (
    echo [OK] %%c
)
exit /b 0

REM ============================================================================
REM Mostrar estado de un conector
REM ============================================================================
:status_connector
call :check_kafka_connect
if %ERRORLEVEL% NEQ 0 exit /b 1

if "%2"=="" (
    echo [X] Debe especificar el nombre del conector
    echo Uso: %0 --status ^<nombre-conector^>
    exit /b 1
)

echo.
echo ======================================
echo   ESTADO DEL CONECTOR: %2
echo ======================================

curl -s "%DEBEZIUM_CONNECT_URL%/connectors/%2/status"
echo.
goto :end

REM ============================================================================
REM Verificar conectividad con bases de datos
REM ============================================================================
:check_databases
call :check_kafka_connect
if %ERRORLEVEL% NEQ 0 exit /b 1
echo.
echo ======================================
echo   CONECTIVIDAD CON BASES DE DATOS
echo ======================================
call :check_database_connectivity
goto :end

:check_database_connectivity
echo.
echo PostgreSQL:
docker exec castor-postgres pg_isready -U castor_user -d facturacion_db >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo [OK] PostgreSQL esta disponible
) else (
    echo [X] PostgreSQL no esta disponible
)

echo.
echo Oracle:
docker exec castor-oracle healthcheck.sh >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo [OK] Oracle esta disponible
) else (
    echo [X] Oracle no esta disponible
)

echo.
echo Kafka:
docker exec castor-kafka kafka-broker-api-versions --bootstrap-server localhost:9092 >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo [OK] Kafka esta disponible
) else (
    echo [X] Kafka no esta disponible
)
exit /b 0

REM ============================================================================
REM Verificar topics de Kafka
REM ============================================================================
:check_topics
call :check_kafka_connect
if %ERRORLEVEL% NEQ 0 exit /b 1
echo.
echo ======================================
echo   TOPICS DE KAFKA
echo ======================================
call :check_kafka_topics
goto :end

:check_kafka_topics
for /f "delims=" %%i in ('docker exec castor-kafka kafka-topics --bootstrap-server localhost:9092 --list 2^>nul ^| findstr "castor\."') do (
    echo [OK] %%i
)

if %ERRORLEVEL% NEQ 0 (
    echo [!] No se encontraron topics CDC
)
exit /b 0

REM ============================================================================
REM Mostrar ayuda
REM ============================================================================
:show_help
echo Uso: %0 [opciones]
echo.
echo Opciones:
echo   -l, --list              Listar todos los conectores
echo   -s, --status ^<nombre^>   Mostrar estado de un conector
echo   -d, --databases         Verificar conectividad con bases de datos
echo   -t, --topics            Listar topics de Kafka
echo   -h, --help              Mostrar esta ayuda
echo.
echo Ejemplos:
echo   %0
echo   %0 --list
echo   %0 --status postgres-clientes-source
echo   %0 --databases
goto :end

:end
endlocal
exit /b 0
