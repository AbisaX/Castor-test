@echo off
REM ============================================================================
REM Script: delete-connectors.bat
REM Descripción: Elimina los conectores de Debezium (Windows)
REM Uso: delete-connectors.bat [opciones] [nombre-conector]
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
if "%1"=="" goto :show_help
if /I "%1"=="-d" goto :delete_connector
if /I "%1"=="--delete" goto :delete_connector
if /I "%1"=="-a" goto :delete_all
if /I "%1"=="--all" goto :delete_all
if /I "%1"=="-p" goto :pause_connector
if /I "%1"=="--pause" goto :pause_connector
if /I "%1"=="-r" goto :resume_connector
if /I "%1"=="--resume" goto :resume_connector
if /I "%1"=="-R" goto :restart_connector
if /I "%1"=="--restart" goto :restart_connector
if /I "%1"=="-l" goto :list_connectors
if /I "%1"=="--list" goto :list_connectors
if /I "%1"=="-h" goto :show_help
if /I "%1"=="--help" goto :show_help

REM Atajo: si se pasa directamente el nombre, eliminar
goto :delete_connector_direct

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
exit /b 0

REM ============================================================================
REM Verificar si un conector existe
REM ============================================================================
:connector_exists
set CONNECTOR_NAME=%~1
curl -sf "%DEBEZIUM_CONNECT_URL%/connectors/%CONNECTOR_NAME%" >nul 2>nul
exit /b %ERRORLEVEL%

REM ============================================================================
REM Eliminar conector
REM ============================================================================
:delete_connector
call :check_kafka_connect
if %ERRORLEVEL% NEQ 0 exit /b 1

if "%2"=="" (
    echo [X] Debe especificar el nombre del conector
    echo Uso: %0 --delete ^<nombre-conector^>
    exit /b 1
)

call :delete_connector_impl "%2" "false"
goto :end

:delete_connector_direct
call :check_kafka_connect
if %ERRORLEVEL% NEQ 0 exit /b 1
call :delete_connector_impl "%1" "false"
goto :end

:delete_connector_impl
set CONNECTOR_NAME=%~1
set FORCE=%~2

call :connector_exists "%CONNECTOR_NAME%"
if %ERRORLEVEL% NEQ 0 (
    echo [X] El conector no existe: %CONNECTOR_NAME%
    exit /b 1
)

if /I "%FORCE%"=="false" (
    echo [!] Esta a punto de eliminar el conector: %CONNECTOR_NAME%
    set /p CONFIRM="Esta seguro? (s/N): "
    if /I "!CONFIRM!" NEQ "s" (
        echo [i] Eliminacion cancelada
        exit /b 0
    )
)

echo [i] Eliminando conector: %CONNECTOR_NAME%
curl -s -X DELETE "%DEBEZIUM_CONNECT_URL%/connectors/%CONNECTOR_NAME%" >nul 2>nul

if %ERRORLEVEL% EQU 0 (
    echo [OK] Conector eliminado exitosamente: %CONNECTOR_NAME%
    timeout /t 2 /nobreak >nul

    REM Verificar que ya no existe
    call :connector_exists "%CONNECTOR_NAME%"
    if %ERRORLEVEL% NEQ 0 (
        echo [OK] Confirmado: El conector ya no existe
    ) else (
        echo [!] El conector aun aparece en la lista
    )
    exit /b 0
) else (
    echo [X] Error al eliminar conector: %CONNECTOR_NAME%
    exit /b 1
)

REM ============================================================================
REM Eliminar todos los conectores
REM ============================================================================
:delete_all
call :check_kafka_connect
if %ERRORLEVEL% NEQ 0 exit /b 1

echo ======================================
echo   ELIMINAR TODOS LOS CONECTORES
echo ======================================
echo.

for /f "delims=" %%i in ('curl -s "%DEBEZIUM_CONNECT_URL%/connectors"') do set CONNECTORS=%%i

if "%CONNECTORS%"=="[]" (
    echo [!] No hay conectores para eliminar
    exit /b 0
)

REM Eliminar corchetes y comillas
set CONNECTORS=%CONNECTORS:[=%
set CONNECTORS=%CONNECTORS:]=%
set CONNECTORS=%CONNECTORS:"=%

echo [!] Conectores actuales:
for %%c in (%CONNECTORS%) do (
    echo   [X] %%c
)
echo.

echo [!] Esta a punto de eliminar TODOS los conectores
set /p CONFIRM="Esta seguro? (s/N): "
if /I "%CONFIRM%" NEQ "s" (
    echo [i] Eliminacion cancelada
    exit /b 0
)

