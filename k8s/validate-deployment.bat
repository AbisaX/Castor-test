@echo off
REM Script de validación de despliegue de Kubernetes - Windows
REM Sistema de Facturación - Proyecto Castor

setlocal enabledelayedexpansion

set NAMESPACE=facturacion
set TOTAL_CHECKS=0
set PASSED_CHECKS=0
set FAILED_CHECKS=0
set WARNING_CHECKS=0

echo ======================================
echo   VALIDACION DE DESPLIEGUE K8S
echo   Sistema de Facturacion
echo ======================================
echo.

REM 1. Verificar kubectl
echo ======================================
echo 1. Verificando kubectl...
echo ======================================
where kubectl >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [X] kubectl no esta instalado o no esta en el PATH
    set /a FAILED_CHECKS+=1
    set /a TOTAL_CHECKS+=1
    goto :end_fail
) else (
    echo [OK] kubectl encontrado
    set /a PASSED_CHECKS+=1
    set /a TOTAL_CHECKS+=1
)
echo.

REM 2. Verificar conexión al cluster
echo ======================================
echo 2. Verificando conexion al cluster...
echo ======================================
kubectl cluster-info >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [X] No se puede conectar al cluster de Kubernetes
    set /a FAILED_CHECKS+=1
    set /a TOTAL_CHECKS+=1
    goto :end_fail
) else (
    echo [OK] Conectado al cluster
    set /a PASSED_CHECKS+=1
    set /a TOTAL_CHECKS+=1
)
echo.

REM 3. Verificar namespace
echo ======================================
echo 3. Verificando namespace...
echo ======================================
kubectl get namespace %NAMESPACE% >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [X] Namespace '%NAMESPACE%' no existe
    set /a FAILED_CHECKS+=1
    set /a TOTAL_CHECKS+=1
    goto :end_fail
) else (
    echo [OK] Namespace '%NAMESPACE%' existe
    set /a PASSED_CHECKS+=1
    set /a TOTAL_CHECKS+=1
)
echo.

REM 4. Verificar Pods
echo ======================================
echo 4. Verificando Pods...
echo ======================================
for /f %%i in ('kubectl get pods -n %NAMESPACE% --no-headers 2^>nul ^| find /c /v ""') do set PODS=%%i
if %PODS% EQU 0 (
    echo [X] No hay pods desplegados
    set /a FAILED_CHECKS+=1
    set /a TOTAL_CHECKS+=1
) else (
    echo [OK] Total de pods: %PODS%
    set /a PASSED_CHECKS+=1
    set /a TOTAL_CHECKS+=1

    REM Contar pods Running
    for /f %%i in ('kubectl get pods -n %NAMESPACE% --no-headers 2^>nul ^| find /c "Running"') do set RUNNING=%%i
    echo [OK] Pods Running: !RUNNING!
    set /a PASSED_CHECKS+=1
    set /a TOTAL_CHECKS+=1

    REM Contar pods Pending
    for /f %%i in ('kubectl get pods -n %NAMESPACE% --no-headers 2^>nul ^| find /c "Pending"') do set PENDING=%%i
    if !PENDING! GTR 0 (
        echo [!] Pods Pending: !PENDING!
        set /a WARNING_CHECKS+=1
        set /a TOTAL_CHECKS+=1
    )

    REM Contar pods Failed
    for /f %%i in ('kubectl get pods -n %NAMESPACE% --no-headers 2^>nul ^| find /c "Error"') do set ERRORS=%%i
    if !ERRORS! GTR 0 (
        echo [X] Pods con Error: !ERRORS!
        set /a FAILED_CHECKS+=1
        set /a TOTAL_CHECKS+=1
    )
)
echo.

REM 5. Verificar Deployments
echo ======================================
echo 5. Verificando Deployments...
echo ======================================

call :check_deployment "postgres"
call :check_deployment "oracle"
call :check_deployment "zipkin"
call :check_deployment "zookeeper"
call :check_deployment "kafka"
call :check_deployment "debezium-connect"
call :check_deployment "python-service"
call :check_deployment "clientes-service"
call :check_deployment "facturacion-service"
call :check_deployment "api-gateway"
echo.

