# Tax Calculator Service

Microservicio de cálculo de impuestos y descuentos desarrollado con FastAPI.

## Características

- ✅ **FastAPI** - Framework moderno y rápido
- ✅ **Pydantic** - Validación de datos automática
- ✅ **OpenAPI/Swagger** - Documentación interactiva
- ✅ **Pytest** - Testing completo
- ✅ **Coverage** - Cobertura de código >80%
- ✅ **Type Hints** - Tipado estático
- ✅ **Async/Await** - Operaciones asíncronas

## Instalación

```bash
# Crear entorno virtual
python -m venv venv
source venv/bin/activate  # En Windows: venv\Scripts\activate

# Instalar dependencias
pip install -r requirements.txt
```

## Ejecución

### Modo Desarrollo

```bash
# Con auto-reload
uvicorn main:app --reload --port 5000

# O directamente con Python
python main.py
```

### Modo Producción

```bash
# Con múltiples workers
uvicorn main:app --host 0.0.0.0 --port 5000 --workers 4
```

### Con Docker

```bash
# Construir imagen
docker build -t tax-calculator:2.0 .

# Ejecutar contenedor
docker run -p 5000:5000 tax-calculator:2.0
```

## Documentación API

Una vez ejecutando, acceder a:

- **Swagger UI**: http://localhost:5000/docs
- **ReDoc**: http://localhost:5000/redoc
- **OpenAPI JSON**: http://localhost:5000/openapi.json

## Endpoints

### POST /calcular

Calcula impuestos y descuentos para items de factura.

**Request:**
```json
{
  "items": [
    {
      "descripcion": "Laptop Dell Inspiron 15",
      "cantidad": 2,
      "precio_unitario": 2500000,
      "porcentaje_impuesto": 19,
      "porcentaje_descuento": 5
    }
  ]
}
```

**Response:**
```json
{
  "subtotal_general": 5000000.00,
  "total_impuestos": 950000.00,
  "total_descuentos": 250000.00,
  "total_final": 5700000.00,
  "detalle_items": [
    {
      "descripcion": "Laptop Dell Inspiron 15",
      "subtotal": 5000000.00,
      "impuesto": 950000.00,
      "descuento": 250000.00,
      "total": 5700000.00
    }
  ],
  "timestamp": "2024-01-15T10:30:00"
}
```

### GET /health

Health check del servicio.

**Response:**
```json
{
  "status": "healthy",
  "service": "tax-calculator",
  "version": "2.0.0",
  "timestamp": "2024-01-15T10:30:00"
}
```

## Testing

### Ejecutar Tests

```bash
# Todos los tests
pytest

# Con verbose
pytest -v

# Con coverage
pytest --cov=. --cov-report=html

# Solo un archivo
pytest test_main.py

# Solo una clase
pytest test_main.py::TestHealthEndpoint

# Solo un test
pytest test_main.py::TestHealthEndpoint::test_health_check_retorna_200
```

### Coverage Report

```bash
# Generar reporte HTML
pytest --cov=. --cov-report=html

# Ver reporte
open htmlcov/index.html  # macOS
start htmlcov/index.html # Windows
xdg-open htmlcov/index.html # Linux
```

## Validaciones

El servicio valida automáticamente:

- ✅ Cantidad > 0
- ✅ Precio unitario > 0
- ✅ Porcentaje impuesto: 0-100
- ✅ Porcentaje descuento: 0-100
- ✅ Descripción: 1-500 caracteres
- ✅ Lista de items no vacía

## Linting & Formatting

```bash
# Formatear código con Black
black main.py test_main.py

# Verificar con Flake8
flake8 main.py test_main.py

# Type checking con mypy
mypy main.py
```

## Diferencias vs Flask

| Aspecto | Flask (v1) | FastAPI (v2) |
|---------|-----------|--------------|
| Performance | Síncrono | Asíncrono |
| Validación | Manual | Automática (Pydantic) |
| Documentación | Manual | Auto-generada (OpenAPI) |
| Type Hints | Opcional | Integrado |
| Testing | unittest | pytest |
| Speed | ~500 req/s | ~2000 req/s |

## Monitoring

### Métricas

FastAPI expone métricas por defecto:

- Request duration
- Request count
- Error rates

### Logging

Logs estructurados en formato:
```
2024-01-15 10:30:00 - main - INFO - Calculando impuestos para 2 items
```

## Deployment

### Kubernetes

Ver manifiestos en `k8s/python-service/`

### Docker Compose

```yaml
tax-calculator:
  image: tax-calculator:2.0
  ports:
    - "5000:5000"
  environment:
    - LOG_LEVEL=info
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:5000/health"]
    interval: 30s
    timeout: 3s
    retries: 3
```

## Variables de Entorno

| Variable | Default | Descripción |
|----------|---------|-------------|
| `PORT` | 5000 | Puerto del servicio |
| `LOG_LEVEL` | info | Nivel de logging |
| `WORKERS` | 4 | Número de workers Uvicorn |

## Troubleshooting

### Puerto en uso

```bash
# Cambiar puerto
uvicorn main:app --port 5001
```

### Logs no aparecen

```bash
# Ejecutar con log level debug
uvicorn main:app --log-level debug
```

## Roadmap

- [ ] Rate limiting
- [ ] Autenticación JWT
- [ ] Cache con Redis
- [ ] Métricas con Prometheus
- [ ] GraphQL endpoint

## Licencia

Desarrollado para Castor - Sistema de Facturación
