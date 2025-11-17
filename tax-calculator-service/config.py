"""
Configuración del Tax Calculator Service
Maneja variables de entorno y configuración por ambiente
"""

import os
from typing import Literal
from pydantic_settings import BaseSettings
from pydantic import Field


class Settings(BaseSettings):
    """
    Configuración de la aplicación usando Pydantic Settings
    Lee variables de entorno automáticamente
    """

    # Información del servicio
    app_name: str = Field(default="Tax Calculator Service", description="Nombre del servicio")
    app_version: str = Field(default="2.0.0", description="Versión del servicio")

    # Servidor
    port: int = Field(default=5000, description="Puerto del servidor")
    host: str = Field(default="0.0.0.0", description="Host del servidor")
    workers: int = Field(default=4, ge=1, le=16, description="Número de workers de Uvicorn")
    reload: bool = Field(default=False, description="Auto-reload en desarrollo")

    # Logging
    log_level: Literal["DEBUG", "INFO", "WARNING", "ERROR", "CRITICAL"] = Field(
        default="INFO",
        description="Nivel de logging"
    )
    log_format: str = Field(
        default="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
        description="Formato de logs"
    )

    # API Documentation
    docs_url: str = Field(default="/docs", description="URL de Swagger UI")
    redoc_url: str = Field(default="/redoc", description="URL de ReDoc")
    openapi_url: str = Field(default="/openapi.json", description="URL del schema OpenAPI")

    # CORS
    cors_origins: list[str] = Field(
        default=["*"],
        description="Orígenes permitidos para CORS"
    )
    cors_allow_credentials: bool = Field(default=True, description="Permitir credenciales CORS")
    cors_allow_methods: list[str] = Field(default=["*"], description="Métodos permitidos CORS")
    cors_allow_headers: list[str] = Field(default=["*"], description="Headers permitidos CORS")

    # Business Logic
    default_tax_rate: float = Field(default=19.0, ge=0, le=100, description="Tasa de impuesto por defecto")
    default_discount_rate: float = Field(default=0.0, ge=0, le=100, description="Tasa de descuento por defecto")
    max_items_per_request: int = Field(default=100, ge=1, description="Máximo de items por request")

    # Performance
    enable_gzip: bool = Field(default=True, description="Habilitar compresión gzip")
    gzip_minimum_size: int = Field(default=500, description="Tamaño mínimo para gzip (bytes)")

    # Security
    enable_https_redirect: bool = Field(default=False, description="Redirigir HTTP a HTTPS")

    # Environment
    environment: Literal["development", "staging", "production"] = Field(
        default="production",
        description="Ambiente de ejecución"
    )

    class Config:
        """Configuración de Pydantic Settings"""
        env_file = ".env"
        env_file_encoding = "utf-8"
        case_sensitive = False

        # Mapeo de variables de entorno
        fields = {
            "port": {"env": ["PORT", "APP_PORT"]},
            "log_level": {"env": ["LOG_LEVEL", "LOGGING_LEVEL"]},
            "workers": {"env": ["WORKERS", "UVICORN_WORKERS"]},
            "environment": {"env": ["ENVIRONMENT", "ENV", "APP_ENV"]},
        }


# Instancia global de configuración
settings = Settings()


def get_settings() -> Settings:
    """
    Factory function para obtener la configuración
    Útil para dependency injection en FastAPI
    """
    return settings


# Función para logging de configuración
def log_configuration():
    """Imprime la configuración actual (sin secretos)"""
    import logging
    logger = logging.getLogger(__name__)

    logger.info("=" * 60)
    logger.info("CONFIGURACIÓN DEL SERVICIO")
    logger.info("=" * 60)
    logger.info(f"Aplicación: {settings.app_name} v{settings.app_version}")
    logger.info(f"Ambiente: {settings.environment}")
    logger.info(f"Host: {settings.host}:{settings.port}")
    logger.info(f"Workers: {settings.workers}")
    logger.info(f"Log Level: {settings.log_level}")
    logger.info(f"Auto-reload: {settings.reload}")
    logger.info(f"Docs URL: {settings.docs_url}")
    logger.info(f"CORS Origins: {settings.cors_origins}")
    logger.info(f"Max Items per Request: {settings.max_items_per_request}")
    logger.info("=" * 60)
