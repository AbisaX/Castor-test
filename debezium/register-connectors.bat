@echo off
REM Script para registrar los conectores de Debezium en Windows
REM Ejecutar después de que el stack esté completamente levantado

setlocal enabledelayedexpansion

set DEBEZIUM_URL=http://localhost:8083
set CONNECTORS_DIR=.\connectors

echo ======================================
echo   REGISTRO DE CONECTORES DEBEZIUM
echo ======================================
echo.

REM Verificar que curl esté disponible
where curl >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Error: curl no esta disponible en el sistema
    exit /b 1
)

REM Esperar a que Debezium Connect esté listo
echo Esperando a que Debezium Connect este listo...
set MAX_ATTEMPTS=30
set ATTEMPT=1

:wait_loop
curl -s -f "%DEBEZIUM_URL%/" >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo OK - Debezium Connect esta listo
    goto :debezium_ready
)
echo   Intento %ATTEMPT% de %MAX_ATTEMPTS%...
timeout /t 5 /nobreak >nul
set /a ATTEMPT+=1
if %ATTEMPT% LEQ %MAX_ATTEMPTS% goto :wait_loop

echo Error: Debezium Connect no esta disponible
exit /b 1

:debezium_ready
echo.

REM Registrar PostgreSQL Source Connector
echo Registrando PostgreSQL Source Connector...
echo --------------------------------------

curl -s "%DEBEZIUM_URL%/connectors/postgres-clientes-source" >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo El conector ya existe. Eliminando...
    curl -X DELETE "%DEBEZIUM_URL%/connectors/postgres-clientes-source" >nul 2>nul
    timeout /t 2 /nobreak >nul
)

curl -X POST ^
    -H "Content-Type: application/json" ^
    --data @"%CONNECTORS_DIR%\postgres-source-connector.json" ^
    "%DEBEZIUM_URL%/connectors"

if %ERRORLEVEL% EQU 0 (
    echo OK - PostgreSQL Source Connector registrado
) else (
    echo Error al registrar PostgreSQL Source Connector
)

echo.
echo Estado del conector:
curl -s "%DEBEZIUM_URL%/connectors/postgres-clientes-source/status"
echo.

REM Esperar antes de registrar el sink
timeout /t 5 /nobreak >nul

REM Registrar Oracle Sink Connector
echo.
echo Registrando Oracle Sink Connector...
echo --------------------------------------

curl -s "%DEBEZIUM_URL%/connectors/oracle-clientes-sink" >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo El conector ya existe. Eliminando...
    curl -X DELETE "%DEBEZIUM_URL%/connectors/oracle-clientes-sink" >nul 2>nul
    timeout /t 2 /nobreak >nul
)

curl -X POST ^
    -H "Content-Type: application/json" ^
    --data @"%CONNECTORS_DIR%\oracle-sink-connector.json" ^
    "%DEBEZIUM_URL%/connectors"

if %ERRORLEVEL% EQU 0 (
    echo OK - Oracle Sink Connector registrado
) else (
    echo Error al registrar Oracle Sink Connector
)

echo.
echo Estado del conector:
curl -s "%DEBEZIUM_URL%/connectors/oracle-clientes-sink/status"
echo.

echo.
echo ======================================
echo   REGISTRO COMPLETADO
echo ======================================
echo.
echo Comandos utiles:
echo   - Listar conectores: curl http://localhost:8083/connectors
echo   - Estado de conector: curl http://localhost:8083/connectors/^<nombre^>/status
echo   - Eliminar conector: curl -X DELETE http://localhost:8083/connectors/^<nombre^>
echo   - Ver topicos de Kafka: docker exec castor-kafka kafka-topics --bootstrap-server localhost:9092 --list
echo.

endlocal
