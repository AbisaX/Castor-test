#!/bin/bash

# Castor API Gateway - Quick Start Script

set -e

echo "╔═══════════════════════════════════════════════════════╗"
echo "║      Castor API Gateway - Quick Start Script         ║"
echo "╚═══════════════════════════════════════════════════════╝"
echo ""

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install Java 17 or higher."
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F '.' '{print $1}')
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ Java 17 or higher is required. Current version: $JAVA_VERSION"
    exit 1
fi

echo "✓ Java version: $(java -version 2>&1 | head -n 1)"
echo ""

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed. Please install Maven 3.6 or higher."
    exit 1
fi

echo "✓ Maven version: $(mvn -version | head -n 1)"
echo ""

# Build the application
echo "Building the application..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "❌ Build failed. Please check the error messages above."
    exit 1
fi

echo "✓ Build successful"
echo ""

# Run the application
echo "Starting API Gateway..."
echo ""
echo "Gateway will be available at:"
echo "  - http://localhost:8080"
echo "  - Health: http://localhost:8080/actuator/health"
echo "  - Metrics: http://localhost:8080/actuator/prometheus"
echo ""
echo "Press Ctrl+C to stop"
echo ""

java -jar target/api-gateway-1.0.0.jar