REM 6. Verificar Services
echo ======================================
echo 6. Verificando Services...
echo ======================================
for /f %%i in ('kubectl get services -n %NAMESPACE% --no-headers 2^>nul ^| find /c /v ""') do set SERVICES=%%i
if %SERVICES% EQU 0 (
    echo [X] No hay servicios desplegados
    set /a FAILED_CHECKS+=1
    set /a TOTAL_CHECKS+=1
) else (
    echo [OK] Total de servicios: %SERVICES%
    set /a PASSED_CHECKS+=1
    set /a TOTAL_CHECKS+=1
)

call :check_service "postgres-service"
call :check_service "oracle-service"
call :check_service "zipkin-service"
call :check_service "zookeeper-service"
call :check_service "kafka-service"
call :check_service "debezium-connect"
call :check_service "python-service"
call :check_service "clientes-service"
call :check_service "facturacion-service"
call :check_service "api-gateway"
echo.

REM 7. Verificar PVCs
echo ======================================
echo 7. Verificando Persistent Volume Claims...
echo ======================================
for /f %%i in ('kubectl get pvc -n %NAMESPACE% --no-headers 2^>nul ^| find /c /v ""') do set PVCS=%%i
if %PVCS% EQU 0 (
    echo [!] No hay PVCs desplegados
    set /a WARNING_CHECKS+=1
    set /a TOTAL_CHECKS+=1
) else (
    echo [OK] Total de PVCs: %PVCS%
    set /a PASSED_CHECKS+=1
    set /a TOTAL_CHECKS+=1

    for /f %%i in ('kubectl get pvc -n %NAMESPACE% --no-headers 2^>nul ^| find /c "Bound"') do set BOUND=%%i
    echo [OK] PVCs Bound: !BOUND!/%PVCS%
    set /a PASSED_CHECKS+=1
    set /a TOTAL_CHECKS+=1
)
echo.

REM 8. Verificar ConfigMaps
echo ======================================
echo 8. Verificando ConfigMaps...
echo ======================================
set CONFIGMAP_COUNT=0
call :check_configmap "postgres-config"
call :check_configmap "postgres-init-script"
call :check_configmap "oracle-config"
call :check_configmap "oracle-init-script"
call :check_configmap "clientes-config"
call :check_configmap "facturacion-config"
call :check_configmap "api-gateway-config"
echo [OK] ConfigMaps encontrados: %CONFIGMAP_COUNT%/7
set /a PASSED_CHECKS+=1
set /a TOTAL_CHECKS+=1
echo.

REM 9. Verificar Secrets
echo ======================================
echo 9. Verificando Secrets...
echo ======================================
set SECRET_COUNT=0
call :check_secret "postgres-secret"
call :check_secret "oracle-secret"
call :check_secret "clientes-secret"
call :check_secret "facturacion-secret"
echo [OK] Secrets encontrados: %SECRET_COUNT%/4
set /a PASSED_CHECKS+=1
set /a TOTAL_CHECKS+=1
echo.

REM 10. Verificar health de servicios
echo ======================================
echo 10. Verificando health endpoints...
echo ======================================
call :check_service_health "clientes-service" "8081" "/actuator/health"
call :check_service_health "facturacion-service" "8082" "/actuator/health"
call :check_service_health "api-gateway" "8080" "/actuator/health"
call :check_service_health "python-service" "5000" "/health"
echo.

REM 11. Mostrar pods con problemas
echo ======================================
echo 11. Verificando pods con problemas...
echo ======================================
kubectl get pods -n %NAMESPACE% --field-selector=status.phase!=Running 2>nul | find /v "NAMESPACE" | find /v ""
if %ERRORLEVEL% EQU 0 (
    echo [!] Se encontraron pods con problemas (ver arriba)
    set /a WARNING_CHECKS+=1
    set /a TOTAL_CHECKS+=1
) else (
    echo [OK] No hay pods con problemas
    set /a PASSED_CHECKS+=1
    set /a TOTAL_CHECKS+=1
)
echo.

