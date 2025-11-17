# Castor API Gateway

API Gateway for the Castor Microservices Platform built with Spring Cloud Gateway. This gateway provides a unified entry point for all microservices with advanced features including rate limiting, circuit breakers, distributed tracing, and comprehensive monitoring.

## Features

### Core Features
- **Spring Cloud Gateway**: Reactive and non-blocking API gateway
- **Dynamic Routing**: Intelligent request routing to downstream services
- **Load Balancing**: Built-in load balancing capabilities
- **CORS Support**: Permissive CORS configuration for development

### Resilience & Reliability
- **Rate Limiting**: Token bucket algorithm with configurable limits per route
  - General routes: 100 requests/minute
  - Facturacion routes: 50 requests/minute
- **Circuit Breaker**: Resilience4j circuit breaker pattern for fault tolerance
- **Retry Mechanism**: Automatic retry for failed requests with exponential backoff
- **Fallback Responses**: Graceful degradation when services are unavailable

### Monitoring & Observability
- **Distributed Tracing**: Micrometer Tracing with Brave integration
- **Zipkin Integration**: Send traces to Zipkin for visualization
- **Prometheus Metrics**: Expose custom metrics for Prometheus
- **Health Checks**: Monitor downstream service health
- **Request/Response Logging**: Detailed logging with trace IDs

### Custom Metrics
- `gateway.requests.total`: Total number of requests by route, method, and service
- `gateway.requests.duration`: Request duration by route, method, status, and service
- `gateway.requests.status`: Request status distribution by route and category

## Architecture

```
┌──────────────┐
│   Client     │
└──────┬───────┘
       │
       v
┌──────────────────────────────────────────────────┐
│           API Gateway (Port 8080)                │
│                                                   │
│  ┌─────────────────────────────────────────┐   │
│  │  Global Filters                         │   │
│  │  - Logging Filter                       │   │
│  │  - Rate Limiting Filter                 │   │
│  │  - Metrics Filter                       │   │
│  │  - Tracing Filter                       │   │
│  └─────────────────────────────────────────┘   │
│                                                   │
│  ┌─────────────────────────────────────────┐   │
│  │  Routes                                  │   │
│  │  - /api/v1/clientes/**                  │   │
│  │  - /api/v1/facturas/**                  │   │
│  │  - /api/v1/tax-calculator/**            │   │
│  └─────────────────────────────────────────┘   │
└──────────────────────────────────────────────────┘
       │            │            │
       v            v            v
┌──────────┐ ┌──────────┐ ┌──────────┐
│ Clientes │ │Facturacion│ │   Tax    │
│ Service  │ │ Service   │ │Calculator│
│  :8081   │ │  :8082    │ │  :8083   │
└──────────┘ └──────────┘ └──────────┘
```

## Routes

### Clientes Service
- **Path**: `/api/v1/clientes/**`
- **Upstream**: `http://clientes-service:8081`
- **Rate Limit**: 100 requests/minute
- **Features**: Circuit breaker, retry, request/response headers

### Facturacion Service
- **Path**: `/api/v1/facturas/**`
- **Upstream**: `http://facturacion-service:8082`
- **Rate Limit**: 50 requests/minute (lower limit due to resource intensity)
- **Features**: Circuit breaker, retry, request/response headers

### Tax Calculator Service
- **Path**: `/api/v1/tax-calculator/**`
- **Upstream**: `http://tax-calculator-service:8083`
- **Rate Limit**: 100 requests/minute
- **Features**: Circuit breaker, retry, request/response headers

## Rate Limiting

Rate limiting is implemented using an in-memory token bucket algorithm.

### Configuration
- **Default Limit**: 100 requests/minute
- **Facturacion Limit**: 50 requests/minute
- **Refresh Period**: 60 seconds

### Response Headers
- `X-RateLimit-Remaining`: Number of requests remaining in current window
- `X-RateLimit-Reset`: Unix timestamp when the limit resets
- `X-RateLimit-Limit`: Total limit for the route

### Rate Limit Exceeded Response
```json
{
  "error": "Rate limit exceeded",
  "message": "Too many requests. Please try again after X seconds.",
  "status": 429
}
```

