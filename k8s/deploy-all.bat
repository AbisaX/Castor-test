@echo off
REM Script de despliegue completo para Kubernetes - Windows
REM Sistema de Facturación - Proyecto Castor

setlocal enabledelayedexpansion

echo ======================================
echo   DESPLIEGUE COMPLETO
echo   Sistema de Facturacion
echo ======================================
echo.

REM Verificar que kubectl existe
where kubectl >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] kubectl no esta instalado o no esta en el PATH
    exit /b 1
)
echo [OK] kubectl encontrado

REM Verificar conexión al cluster
kubectl cluster-info >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] No se puede conectar al cluster de Kubernetes
    echo Verifica tu configuracion de kubectl
    exit /b 1
)
echo [OK] Conectado al cluster
echo.

REM 1. Crear Namespace
echo ======================================
echo  1. CREANDO NAMESPACE
echo ======================================
kubectl apply -f namespace.yaml
echo [OK] Namespace facturacion creado/actualizado
echo.

REM 2. Desplegar PostgreSQL
echo ======================================
echo  2. DESPLEGANDO POSTGRESQL
echo ======================================
kubectl apply -f postgres\postgres-configmap.yaml
kubectl apply -f postgres\postgres-init-script-configmap.yaml
kubectl apply -f postgres\postgres-secret.yaml
kubectl apply -f postgres\postgres-pvc.yaml
kubectl apply -f postgres\postgres-deployment.yaml
kubectl apply -f postgres\postgres-service.yaml
echo [OK] PostgreSQL desplegado
echo Esperando 60 segundos para que PostgreSQL inicie...
timeout /t 60 /nobreak >nul
echo.

REM 3. Desplegar Oracle
echo ======================================
echo  3. DESPLEGANDO ORACLE
echo ======================================
kubectl apply -f oracle\oracle-configmap.yaml
kubectl apply -f oracle\oracle-init-script-configmap.yaml
kubectl apply -f oracle\oracle-secret.yaml
kubectl apply -f oracle\oracle-pvc.yaml
kubectl apply -f oracle\oracle-deployment.yaml
kubectl apply -f oracle\oracle-service.yaml
echo [OK] Oracle desplegado
echo [INFO] Oracle puede tomar 5-10 minutos en estar listo...
echo Esperando 120 segundos...
timeout /t 120 /nobreak >nul
echo.

REM 4. Desplegar Zipkin
echo ======================================
echo  4. DESPLEGANDO ZIPKIN
echo ======================================
kubectl apply -f zipkin\zipkin-deployment.yaml
kubectl apply -f zipkin\zipkin-service.yaml
echo [OK] Zipkin desplegado
timeout /t 30 /nobreak >nul
echo.

REM 5. Desplegar Kafka y CDC
echo ======================================
echo  5. DESPLEGANDO KAFKA Y CDC
echo ======================================
echo Desplegando Zookeeper...
kubectl apply -f kafka\zookeeper-deployment.yaml
kubectl apply -f kafka\zookeeper-service.yaml
timeout /t 30 /nobreak >nul

echo Desplegando Kafka...
kubectl apply -f kafka\kafka-deployment.yaml
kubectl apply -f kafka\kafka-service.yaml
timeout /t 45 /nobreak >nul

echo Desplegando Debezium Connect...
kubectl apply -f kafka\debezium-deployment.yaml
kubectl apply -f kafka\debezium-service.yaml
echo [OK] Kafka y Debezium desplegados
timeout /t 45 /nobreak >nul
echo.

REM 6. Desplegar Tax Calculator Service
echo ======================================
echo  6. DESPLEGANDO TAX CALCULATOR
echo ======================================
kubectl apply -f python-service\python-deployment.yaml
kubectl apply -f python-service\python-service.yaml
echo [OK] Tax Calculator desplegado
timeout /t 20 /nobreak >nul
echo.

REM 7. Desplegar Microservicios
echo ======================================
echo  7. DESPLEGANDO MICROSERVICIOS
echo ======================================
echo Desplegando Clientes Service...
kubectl apply -f clientes-service\clientes-configmap.yaml
kubectl apply -f clientes-service\clientes-secret.yaml
kubectl apply -f clientes-service\clientes-deployment.yaml
kubectl apply -f clientes-service\clientes-service.yaml
timeout /t 45 /nobreak >nul
echo [OK] Clientes Service desplegado

echo Desplegando Facturacion Service...
kubectl apply -f facturacion-service\facturacion-configmap.yaml
kubectl apply -f facturacion-service\facturacion-secret.yaml
kubectl apply -f facturacion-service\facturacion-deployment.yaml
kubectl apply -f facturacion-service\facturacion-service.yaml
timeout /t 60 /nobreak >nul
echo [OK] Facturacion Service desplegado
echo.

REM 8. Desplegar API Gateway
echo ======================================
echo  8. DESPLEGANDO API GATEWAY
echo ======================================
kubectl apply -f api-gateway\api-gateway-configmap.yaml
kubectl apply -f api-gateway\api-gateway-deployment.yaml
kubectl apply -f api-gateway\api-gateway-service.yaml
echo [OK] API Gateway desplegado
timeout /t 45 /nobreak >nul
echo.

REM 9. Mostrar resumen
echo ======================================
echo   DESPLIEGUE COMPLETADO
echo ======================================
echo.
echo Todos los componentes han sido desplegados exitosamente
echo.

echo Recursos desplegados:
kubectl get all -n facturacion
echo.

echo ======================================
echo   COMANDOS UTILES
echo ======================================
echo.
echo Para acceder al API Gateway:
echo   kubectl get svc api-gateway -n facturacion
echo.
echo Para ver logs de un servicio:
echo   kubectl logs -f deployment/clientes-service -n facturacion
echo.
echo Para port-forward al API Gateway:
echo   kubectl port-forward svc/api-gateway 8080:8080 -n facturacion
echo.
echo Para ver todos los pods:
echo   kubectl get pods -n facturacion
echo.
echo Para configurar Debezium CDC:
echo   cd ..\debezium
echo   kubectl port-forward svc/debezium-connect 8083:8083 -n facturacion
echo   register-connectors.bat
echo.

endlocal