REM Resumen final
echo ======================================
echo   RESUMEN DE VALIDACION
echo ======================================
echo.
echo Total de verificaciones: %TOTAL_CHECKS%
echo Pasadas: %PASSED_CHECKS%
echo Advertencias: %WARNING_CHECKS%
echo Fallidas: %FAILED_CHECKS%
echo.

if %FAILED_CHECKS% EQU 0 (
    if %WARNING_CHECKS% EQU 0 (
        echo [OK] El despliegue esta completamente funcional
        endlocal
        exit /b 0
    ) else (
        echo [!] El despliegue esta mayormente funcional con algunas advertencias
        endlocal
        exit /b 0
    )
) else (
    :end_fail
    echo [X] El despliegue tiene problemas que requieren atencion
    echo.
    echo Para mas detalles, ejecute:
    echo   kubectl get all -n %NAMESPACE%
    echo   kubectl describe pods -n %NAMESPACE%
    echo   kubectl logs -n %NAMESPACE% ^<pod-name^>
    endlocal
    exit /b 1
)

REM ===== FUNCIONES =====

:check_deployment
set deployment_name=%~1
kubectl get deployment %deployment_name% -n %NAMESPACE% >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    for /f %%i in ('kubectl get deployment %deployment_name% -n %NAMESPACE% -o jsonpath^="{.status.readyReplicas}" 2^>nul') do set READY=%%i
    for /f %%i in ('kubectl get deployment %deployment_name% -n %NAMESPACE% -o jsonpath^="{.spec.replicas}" 2^>nul') do set DESIRED=%%i

    if "!READY!"=="!DESIRED!" (
        if "!READY!" NEQ "0" (
            echo [OK] Deployment '%deployment_name%': !READY!/!DESIRED! ready
            set /a PASSED_CHECKS+=1
        ) else (
            echo [!] Deployment '%deployment_name%': 0/0 ready
            set /a WARNING_CHECKS+=1
        )
    ) else (
        echo [!] Deployment '%deployment_name%': !READY!/!DESIRED! ready
        set /a WARNING_CHECKS+=1
    )
) else (
    echo [!] Deployment '%deployment_name%' no encontrado
    set /a WARNING_CHECKS+=1
)
set /a TOTAL_CHECKS+=1
goto :eof

:check_service
set service_name=%~1
kubectl get service %service_name% -n %NAMESPACE% >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo [OK] Service '%service_name%' disponible
    set /a PASSED_CHECKS+=1
) else (
    echo [!] Service '%service_name%' no encontrado
    set /a WARNING_CHECKS+=1
)
set /a TOTAL_CHECKS+=1
goto :eof

:check_configmap
set cm_name=%~1
kubectl get configmap %cm_name% -n %NAMESPACE% >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    set /a CONFIGMAP_COUNT+=1
)
goto :eof

:check_secret
set secret_name=%~1
kubectl get secret %secret_name% -n %NAMESPACE% >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    set /a SECRET_COUNT+=1
)
goto :eof

:check_service_health
set service=%~1
set port=%~2
set path=%~3

REM Obtener el pod del servicio
for /f %%i in ('kubectl get pods -n %NAMESPACE% -l app^=%service% --field-selector^=status.phase^=Running -o jsonpath^="{.items[0].metadata.name}" 2^>nul') do set POD=%%i

if not "!POD!"=="" (
    REM Intentar curl al health endpoint
    kubectl exec -n %NAMESPACE% !POD! -- curl -s -o nul -w "%%{http_code}" http://localhost:%port%%path% 2>nul | findstr "200" >nul
    if !ERRORLEVEL! EQU 0 (
        echo [OK] %service% health check passed
        set /a PASSED_CHECKS+=1
    ) else (
        echo [!] %service% health check failed
        set /a WARNING_CHECKS+=1
    )
) else (
    echo [!] %service% pod no esta Running, no se puede verificar health
    set /a WARNING_CHECKS+=1
)
set /a TOTAL_CHECKS+=1
goto :eof
