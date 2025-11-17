@echo off
REM ============================================================================
REM Script para ejecutar tests del Clientes Service
REM ============================================================================

echo.
echo ╔══════════════════════════════════════════════════════════════════════╗
echo ║          EJECUTANDO SUITE DE TESTS - CLIENTES SERVICE                ║
echo ╚══════════════════════════════════════════════════════════════════════╝
echo.

REM Verificar que Maven esta instalado
where mvn >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Maven no esta instalado o no esta en el PATH
    echo Por favor instale Maven desde https://maven.apache.org/download.cgi
    exit /b 1
)

echo [INFO] Maven encontrado
echo.

REM Menu de opciones
:menu
echo Seleccione una opcion:
echo.
echo 1. Ejecutar TODOS los tests
echo 2. Ejecutar solo tests unitarios (ClienteServiceTest)
echo 3. Ejecutar solo tests de API (ClienteControllerTest)
echo 4. Ejecutar solo tests de integracion (ClienteRepositoryAdapterTest)
echo 5. Ejecutar tests con reporte de cobertura (JaCoCo)
echo 6. Verificar cobertura minima (70%%)
echo 7. Salir
echo.

set /p opcion="Ingrese opcion (1-7): "

if "%opcion%"=="1" goto todos
if "%opcion%"=="2" goto unitarios
if "%opcion%"=="3" goto api
if "%opcion%"=="4" goto integracion
if "%opcion%"=="5" goto cobertura
if "%opcion%"=="6" goto verificar
if "%opcion%"=="7" goto fin

echo [ERROR] Opcion invalida
goto menu

:todos
echo.
echo [INFO] Ejecutando TODOS los tests...
echo.
call mvn clean test
goto resultado

:unitarios
echo.
echo [INFO] Ejecutando tests unitarios (ClienteServiceTest)...
echo.
call mvn test -Dtest=ClienteServiceTest
goto resultado

:api
echo.
echo [INFO] Ejecutando tests de API (ClienteControllerTest)...
echo.
call mvn test -Dtest=ClienteControllerTest
goto resultado

:integracion
echo.
echo [INFO] Ejecutando tests de integracion (ClienteRepositoryAdapterTest)...
echo [NOTA] Requiere Docker en ejecucion para TestContainers
echo.
call mvn test -Dtest=ClienteRepositoryAdapterTest
goto resultado

:cobertura
echo.
echo [INFO] Ejecutando tests con reporte de cobertura...
echo.
call mvn clean test jacoco:report
echo.
echo [INFO] Reporte de cobertura generado en: target\site\jacoco\index.html
echo.
pause
goto menu

:verificar
echo.
echo [INFO] Verificando cobertura minima (70%%)...
echo.
call mvn clean verify
goto resultado

:resultado
echo.
if %ERRORLEVEL% equ 0 (
    echo ╔══════════════════════════════════════════════════════════════════════╗
    echo ║                        ✅ TESTS EXITOSOS                              ║
    echo ╚══════════════════════════════════════════════════════════════════════╝
) else (
    echo ╔══════════════════════════════════════════════════════════════════════╗
    echo ║                        ❌ TESTS FALLIDOS                              ║
    echo ╚══════════════════════════════════════════════════════════════════════╝
)
echo.
pause
goto menu

:fin
echo.
echo Saliendo...
exit /b 0
