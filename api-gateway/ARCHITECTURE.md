# Castor API Gateway - Architecture

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                           CLIENT LAYER                              │
│                                                                     │
│  Web Browsers    Mobile Apps    IoT Devices    External APIs      │
└────────────────────────┬────────────────────────────────────────────┘
                         │
                         │ HTTP/HTTPS
                         ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      API GATEWAY (Port 8080)                        │
│                   Spring Cloud Gateway (Reactive)                   │
│                                                                     │
│  ┌───────────────────────────────────────────────────────────────┐ │
│  │                    GLOBAL FILTER CHAIN                        │ │
│  │                                                               │ │
│  │  1. LoggingFilter (Order: HIGHEST_PRECEDENCE)                │ │
│  │     - Request/Response logging                               │ │
│  │     - Latency measurement                                    │ │
│  │     - Trace ID logging                                       │ │
│  │                                                               │ │
│  │  2. RateLimitingFilter (Order: -100)                         │ │
│  │     - Token bucket algorithm                                 │ │
│  │     - Per-client IP tracking                                 │ │
│  │     - Configurable limits per route                          │ │
│  │                                                               │ │
│  │  3. MetricsFilter (Order: -50)                               │ │
│  │     - Request counting                                       │ │
│  │     - Latency tracking                                       │ │
│  │     - Status code distribution                               │ │
│  │                                                               │ │
│  │  4. TracingFilter (Order: -25)                               │ │
│  │     - Trace context creation                                 │ │
│  │     - Span propagation                                       │ │
│  │     - Trace ID headers                                       │ │
│  └───────────────────────────────────────────────────────────────┘ │
│                                                                     │
│  ┌───────────────────────────────────────────────────────────────┐ │
│  │                       ROUTE DEFINITIONS                       │ │
│  │                                                               │ │
│  │  Route 1: /api/v1/clientes/**                                │ │
│  │  - URI: http://clientes-service:8081                         │ │
│  │  - Rate Limit: 100 req/min                                   │ │
│  │  - Circuit Breaker: clientesCircuitBreaker                   │ │
│  │  - Retry: 3 attempts with backoff                            │ │
│  │                                                               │ │
│  │  Route 2: /api/v1/facturas/**                                │ │
│  │  - URI: http://facturacion-service:8082                      │ │
│  │  - Rate Limit: 50 req/min                                    │ │
│  │  - Circuit Breaker: facturacionCircuitBreaker                │ │
│  │  - Retry: 3 attempts with backoff                            │ │
│  │                                                               │ │
│  │  Route 3: /api/v1/tax-calculator/**                          │ │
│  │  - URI: http://tax-calculator-service:8083                   │ │
│  │  - Rate Limit: 100 req/min                                   │ │
│  │  - Circuit Breaker: taxCalculatorCircuitBreaker              │ │
│  │  - Retry: 3 attempts with backoff                            │ │
│  └───────────────────────────────────────────────────────────────┘ │
│                                                                     │
│  ┌───────────────────────────────────────────────────────────────┐ │
│  │                  RESILIENCE PATTERNS                          │ │
│  │                                                               │ │
│  │  Circuit Breaker (Resilience4j)                              │ │
│  │  - Failure Rate: 50%                                         │ │
│  │  - Wait Duration: 60s                                        │ │
│  │  - Sliding Window: 10 requests                               │ │
│  │  - States: CLOSED → OPEN → HALF_OPEN                         │ │
│  │                                                               │ │
│  │  Fallback Controller                                         │ │
│  │  - /fallback/clientes                                        │ │
│  │  - /fallback/facturacion                                     │ │
│  │  - /fallback/tax-calculator                                  │ │
│  └───────────────────────────────────────────────────────────────┘ │
│                                                                     │
│  ┌───────────────────────────────────────────────────────────────┐ │
│  │                   EXCEPTION HANDLING                          │ │
│  │                                                               │ │
│  │  GatewayExceptionHandler                                     │ │
│  │  - NotFoundException → 404                                   │ │
│  │  - ConnectException → 503                                    │ │
│  │  - TimeoutException → 504                                    │ │
│  │  - ResponseStatusException → Custom                          │ │
│  │  - Others → 500                                              │ │
│  └───────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
                         │
                         │ Routed Requests
                         ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      DOWNSTREAM SERVICES                            │
│                                                                     │
│  ┌──────────────────┐  ┌──────────────────┐  ┌─────────────────┐  │
│  │ Clientes Service │  │Facturacion Service│  │ Tax Calculator  │  │
│  │    Port 8081     │  │    Port 8082      │  │   Port 8083     │  │
│  │                  │  │                   │  │                 │  │
│  │ - GET /clientes  │  │ - GET /facturas   │  │ - POST /calc    │  │
│  │ - POST /clientes │  │ - POST /facturas  │  │ - GET /rates    │  │
│  │ - PUT /clientes  │  │ - PUT /facturas   │  │                 │  │
│  │ - DELETE /...    │  │ - DELETE /...     │  │                 │  │
│  └──────────────────┘  └──────────────────┘  └─────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
                         │
                         │ Telemetry Data
                         ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    OBSERVABILITY STACK                              │
│                                                                     │
│  ┌──────────────────┐  ┌──────────────────┐  ┌─────────────────┐  │
│  │     Zipkin       │  │   Prometheus     │  │     Grafana     │  │
│  │   Port 9411      │  │   Port 9090      │  │   Port 3000     │  │
│  │                  │  │                   │  │                 │  │
│  │ Distributed      │  │ Metrics          │  │ Visualization   │  │
│  │ Tracing          │  │ Collection       │  │ & Dashboards    │  │
│  │                  │  │                   │  │                 │  │
│  │ - Trace views    │  │ - Time series    │  │ - Custom charts │  │
│  │ - Service graph  │  │ - Alerting       │  │ - Alert mgmt    │  │
│  │ - Latency stats  │  │ - Query PromQL   │  │ - Multi-source  │  │
│  └──────────────────┘  └──────────────────┘  └─────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

## Component Diagram

```
┌────────────────────────────────────────────────────────────────┐
│                    API GATEWAY COMPONENTS                      │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │              Application Layer                           │ │
│  │                                                          │ │
│  │  ApiGatewayApplication                                   │ │
│  │  - Main entry point                                      │ │
│  │  - Spring Boot configuration                             │ │
│  │  - Startup banner                                        │ │
│  └──────────────────────────────────────────────────────────┘ │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │              Configuration Layer                         │ │
│  │                                                          │ │
│  │  GatewayConfig                 GatewayProperties         │ │
│  │  - Route definitions           - Externalized config     │ │
│  │  - Predicates                  - Rate limits             │ │
│  │  - Filters                     - Timeouts                │ │
│  │                                - Service URLs            │ │
│  │  CorsConfig                                              │ │
│  │  - CORS settings                                         │ │
│  │  - Allowed origins                                       │ │
│  │  - Exposed headers                                       │ │
│  └──────────────────────────────────────────────────────────┘ │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │                Filter Layer                              │ │
│  │                                                          │ │
│  │  LoggingFilter           RateLimitingFilter              │ │
│  │  - Request logging       - Token bucket                  │ │
│  │  - Response logging      - Per-client limits             │ │
│  │  - Latency tracking      - Header injection              │ │
│  │                                                          │ │
│  │  MetricsFilter           TracingFilter                   │ │
│  │  - Counters              - Trace context                 │ │
│  │  - Timers                - Span creation                 │ │
│  │  - Gauges                - ID propagation                │ │
│  └──────────────────────────────────────────────────────────┘ │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │             Controller Layer                             │ │
│  │                                                          │ │
│  │  FallbackController                                      │ │
│  │  - Circuit breaker fallbacks                             │ │
│  │  - Graceful degradation                                  │ │
│  │  - Error responses                                       │ │
│  └──────────────────────────────────────────────────────────┘ │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │              Exception Layer                             │ │
│  │                                                          │ │
│  │  GatewayExceptionHandler     ErrorResponse               │ │
│  │  - Global error handling     - Standardized format       │ │
│  │  - Error categorization      - Trace ID inclusion        │ │
│  │  - Status code mapping       - Timestamp                 │ │
│  └──────────────────────────────────────────────────────────┘ │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │               Health Layer                               │ │
│  │                                                          │ │
│  │  DownstreamServiceHealthIndicator                        │ │
│  │  - Service health checks                                 │ │
│  │  - Response time tracking                                │ │
│  │  - Overall status aggregation                            │ │
│  └──────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────┘
```

## Request Flow Diagram

```
┌──────────┐
│  Client  │
└────┬─────┘
     │
     │ 1. HTTP Request
     ▼
┌─────────────────────────────────────────────────┐
│         LoggingFilter (Order: -2147483648)      │
│         Log request details + start timer       │
└────────────────────┬────────────────────────────┘
                     │
                     │ 2. Apply rate limiting
                     ▼
┌─────────────────────────────────────────────────┐
│         RateLimitingFilter (Order: -100)        │
│         Check token bucket, add headers         │
└────────────────────┬────────────────────────────┘
                     │
                     │ 3. Record metrics
                     ▼
┌─────────────────────────────────────────────────┐
│           MetricsFilter (Order: -50)            │
│         Increment counters, start timer         │
└────────────────────┬────────────────────────────┘
                     │
                     │ 4. Create trace
                     ▼
┌─────────────────────────────────────────────────┐
│           TracingFilter (Order: -25)            │
│         Create span, add trace headers          │
└────────────────────┬────────────────────────────┘
                     │
                     │ 5. Route matching
                     ▼
┌─────────────────────────────────────────────────┐
│              Route Locator                      │
│         Match path to route definition          │
└────────────────────┬────────────────────────────┘
                     │
                     │ 6. Circuit breaker check
                     ▼
┌─────────────────────────────────────────────────┐
│          Resilience4j Circuit Breaker           │
│         Check state (CLOSED/OPEN/HALF_OPEN)     │
└────┬──────────────────────────────────────┬─────┘
     │                                      │
     │ Circuit CLOSED                       │ Circuit OPEN
     │                                      │
     │ 7. Forward request                   │ 8. Fallback
     ▼                                      ▼
┌─────────────────┐              ┌─────────────────┐
│ Downstream      │              │ Fallback        │
│ Service         │              │ Controller      │
│                 │              │                 │
│ - Process       │              │ - Return 503    │
│ - Return        │              │ - Error message │
└────┬────────────┘              └────┬────────────┘
     │                                │
     │ 9. Response                    │
     │                                │
     └────────────┬───────────────────┘
                  │
                  │ 10. Apply filters (reverse order)
                  ▼
┌─────────────────────────────────────────────────┐
│         Response Processing Chain               │
│         - Record metrics                        │
│         - Log response                          │
│         - Add headers                           │
│         - End span                              │
└────────────────────┬────────────────────────────┘
                     │
                     │ 11. Return to client
                     ▼
┌─────────────────────────────────────────────────┐
│                  Client                         │
│         Receives response with headers:         │
│         - X-Gateway-Response                    │
│         - X-RateLimit-Remaining                 │
│         - X-RateLimit-Reset                     │
│         - X-Trace-Id                            │
│         - X-Span-Id                             │
└─────────────────────────────────────────────────┘
```

## Data Flow - Rate Limiting

```
Request arrives
      │
      ▼
┌──────────────────────────────────────┐
│  Extract client identifier           │
│  (Route ID + Client IP)               │
└──────────────┬───────────────────────┘
               │
               ▼
┌──────────────────────────────────────┐
│  Get or create token bucket          │
│  for this client+route                │
└──────────────┬───────────────────────┘
               │
               ▼
┌──────────────────────────────────────┐
│  Refill tokens if period elapsed     │
│  (60 seconds)                         │
└──────────────┬───────────────────────┘
               │
               ▼
        ┌──────────┐
        │ Tokens   │
        │ > 0?     │
        └─┬────┬───┘
    Yes   │    │   No
          │    │
          ▼    ▼
     ┌─────┐ ┌──────────────────────┐
     │Allow│ │ Return 429           │
     │     │ │ Too Many Requests    │
     └──┬──┘ │ + Headers:           │
        │    │ - X-RateLimit-       │
        │    │   Remaining: 0       │
        │    │ - X-RateLimit-Reset  │
        │    └──────────────────────┘
        │
        ▼
┌──────────────────────────────────────┐
│  Consume 1 token                     │
│  Add headers:                        │
│  - X-RateLimit-Remaining             │
│  - X-RateLimit-Reset                 │
│  - X-RateLimit-Limit                 │
└──────────────┬───────────────────────┘
               │
               ▼
          Continue to
          next filter
```

## Circuit Breaker State Machine

```
                    ┌─────────────┐
         Start ────→│   CLOSED    │
                    │             │
                    │ Success: OK │
                    │ Failure:    │
                    │ tracked     │
                    └──────┬──────┘
                           │
                           │ Failure rate > 50%
                           │ (min 5 calls)
                           ▼
                    ┌─────────────┐
                    │    OPEN     │
                    │             │
                    │ All calls   │
                    │ rejected    │
                    │             │
                    │ Fallback    │
                    │ triggered   │
                    └──────┬──────┘
                           │
                           │ After 60 seconds
                           ▼
                    ┌─────────────┐
                    │ HALF_OPEN   │
                    │             │
                    │ Allow 3     │
                    │ test calls  │
                    └─┬─────────┬─┘
                      │         │
         Success rate │         │ Failures
         > 50%        │         │ continue
                      │         │
                      ▼         ▼
              ┌─────────┐  ┌─────────┐
              │ CLOSED  │  │  OPEN   │
              └─────────┘  └─────────┘
```

## Deployment Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Kubernetes Cluster                       │
│                                                             │
│  ┌────────────────────────────────────────────────────────┐ │
│  │                    Namespace: castor                   │ │
│  │                                                        │ │
│  │  ┌─────────────────────────────────────────────────┐  │ │
│  │  │          Service: api-gateway-service           │  │ │
│  │  │          Type: LoadBalancer                     │  │ │
│  │  │          Port: 8080                             │  │ │
│  │  └───────────────────┬─────────────────────────────┘  │ │
│  │                      │                                │ │
│  │                      ▼                                │ │
│  │  ┌─────────────────────────────────────────────────┐  │ │
│  │  │      Deployment: api-gateway                    │  │ │
│  │  │      Replicas: 3                                │  │ │
│  │  │                                                 │  │ │
│  │  │  ┌────────────┐ ┌────────────┐ ┌────────────┐  │  │ │
│  │  │  │   Pod 1    │ │   Pod 2    │ │   Pod 3    │  │  │ │
│  │  │  │  Gateway   │ │  Gateway   │ │  Gateway   │  │  │ │
│  │  │  │  :8080     │ │  :8080     │ │  :8080     │  │  │ │
│  │  │  └────────────┘ └────────────┘ └────────────┘  │  │ │
│  │  │                                                 │  │ │
│  │  │  Resources:                                     │  │ │
│  │  │  - CPU: 500m - 1000m                            │  │ │
│  │  │  - Memory: 512Mi - 1Gi                          │  │ │
│  │  │                                                 │  │ │
│  │  │  Environment:                                   │  │ │
│  │  │  - SPRING_PROFILES_ACTIVE=prod                  │  │ │
│  │  │  - ConfigMap: gateway-config                    │  │ │
│  │  │  - Secret: gateway-secrets                      │  │ │
│  │  └─────────────────────────────────────────────────┘  │ │
│  │                                                        │ │
│  │  ┌─────────────────────────────────────────────────┐  │ │
│  │  │      HorizontalPodAutoscaler                    │  │ │
│  │  │      Min: 3, Max: 10                            │  │ │
│  │  │      Target CPU: 70%                            │  │ │
│  │  └─────────────────────────────────────────────────┘  │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## Technology Stack Layers

```
┌─────────────────────────────────────────────────────────┐
│                   Presentation Layer                    │
│                 Spring Cloud Gateway                    │
│                    (Port 8080)                          │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                   Filter Layer                          │
│  - Logging    - Rate Limiting                           │
│  - Metrics    - Tracing                                 │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                  Routing Layer                          │
│  - Route Matching                                       │
│  - Load Balancing                                       │
│  - Service Discovery                                    │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                Resilience Layer                         │
│  - Circuit Breaker (Resilience4j)                       │
│  - Retry Logic                                          │
│  - Fallback Handling                                    │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│               Observability Layer                       │
│  - Metrics (Prometheus)                                 │
│  - Tracing (Zipkin)                                     │
│  - Health Checks                                        │
│  - Logging (SLF4J)                                      │
└─────────────────────────────────────────────────────────┘
```