## Circuit Breaker

Circuit breakers prevent cascading failures and provide fallback responses.

### Configuration
- **Failure Rate Threshold**: 50%
- **Sliding Window Size**: 10 requests
- **Minimum Number of Calls**: 5
- **Wait Duration in Open State**: 60 seconds
- **Half-Open State Calls**: 3

### Fallback Endpoints
- `/fallback/clientes`: Clientes service fallback
- `/fallback/facturacion`: Facturacion service fallback
- `/fallback/tax-calculator`: Tax Calculator service fallback

## Distributed Tracing

All requests are traced with unique trace IDs propagated to downstream services.

### Trace Headers
- `X-Trace-Id`: Unique identifier for the entire request trace
- `X-Span-Id`: Unique identifier for the current span

### Zipkin
View traces at: `http://localhost:9411`

## Metrics & Monitoring

### Actuator Endpoints
- **Health**: `http://localhost:8080/actuator/health`
- **Metrics**: `http://localhost:8080/actuator/metrics`
- **Prometheus**: `http://localhost:8080/actuator/prometheus`
- **Gateway Routes**: `http://localhost:8080/actuator/gateway/routes`
- **Circuit Breakers**: `http://localhost:8080/actuator/circuitbreakers`

### Prometheus Metrics
The gateway exposes custom metrics that can be scraped by Prometheus:
```yaml
# Custom Gateway Metrics
gateway_requests_total{route="clientes-service",method="GET",service="clientes-service"}
gateway_requests_duration_seconds{route="clientes-service",method="GET",service="clientes-service",status="200"}
gateway_requests_status{route="clientes-service",method="GET",service="clientes-service",status="200",status_category="2xx_success"}
```

### Health Check Response
```json
{
  "status": "UP",
  "components": {
    "diskSpace": {...},
    "ping": {...},
    "downstreamServiceHealthIndicator": {
      "status": "UP",
      "details": {
        "message": "All downstream services are healthy",
        "clientes-service": {
          "status": "UP",
          "responseTime": "45ms"
        },
        "facturacion-service": {
          "status": "UP",
          "responseTime": "32ms"
        },
        "tax-calculator-service": {
          "status": "UP",
          "responseTime": "28ms"
        }
      }
    }
  }
}
```

## Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- Docker (optional, for containerized deployment)

### Running Locally

1. **Clone the repository**
```bash
cd c:\Users\57310\Documents\Castor\api-gateway
```

2. **Build the application**
```bash
mvn clean package
```

3. **Run the application**
```bash
mvn spring-boot:run
```

Or run the JAR directly:
```bash
java -jar target/api-gateway-1.0.0.jar
```

4. **Access the gateway**
- Gateway: http://localhost:8080
- Health: http://localhost:8080/actuator/health
- Metrics: http://localhost:8080/actuator/prometheus

### Running with Docker

1. **Build the Docker image**
```bash
docker build -t castor/api-gateway:1.0.0 .
```

2. **Run the container**
```bash
docker run -d \
  --name api-gateway \
  -p 8080:8080 \
  -e CLIENTES_SERVICE_URL=http://clientes-service:8081 \
  -e FACTURACION_SERVICE_URL=http://facturacion-service:8082 \
  -e TAX_CALCULATOR_SERVICE_URL=http://tax-calculator-service:8083 \
  -e ZIPKIN_ENDPOINT=http://zipkin:9411/api/v2/spans \
  castor/api-gateway:1.0.0
```

### Running with Docker Compose

Add this service to your `docker-compose.yml`:
```yaml
services:
  api-gateway:
    build: ./api-gateway
    ports:
      - "8080:8080"
    environment:
      - CLIENTES_SERVICE_URL=http://clientes-service:8081
      - FACTURACION_SERVICE_URL=http://facturacion-service:8082
      - TAX_CALCULATOR_SERVICE_URL=http://tax-calculator-service:8083
      - ZIPKIN_ENDPOINT=http://zipkin:9411/api/v2/spans
      - SPRING_PROFILES_ACTIVE=prod
    depends_on:
      - clientes-service
      - facturacion-service
      - tax-calculator-service
      - zipkin
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 3s
      retries: 3
      start_period: 40s
```

