# Cliente Node.js - Sistema de Facturación

Cliente de prueba desarrollado en Node.js para validar el funcionamiento completo del API REST del sistema de facturación.

## Tabla de Contenidos

- [Requisitos](#requisitos)
- [Instalación](#instalación)
- [Uso](#uso)
- [Docker](#docker)
- [Pruebas](#pruebas)
- [Variables de Entorno](#variables-de-entorno)
- [Scripts Disponibles](#scripts-disponibles)

## Requisitos

- **Node.js**: 16.0.0 o superior
- **npm**: 8.0.0 o superior
- **Docker**: (opcional) para ejecución containerizada

## Instalación

### Instalación Local

```bash
# Instalar dependencias
npm install

# Copiar archivo de ejemplo de variables de entorno
cp .env.example .env

# Editar .env con la URL del API Gateway
nano .env  # o tu editor preferido
```

## Uso

### Opción 1: Ejecución Local (Node.js)

```bash
# Ejecutar contra localhost (default)
npm start

# Ejecutar contra otro servidor
API_URL=http://backend-service:8080 npm start
```

### Opción 2: Ejecución con Docker

```bash
# Construir imagen
npm run docker:build
# O: docker build -t facturacion-client:latest .

# Ejecutar contra localhost (desde Docker a host)
npm run docker:run

# Ejecutar en red Docker (misma red que los servicios)
npm run docker:run:network
```

### Opción 3: Docker Compose

Si tienes un `docker-compose.yml` con todos los servicios:

```bash
docker-compose up client-nodejs
```

## Docker

### Dockerfile Multi-stage

El proyecto incluye un `Dockerfile` optimizado multi-stage:
- **Stage 1 (Builder)**: Instala dependencias
- **Stage 2 (Runtime)**: Imagen mínima con Alpine Linux y usuario no-root

### Características de Seguridad

- ✓ Usuario no-root (`nodejs:1001`)
- ✓ Imagen base Alpine (mínima)
- ✓ Health check incluido
- ✓ Capas optimizadas para cache

### Comandos Docker

```bash
# Construir imagen
docker build -t facturacion-client:latest .

# Ejecutar en localhost
docker run --rm --name facturacion-client \
  -e API_URL=http://host.docker.internal:8080 \
  facturacion-client:latest

# Ejecutar en red Docker
docker run --rm --name facturacion-client \
  --network castor-network \
  -e API_URL=http://api-gateway:8080 \
  facturacion-client:latest

# Ver logs
docker logs facturacion-client
```

## Pruebas

El cliente ejecuta automáticamente las siguientes 8 pruebas funcionales:

### Flujo de Pruebas

1. **✓ PRUEBA 1: Crear Cliente**
   - Crea un nuevo cliente con todos los campos requeridos
   - Valida que se retorne un ID válido

2. **✓ PRUEBA 2: Obtener Cliente**
   - Consulta el cliente recién creado por su ID
   - Valida que los datos coincidan

3. **✓ PRUEBA 3: Listar Todos los Clientes**
   - Obtiene la lista completa de clientes
   - Valida que el cliente creado esté en la lista

4. **✓ PRUEBA 4: Crear Factura**
   - Crea una factura con 3 items diferentes
   - Cada item tiene: descripción, cantidad, precio, impuesto y descuento
   - Valida cálculos de subtotal, impuestos, descuentos y total

5. **✓ PRUEBA 5: Obtener Factura**
   - Consulta la factura recién creada por su ID
   - Valida todos los campos y items

6. **✓ PRUEBA 6: Listar Facturas por Cliente**
   - Obtiene todas las facturas de un cliente específico
   - Valida que la factura creada esté en la lista

7. **✓ PRUEBA 7: Actualizar Cliente**
   - Modifica los datos del cliente (nombre, email, teléfono, dirección)
   - Valida que los cambios se hayan aplicado

8. **✓ PRUEBA 8: Validación de Negocio**
   - Intenta crear una factura con un cliente inexistente (ID: 99999)
   - Valida que el sistema rechace la operación correctamente

### Ejemplo de Salida

```
████████████████████████████████████████████████████████████████████████████████
  CLIENTE DE PRUEBA - SISTEMA DE FACTURACIÓN
  Castor - Prueba Técnica Backend Java
████████████████████████████████████████████████████████████████████████████████

🔗 API Base URL: http://localhost:8080
⏰ Fecha: 17/11/2025, 10:30:45

================================================================================
  PRUEBA 1: Crear Cliente
================================================================================

ℹ Enviando request POST /clientes...
✓ Cliente creado exitosamente
{
  "id": 1,
  "nombre": "Empresa de Prueba S.A.S.",
  "nit": "900123456-7",
  "email": "contacto@empresaprueba.com",
  ...
}

================================================================================
  PRUEBA 4: Crear Factura para Cliente 1
================================================================================

ℹ Enviando request POST /facturas...
✓ Factura creada exitosamente

📊 Resumen de la Factura:
   Número: FAC-2025-0001
   Subtotal: $5,916,000
   Impuestos: $1,124,040
   Descuentos: $287,800
   TOTAL: $6,752,240

...

================================================================================
  RESUMEN DE PRUEBAS
================================================================================

✓ Todas las pruebas completadas exitosamente
✓ Cliente creado con ID: 1
✓ Factura creada con ID: 1
✓ Validaciones de negocio funcionando correctamente
```

## Variables de Entorno

Configura las siguientes variables en un archivo `.env` o como variables de entorno:

| Variable | Descripción | Default | Ejemplo |
|----------|-------------|---------|---------|
| `API_URL` | URL base del API Gateway | `http://localhost:8080` | `http://api-gateway:8080` |

### Ejemplos de Configuración

**Desarrollo Local:**
```bash
API_URL=http://localhost:8080
```

**Docker Compose:**
```bash
API_URL=http://api-gateway:8080
```

**Kubernetes:**
```bash
API_URL=http://api-gateway.facturacion.svc.cluster.local:8080
```

## Scripts Disponibles

### Scripts de Ejecución

- `npm start` - Ejecuta el cliente de pruebas
- `npm test` - Alias de `npm start` (ejecuta pruebas)

### Scripts de Docker

- `npm run docker:build` - Construye la imagen Docker
- `npm run docker:run` - Ejecuta el contenedor (apunta a localhost)
- `npm run docker:run:network` - Ejecuta en red Docker

### Scripts de Utilidad

- `npm run clean` - Elimina node_modules y package-lock.json
- `npm run lint` - (Placeholder) Ejecuta linter

## Estructura del Proyecto

```
client-nodejs/
├── index.js              # Cliente de pruebas principal
├── package.json          # Dependencias y scripts
├── package-lock.json     # Lock file de dependencias
├── Dockerfile            # Multi-stage Dockerfile optimizado
├── .dockerignore         # Archivos excluidos de la imagen Docker
├── .gitignore            # Archivos excluidos de Git
├── .env.example          # Ejemplo de variables de entorno
└── README.md             # Esta documentación
```

## Dependencias

### Dependencias de Producción

- **axios** (^1.6.2): Cliente HTTP para realizar peticiones al API
- **colors** (^1.4.0): Librería para colorear la salida de consola

### ¿Por qué estas dependencias?

- **axios**: Robusto, con interceptores, manejo de errores y soporte para timeouts
- **colors**: Mejora la legibilidad de los resultados en consola

## Troubleshooting

### Error: `connect ECONNREFUSED`

**Problema**: No se puede conectar al API Gateway

**Soluciones**:
1. Verifica que el API Gateway esté ejecutándose
2. Verifica la variable `API_URL`
3. Si usas Docker, asegúrate de usar `host.docker.internal` para localhost

```bash
# Verificar que el API esté corriendo
curl http://localhost:8080/actuator/health

# O desde Docker
docker run --rm curlimages/curl http://host.docker.internal:8080/actuator/health
```

### Error: `Cannot find module`

**Problema**: Faltan dependencias

**Solución**:
```bash
rm -rf node_modules package-lock.json
npm install
```

### Timeout en Docker

**Problema**: Las pruebas toman demasiado tiempo

**Solución**: Aumenta el timeout de axios (futuro enhancement)

## Mejoras Futuras

- [ ] Agregar tests unitarios con Jest
- [ ] Agregar ESLint y Prettier
- [ ] Soporte para configuración de timeout y reintentos
- [ ] Exportar resultados a formato JSON/XML
- [ ] Agregar métricas de rendimiento
- [ ] Soporte para ejecución en CI/CD
- [ ] Agregar tests de carga con k6

## Licencia

ISC

## Autor

Castor - Sistema de Facturación
