# API Gateway Routes Documentation

This document describes all available routes through the Castor API Gateway.

## Base URL
- **Local Development**: `http://localhost:8080`
- **Production**: `https://api.castor.com` (configure as needed)

## Route Table

| Service | Route Pattern | Upstream URL | Rate Limit | Circuit Breaker |
|---------|--------------|--------------|------------|-----------------|
| Clientes | `/api/v1/clientes/**` | `http://clientes-service:8081` | 100 req/min | Yes |
| Facturacion | `/api/v1/facturas/**` | `http://facturacion-service:8082` | 50 req/min | Yes |
| Tax Calculator | `/api/v1/tax-calculator/**` | `http://tax-calculator-service:8083` | 100 req/min | Yes |

## Headers

### Request Headers (Added by Gateway)
- `X-Gateway-Request`: `API-Gateway` - Identifies requests from the gateway
- `X-Trace-Id`: Distributed tracing ID
- `X-Span-Id`: Current span ID

### Response Headers (Added by Gateway)
- `X-Gateway-Response`: `API-Gateway` - Identifies responses from the gateway
- `X-RateLimit-Remaining`: Number of requests remaining
- `X-RateLimit-Reset`: Unix timestamp when limit resets
- `X-RateLimit-Limit`: Total rate limit for the route
- `X-Trace-Id`: Trace ID for correlation
- `X-Span-Id`: Span ID for correlation

## Route Details

### 1. Clientes Service Routes

#### Base Path: `/api/v1/clientes`

**Upstream**: `http://clientes-service:8081`

**Example Endpoints**:
```
GET    /api/v1/clientes           - List all clientes
GET    /api/v1/clientes/{id}      - Get cliente by ID
POST   /api/v1/clientes           - Create new cliente
PUT    /api/v1/clientes/{id}      - Update cliente
DELETE /api/v1/clientes/{id}      - Delete cliente
```

**Rate Limit**: 100 requests/minute per client IP

**Circuit Breaker**: `clientesCircuitBreaker`
- Failure Rate Threshold: 50%
- Wait Duration: 60 seconds
- Sliding Window: 10 requests

**Example Request**:
```bash
curl -X GET http://localhost:8080/api/v1/clientes \
  -H "Content-Type: application/json"
```

**Example Response**:
```json
{
  "data": [...],
  "headers": {
    "X-Gateway-Response": "API-Gateway",
    "X-RateLimit-Remaining": "99",
    "X-RateLimit-Reset": "1700000000",
    "X-Trace-Id": "abc123def456"
  }
}
```

### 2. Facturacion Service Routes

#### Base Path: `/api/v1/facturas`

**Upstream**: `http://facturacion-service:8082`

**Example Endpoints**:
```
GET    /api/v1/facturas              - List all facturas
GET    /api/v1/facturas/{id}         - Get factura by ID
POST   /api/v1/facturas              - Create new factura
PUT    /api/v1/facturas/{id}         - Update factura
DELETE /api/v1/facturas/{id}         - Delete factura
GET    /api/v1/facturas/cliente/{id} - Get facturas by cliente
```

**Rate Limit**: 50 requests/minute per client IP (lower due to resource intensity)

**Circuit Breaker**: `facturacionCircuitBreaker`
- Failure Rate Threshold: 50%
- Wait Duration: 60 seconds
- Sliding Window: 10 requests

**Example Request**:
```bash
curl -X POST http://localhost:8080/api/v1/facturas \
  -H "Content-Type: application/json" \
  -d '{
    "clienteId": "123",
    "monto": 1000.00,
    "descripcion": "Servicios de consultoría"
  }'
```

### 3. Tax Calculator Service Routes

#### Base Path: `/api/v1/tax-calculator`

**Upstream**: `http://tax-calculator-service:8083`

**Example Endpoints**:
```
POST   /api/v1/tax-calculator/calculate      - Calculate taxes
GET    /api/v1/tax-calculator/rates          - Get tax rates
GET    /api/v1/tax-calculator/rates/{type}   - Get specific tax rate
```

**Rate Limit**: 100 requests/minute per client IP

**Circuit Breaker**: `taxCalculatorCircuitBreaker`
- Failure Rate Threshold: 50%
- Wait Duration: 60 seconds
- Sliding Window: 10 requests

**Example Request**:
```bash
curl -X POST http://localhost:8080/api/v1/tax-calculator/calculate \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 1000.00,
    "taxType": "IVA",
    "country": "CO"
  }'
```

## Error Responses

### Rate Limit Exceeded (429)
```json
{
  "error": "Rate limit exceeded",
  "message": "Too many requests. Please try again after 45 seconds.",
  "status": 429
}
```

### Service Unavailable (503) - Circuit Breaker Open
```json
{
  "status": 503,
  "error": "Service Unavailable",
  "message": "Clientes service is currently unavailable. Please try again later.",
  "path": "/fallback/clientes",
  "timestamp": "2025-11-16T10:30:45",
  "traceId": "abc123def456"
}
```