## Configuration

### Environment Variables (Production)

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | Server port | 8080 |
| `CLIENTES_SERVICE_URL` | Clientes service URL | http://clientes-service:8081 |
| `FACTURACION_SERVICE_URL` | Facturacion service URL | http://facturacion-service:8082 |
| `TAX_CALCULATOR_SERVICE_URL` | Tax Calculator service URL | http://tax-calculator-service:8083 |
| `RATE_LIMITING_ENABLED` | Enable rate limiting | true |
| `RATE_LIMIT_DEFAULT` | Default rate limit | 100 |
| `CIRCUIT_BREAKER_ENABLED` | Enable circuit breaker | true |
| `CIRCUIT_BREAKER_FAILURE_RATE` | Failure rate threshold | 50 |
| `ZIPKIN_ENDPOINT` | Zipkin endpoint | http://zipkin:9411/api/v2/spans |
| `TRACING_ENABLED` | Enable distributed tracing | true |
| `TRACING_SAMPLING_PROBABILITY` | Trace sampling rate | 0.1 (10%) |
| `LOG_LEVEL_ROOT` | Root log level | INFO |
| `LOG_LEVEL_APP` | Application log level | INFO |

### Profiles
- **default**: Development configuration with verbose logging
- **prod**: Production configuration with optimized settings

Activate production profile:
```bash
java -jar app.jar --spring.profiles.active=prod
```

## Testing

### Example Requests

**Get Clientes**
```bash
curl -X GET http://localhost:8080/api/v1/clientes \
  -H "Content-Type: application/json"
```

**Create Factura**
```bash
curl -X POST http://localhost:8080/api/v1/facturas \
  -H "Content-Type: application/json" \
  -d '{
    "clienteId": "123",
    "monto": 1000.00
  }'
```

**Calculate Tax**
```bash
curl -X POST http://localhost:8080/api/v1/tax-calculator/calculate \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 1000.00,
    "taxType": "IVA"
  }'
```

### Test Rate Limiting
```bash
# Send 150 requests to exceed the limit
for i in {1..150}; do
  curl -X GET http://localhost:8080/api/v1/clientes
  echo "Request $i"
done
```

## Logging

Logs include trace IDs for correlation with distributed traces.

### Log Format
```
2025-11-16 10:30:45 [trace-id/span-id] [http-nio-8080-exec-1] INFO  c.c.g.filter.LoggingFilter - [abc123] Incoming Request - Route: clientes-service, Method: GET, Path: /api/v1/clientes
```

### Log Levels
- **Production**: INFO
- **Development**: DEBUG for com.castor.gateway

## Performance Optimization

### JVM Options
The Dockerfile includes optimized JVM options:
- Container-aware memory management
- G1GC garbage collector
- String deduplication
- 75% max RAM usage

### Connection Pooling
- Elastic connection pool
- 15s max idle time
- 100 max connections in production

## Troubleshooting

### Common Issues

**1. Service Unavailable (503)**
- Check if downstream services are running
- Verify service URLs in configuration
- Check circuit breaker status: `http://localhost:8080/actuator/circuitbreakers`

**2. Rate Limit Exceeded (429)**
- Wait for the reset time indicated in `X-RateLimit-Reset` header
- Adjust rate limits in configuration if needed

**3. Gateway Timeout (504)**
- Increase response timeout: `GATEWAY_RESPONSE_TIMEOUT`
- Check downstream service performance

**4. Traces Not Appearing in Zipkin**
- Verify Zipkin is running: `http://localhost:9411`
- Check `ZIPKIN_ENDPOINT` configuration
- Increase sampling probability for development: `TRACING_SAMPLING_PROBABILITY=1.0`

## Contributing

1. Follow the existing code structure
2. Add appropriate logging with trace IDs
3. Update metrics for new features
4. Test rate limiting and circuit breaker behavior
5. Update documentation

## License

Copyright (c) 2025 Castor Platform

## Contact

For issues or questions, please contact the development team.

---

**Castor API Gateway** - Powering the Castor Microservices Platform
