@echo off
REM Castor API Gateway - Quick Start Script for Windows

echo ╔═══════════════════════════════════════════════════════╗
echo ║      Castor API Gateway - Quick Start Script         ║
echo ╚═══════════════════════════════════════════════════════╝
echo.

REM Check if Java is installed
where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Java is not installed. Please install Java 17 or higher.
    exit /b 1
)

echo ✓ Java found
echo.

REM Check if Maven is installed
where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Maven is not installed. Please install Maven 3.6 or higher.
    exit /b 1
)

echo ✓ Maven found
echo.

REM Build the application
echo Building the application...
call mvn clean package -DskipTests

if %ERRORLEVEL% NEQ 0 (
    echo ❌ Build failed. Please check the error messages above.
    exit /b 1
)

echo ✓ Build successful
echo.

REM Run the application
echo Starting API Gateway...
echo.
echo Gateway will be available at:
echo   - http://localhost:8080
echo   - Health: http://localhost:8080/actuator/health
echo   - Metrics: http://localhost:8080/actuator/prometheus
echo.
echo Press Ctrl+C to stop
echo.

java -jar target\api-gateway-1.0.0.jar