echo.
for %%c in (%CONNECTORS%) do (
    call :delete_connector_impl "%%c" "true"
)

echo.
echo [OK] Todos los conectores han sido eliminados
goto :end

REM ============================================================================
REM Pausar conector
REM ============================================================================
:pause_connector
call :check_kafka_connect
if %ERRORLEVEL% NEQ 0 exit /b 1

if "%2"=="" (
    echo [X] Debe especificar el nombre del conector
    echo Uso: %0 --pause ^<nombre-conector^>
    exit /b 1
)

call :connector_exists "%2"
if %ERRORLEVEL% NEQ 0 (
    echo [X] El conector no existe: %2
    exit /b 1
)

echo [i] Pausando conector: %2
curl -s -X PUT "%DEBEZIUM_CONNECT_URL%/connectors/%2/pause" >nul 2>nul

if %ERRORLEVEL% EQU 0 (
    echo [OK] Conector pausado exitosamente: %2
) else (
    echo [X] Error al pausar conector: %2
)
goto :end

REM ============================================================================
REM Reanudar conector
REM ============================================================================
:resume_connector
call :check_kafka_connect
if %ERRORLEVEL% NEQ 0 exit /b 1

if "%2"=="" (
    echo [X] Debe especificar el nombre del conector
    echo Uso: %0 --resume ^<nombre-conector^>
    exit /b 1
)

call :connector_exists "%2"
if %ERRORLEVEL% NEQ 0 (
    echo [X] El conector no existe: %2
    exit /b 1
)

echo [i] Reanudando conector: %2
curl -s -X PUT "%DEBEZIUM_CONNECT_URL%/connectors/%2/resume" >nul 2>nul

if %ERRORLEVEL% EQU 0 (
    echo [OK] Conector reanudado exitosamente: %2
) else (
    echo [X] Error al reanudar conector: %2
)
goto :end

REM ============================================================================
REM Reiniciar conector
REM ============================================================================
:restart_connector
call :check_kafka_connect
if %ERRORLEVEL% NEQ 0 exit /b 1

if "%2"=="" (
    echo [X] Debe especificar el nombre del conector
    echo Uso: %0 --restart ^<nombre-conector^>
    exit /b 1
)

call :connector_exists "%2"
if %ERRORLEVEL% NEQ 0 (
    echo [X] El conector no existe: %2
    exit /b 1
)

echo [i] Reiniciando conector: %2
curl -s -X POST "%DEBEZIUM_CONNECT_URL%/connectors/%2/restart" >nul 2>nul

if %ERRORLEVEL% EQU 0 (
    echo [OK] Conector reiniciado exitosamente: %2
) else (
    echo [X] Error al reiniciar conector: %2
)
goto :end

REM ============================================================================
REM Listar conectores
REM ============================================================================
:list_connectors
call :check_kafka_connect
if %ERRORLEVEL% NEQ 0 exit /b 1

echo ======================================
echo   CONECTORES ACTUALES
echo ======================================

for /f "delims=" %%i in ('curl -s "%DEBEZIUM_CONNECT_URL%/connectors"') do set CONNECTORS=%%i

if "%CONNECTORS%"=="[]" (
    echo [!] No hay conectores desplegados
    exit /b 0
)

REM Eliminar corchetes y comillas
set CONNECTORS=%CONNECTORS:[=%
set CONNECTORS=%CONNECTORS:]=%
set CONNECTORS=%CONNECTORS:"=%

for %%c in (%CONNECTORS%) do (
    echo [OK] %%c
)
goto :end

REM ============================================================================
REM Mostrar ayuda
REM ============================================================================
:show_help
echo Uso: %0 [opciones] [nombre-conector]
echo.
echo Opciones:
echo   -d, --delete ^<nombre^>   Eliminar un conector especifico
echo   -a, --all               Eliminar todos los conectores
echo   -p, --pause ^<nombre^>    Pausar un conector
echo   -r, --resume ^<nombre^>   Reanudar un conector pausado
echo   -R, --restart ^<nombre^>  Reiniciar un conector
echo   -l, --list              Listar conectores actuales
echo   -h, --help              Mostrar esta ayuda
echo.
echo Ejemplos:
echo   %0 --delete postgres-clientes-source
echo   %0 --all
echo   %0 --pause postgres-clientes-source
echo   %0 --restart oracle-clientes-sink
echo.
echo Atajos:
echo   %0 ^<nombre^>             Eliminar conector especificado
goto :end

:end
endlocal
exit /b 0
