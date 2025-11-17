"""
Tax Calculator Service - FastAPI
Microservicio para cálculo de impuestos y descuentos
"""

from fastapi import FastAPI, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.middleware.gzip import GZipMiddleware
from pydantic import BaseModel, Field, validator
from typing import List, Optional
from decimal import Decimal, ROUND_HALF_UP
import logging
from datetime import datetime
from config import settings, log_configuration

# Configuración de logging
logging.basicConfig(
    level=getattr(logging, settings.log_level),
    format=settings.log_format
)
logger = logging.getLogger(__name__)

# Configuración de FastAPI
app = FastAPI(
    title=settings.app_name,
    description="Microservicio de cálculo de impuestos y descuentos para sistema de facturación",
    version=settings.app_version,
    docs_url=settings.docs_url,
    redoc_url=settings.redoc_url,
    openapi_url=settings.openapi_url
)

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=settings.cors_allow_credentials,
    allow_methods=settings.cors_allow_methods,
    allow_headers=settings.cors_allow_headers,
)

# GZip Compression
if settings.enable_gzip:
    app.add_middleware(GZipMiddleware, minimum_size=settings.gzip_minimum_size)


# ============= Modelos Pydantic =============

class ItemCalculation(BaseModel):
    """Modelo para un item a calcular"""
    descripcion: str = Field(
        ...,
        min_length=1,
        max_length=500,
        description="Descripción del producto o servicio"
    )
    cantidad: int = Field(
        ...,
        gt=0,
        description="Cantidad del item (debe ser positiva)"
    )
    precio_unitario: Decimal = Field(
        ...,
        gt=0,
        description="Precio unitario del item"
    )
    porcentaje_impuesto: Optional[Decimal] = Field(
        default=Decimal("0"),
        ge=0,
        le=100,
        description="Porcentaje de impuesto a aplicar (0-100)"
    )
    porcentaje_descuento: Optional[Decimal] = Field(
        default=Decimal("0"),
        ge=0,
        le=100,
        description="Porcentaje de descuento a aplicar (0-100)"
    )

    @validator('precio_unitario', 'porcentaje_impuesto', 'porcentaje_descuento', pre=True)
    def round_decimal(cls, v):
        """Redondear valores decimales a 2 decimales"""
        if v is not None:
            return Decimal(str(v)).quantize(Decimal('0.01'), rounding=ROUND_HALF_UP)
        return v

    class Config:
        json_schema_extra = {
            "example": {
                "descripcion": "Laptop Dell Inspiron 15",
                "cantidad": 2,
                "precio_unitario": 2500000,
                "porcentaje_impuesto": 19,
                "porcentaje_descuento": 5
            }
        }


class CalculationRequest(BaseModel):
    """Request para cálculo de impuestos"""
    items: List[ItemCalculation] = Field(
        ...,
        min_items=1,
        description="Lista de items a procesar"
    )

    class Config:
        json_schema_extra = {
            "example": {
                "items": [
                    {
                        "descripcion": "Laptop Dell Inspiron 15",
                        "cantidad": 2,
                        "precio_unitario": 2500000,
                        "porcentaje_impuesto": 19,
                        "porcentaje_descuento": 5
                    },
                    {
                        "descripcion": "Mouse Inalámbrico",
                        "cantidad": 3,
                        "precio_unitario": 80000,
                        "porcentaje_impuesto": 19,
                        "porcentaje_descuento": 10
                    }
                ]
            }
        }


class ItemDetail(BaseModel):
    """Detalle de cálculo por item"""
    descripcion: str
    subtotal: Decimal
    impuesto: Decimal
    descuento: Decimal
    total: Decimal


class CalculationResponse(BaseModel):
    """Response con resultados del cálculo"""
    subtotal_general: Decimal = Field(description="Suma de todos los subtotales")
    total_impuestos: Decimal = Field(description="Total de impuestos calculados")
    total_descuentos: Decimal = Field(description="Total de descuentos aplicados")
    total_final: Decimal = Field(description="Total final (subtotal + impuestos - descuentos)")
    detalle_items: List[ItemDetail] = Field(description="Detalle por cada item")
    timestamp: datetime = Field(description="Fecha y hora del cálculo")

    class Config:
        json_schema_extra = {
            "example": {
                "subtotal_general": 5240000.00,
                "total_impuestos": 995600.00,
                "total_descuentos": 286000.00,
                "total_final": 5949600.00,
                "detalle_items": [],
                "timestamp": "2024-01-15T10:30:00"
            }
        }