### Gateway Timeout (504)
```json
{
  "status": 504,
  "error": "Gateway Timeout",
  "message": "Request to downstream service timed out",
  "path": "/api/v1/clientes",
  "timestamp": "2025-11-16T10:30:45",
  "traceId": "abc123def456"
}
```

### Service Not Found (404)
```json
{
  "status": 404,
  "error": "Service Not Found",
  "message": "The requested service is not available",
  "path": "/api/v1/unknown",
  "timestamp": "2025-11-16T10:30:45",
  "traceId": "abc123def456"
}
```

### Internal Server Error (500)
```json
{
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred",
  "path": "/api/v1/clientes",
  "timestamp": "2025-11-16T10:30:45",
  "traceId": "abc123def456"
}
```

## Management Endpoints

### Actuator Health
```bash
GET http://localhost:8080/actuator/health
```

**Response**:
```json
{
  "status": "UP",
  "components": {
    "downstreamServiceHealthIndicator": {
      "status": "UP",
      "details": {
        "message": "All downstream services are healthy",
        "clientes-service": {"status": "UP", "responseTime": "45ms"},
        "facturacion-service": {"status": "UP", "responseTime": "32ms"},
        "tax-calculator-service": {"status": "UP", "responseTime": "28ms"}
      }
    }
  }
}
```

### Gateway Routes
```bash
GET http://localhost:8080/actuator/gateway/routes
```

### Prometheus Metrics
```bash
GET http://localhost:8080/actuator/prometheus
```

### Circuit Breakers Status
```bash
GET http://localhost:8080/actuator/circuitbreakers
```

## Rate Limiting Details

### How It Works
- Token bucket algorithm
- Per client IP address
- Per route configuration
- Automatic refresh every 60 seconds

### Testing Rate Limits
```bash
# Send multiple requests quickly
for i in {1..60}; do
  curl -s -o /dev/null -w "%{http_code}\n" \
    http://localhost:8080/api/v1/clientes
done
```

### Monitoring Rate Limits
Check response headers:
```bash
curl -I http://localhost:8080/api/v1/clientes
```

Look for:
- `X-RateLimit-Remaining: 95`
- `X-RateLimit-Reset: 1700000060`
- `X-RateLimit-Limit: 100`

## Best Practices

### 1. Handle Rate Limits
Always check `X-RateLimit-Remaining` header and implement backoff strategy:

```javascript
const response = await fetch('http://localhost:8080/api/v1/clientes');
const remaining = response.headers.get('X-RateLimit-Remaining');

if (remaining < 10) {
  console.warn('Approaching rate limit, slow down requests');
}
```

### 2. Implement Retry Logic
For 5xx errors and timeouts, implement exponential backoff:

```javascript
async function fetchWithRetry(url, maxRetries = 3) {
  for (let i = 0; i < maxRetries; i++) {
    try {
      const response = await fetch(url);
      if (response.ok) return response;

      if (response.status >= 500) {
        await sleep(Math.pow(2, i) * 1000); // Exponential backoff
        continue;
      }

      return response;
    } catch (error) {
      if (i === maxRetries - 1) throw error;
      await sleep(Math.pow(2, i) * 1000);
    }
  }
}
```

### 3. Use Trace IDs
Include trace IDs in error reports for debugging:

```javascript
const traceId = response.headers.get('X-Trace-Id');
console.error(`Request failed. Trace ID: ${traceId}`);
```

### 4. Monitor Health
Periodically check gateway health before making requests:

```javascript
const health = await fetch('http://localhost:8080/actuator/health');
const status = await health.json();

if (status.status !== 'UP') {
  console.warn('Gateway is not healthy');
}
```

## Performance Tips

1. **Connection Pooling**: The gateway uses connection pooling. Reuse client connections.
2. **Parallel Requests**: Make independent requests in parallel when possible.
3. **Timeout Handling**: Set appropriate client-side timeouts (recommend 30s).
4. **Compression**: Enable gzip compression on client side.
5. **Caching**: Implement client-side caching for GET requests where appropriate.

## Security Notes

1. **No Authentication**: This gateway does not implement authentication/authorization.
2. **CORS**: Permissive CORS is enabled for development. Restrict in production.
3. **Rate Limiting**: Provides basic DDoS protection.
4. **Headers**: Sensitive headers are not logged in production.

## Troubleshooting

### Issue: Constant 503 Errors
**Solution**: Check circuit breaker status at `/actuator/circuitbreakers`. Wait for circuit to close or restart services.

### Issue: 429 Too Many Requests
**Solution**: Implement request throttling on client side. Check `X-RateLimit-Reset` header.

### Issue: Slow Response Times
**Solution**:
1. Check `/actuator/metrics` for latency percentiles
2. Review Zipkin traces at `http://localhost:9411`
3. Check downstream service health

### Issue: Timeouts
**Solution**:
1. Increase `GATEWAY_RESPONSE_TIMEOUT` if needed
2. Optimize downstream services
3. Check network connectivity

## Versioning

Current API Version: **v1**

Future versions will be accessible via:
- `/api/v2/...`
- `/api/v3/...`

The gateway supports multiple API versions simultaneously.