class HealthResponse(BaseModel):
    """Response del health check"""
    status: str
    service: str
    version: str
    timestamp: datetime


# ============= Endpoints =============

@app.get(
    "/health",
    response_model=HealthResponse,
    tags=["Health"],
    summary="Health Check",
    description="Verifica el estado del servicio"
)
async def health_check():
    """
    Health check endpoint para Kubernetes y monitoreo
    """
    return HealthResponse(
        status="healthy",
        service="tax-calculator",
        version="2.0.0",
        timestamp=datetime.now()
    )


@app.post(
    "/calcular",
    response_model=CalculationResponse,
    status_code=status.HTTP_200_OK,
    tags=["Calculations"],
    summary="Calcular Impuestos y Descuentos",
    description="Calcula impuestos y descuentos para una lista de items de factura"
)
async def calcular_impuestos(request: CalculationRequest):
    """
    Calcula impuestos y descuentos para items de factura.

    - **items**: Lista de items con descripción, cantidad, precio y porcentajes
    - **Returns**: Totales calculados y detalle por item
    """
    logger.info(f"Calculando impuestos para {len(request.items)} items")

    try:
        subtotal_general = Decimal("0")
        total_impuestos = Decimal("0")
        total_descuentos = Decimal("0")
        detalle_items = []

        for idx, item in enumerate(request.items):
            # Cálculo del subtotal del item
            subtotal_item = (
                Decimal(str(item.cantidad)) * item.precio_unitario
            ).quantize(Decimal('0.01'), rounding=ROUND_HALF_UP)

            # Cálculo de impuesto
            impuesto_item = (
                subtotal_item * (item.porcentaje_impuesto / Decimal("100"))
            ).quantize(Decimal('0.01'), rounding=ROUND_HALF_UP)

            # Cálculo de descuento
            descuento_item = (
                subtotal_item * (item.porcentaje_descuento / Decimal("100"))
            ).quantize(Decimal('0.01'), rounding=ROUND_HALF_UP)

            # Total del item
            total_item = (
                subtotal_item + impuesto_item - descuento_item
            ).quantize(Decimal('0.01'), rounding=ROUND_HALF_UP)

            # Acumular totales
            subtotal_general += subtotal_item
            total_impuestos += impuesto_item
            total_descuentos += descuento_item

            # Agregar detalle
            detalle_items.append(ItemDetail(
                descripcion=item.descripcion,
                subtotal=subtotal_item,
                impuesto=impuesto_item,
                descuento=descuento_item,
                total=total_item
            ))

            logger.debug(
                f"Item {idx + 1}: {item.descripcion} - "
                f"Subtotal: {subtotal_item}, Impuesto: {impuesto_item}, "
                f"Descuento: {descuento_item}"
            )

        # Total final
        total_final = (
            subtotal_general + total_impuestos - total_descuentos
        ).quantize(Decimal('0.01'), rounding=ROUND_HALF_UP)

        logger.info(
            f"Cálculo completado - Subtotal: {subtotal_general}, "
            f"Impuestos: {total_impuestos}, Descuentos: {total_descuentos}, "
            f"Total: {total_final}"
        )

        return CalculationResponse(
            subtotal_general=subtotal_general,
            total_impuestos=total_impuestos,
            total_descuentos=total_descuentos,
            total_final=total_final,
            detalle_items=detalle_items,
            timestamp=datetime.now()
        )

    except Exception as e:
        logger.error(f"Error en cálculo: {str(e)}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error interno al calcular: {str(e)}"
        )


@app.get(
    "/",
    tags=["Info"],
    summary="Información del Servicio"
)
async def root():
    """
    Información básica del servicio
    """
    return {
        "service": "Tax Calculator Service",
        "version": "2.0.0",
        "description": "Microservicio de cálculo de impuestos y descuentos",
        "framework": "FastAPI",
        "docs": "/docs",
        "health": "/health"
    }


# ============= Startup/Shutdown Events =============

@app.on_event("startup")
async def startup_event():
    log_configuration()


@app.on_event("shutdown")
async def shutdown_event():
    logger.info("=" * 60)
    logger.info("Tax Calculator Service deteniendo...")
    logger.info("=" * 60)


# ============= Ejecutar con Uvicorn =============

if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "main:app",
        host=settings.host,
        port=settings.port,
        reload=settings.reload,
        log_level=settings.log_level.lower(),
        workers=1 if settings.reload else settings.workers
    )
