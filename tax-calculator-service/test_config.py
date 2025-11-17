"""
Tests para configuración y casos adicionales del Tax Calculator Service
"""

import pytest
from fastapi.testclient import TestClient
from decimal import Decimal
from main import app
from config import settings

client = TestClient(app)


class TestConfiguracion:
    """Tests de configuración y environment"""

    def test_health_version_correcta(self):
        """Debe retornar la versión configurada"""
        response = client.get("/health")
        data = response.json()
        assert data["version"] == settings.app_version

    def test_root_info_servicio(self):
        """Debe retornar información del servicio"""
        response = client.get("/")
        data = response.json()
        assert data["service"] == "Tax Calculator Service"
        assert data["version"] == settings.app_version
        assert data["framework"] == "FastAPI"

    def test_docs_accesible(self):
        """Debe poder acceder a la documentación"""
        response = client.get(settings.docs_url)
        assert response.status_code == 200

    def test_openapi_schema_accesible(self):
        """Debe poder acceder al schema OpenAPI"""
        response = client.get(settings.openapi_url)
        assert response.status_code == 200
        data = response.json()
        assert "openapi" in data
        assert "info" in data


class TestRendimientoYPrecision:
    """Tests de rendimiento y precisión numérica"""

    def test_calculo_grandes_cantidades(self):
        """Debe manejar correctamente cantidades grandes"""
        response = client.post("/calcular", json={
            "items": [
                {
                    "descripcion": "Item con cantidad grande",
                    "cantidad": 1000,
                    "precio_unitario": 999999.99,
                    "porcentaje_impuesto": 19,
                    "porcentaje_descuento": 5
                }
            ]
        })

        assert response.status_code == 200
        data = response.json()
        # Verificar que los cálculos son precisos
        assert Decimal(str(data["subtotal_general"])) == Decimal("999999990.00")

    def test_precision_decimal(self):
        """Debe mantener precisión decimal correcta"""
        response = client.post("/calcular", json={
            "items": [
                {
                    "descripcion": "Test precisión",
                    "cantidad": 1,
                    "precio_unitario": 10.50,
                    "porcentaje_impuesto": 16.5,
                    "porcentaje_descuento": 3.75
                }
            ]
        })

        data = response.json()
        # Subtotal: 10.50
        assert Decimal(str(data["subtotal_general"])) == Decimal("10.50")
        # Impuesto: 10.50 * 0.165 = 1.7325 -> 1.73 (redondeado)
        assert Decimal(str(data["total_impuestos"])) == Decimal("1.73")
        # Descuento: 10.50 * 0.0375 = 0.39375 -> 0.39 (redondeado)
        assert Decimal(str(data["total_descuentos"])) == Decimal("0.39")

    def test_muchos_items(self):
        """Debe manejar múltiples items eficientemente"""
        items = []
        for i in range(50):
            items.append({
                "descripcion": f"Item {i+1}",
                "cantidad": 1,
                "precio_unitario": 100.0,
                "porcentaje_impuesto": 19,
                "porcentaje_descuento": 0
            })

        response = client.post("/calcular", json={"items": items})

        assert response.status_code == 200
        data = response.json()
        assert len(data["detalle_items"]) == 50
        assert Decimal(str(data["subtotal_general"])) == Decimal("5000.00")


class TestValidacionesAvanzadas:
    """Tests de validaciones avanzadas"""

    def test_descripcion_muy_larga_falla(self):
        """Debe rechazar descripciones muy largas (>500 chars)"""
        descripcion_larga = "A" * 501

        response = client.post("/calcular", json={
            "items": [
                {
                    "descripcion": descripcion_larga,
                    "cantidad": 1,
                    "precio_unitario": 100.00
                }
            ]
        })

        assert response.status_code == 422

    def test_porcentaje_negativo_falla(self):
        """Debe rechazar porcentajes negativos"""
        response = client.post("/calcular", json={
            "items": [
                {
                    "descripcion": "Item",
                    "cantidad": 1,
                    "precio_unitario": 100.00,
                    "porcentaje_impuesto": -5
                }
            ]
        })

        assert response.status_code == 422

    def test_campos_faltantes_falla(self):
        """Debe rechazar requests con campos obligatorios faltantes"""
        response = client.post("/calcular", json={
            "items": [
                {
                    "cantidad": 1,
                    "precio_unitario": 100.00
                }
            ]
        })

        assert response.status_code == 422

    def test_precio_unitario_string_invalido(self):
        """Debe rechazar precio_unitario con formato inválido"""
        response = client.post("/calcular", json={
            "items": [
                {
                    "descripcion": "Test",
                    "cantidad": 1,
                    "precio_unitario": "no es un numero"
                }
            ]
        })

        assert response.status_code == 422


class TestEstructuraResponse:
    """Tests de estructura de respuestas"""

    def test_response_incluye_timestamp(self):
        """Debe incluir timestamp en la respuesta"""
        response = client.post("/calcular", json={
            "items": [
                {
                    "descripcion": "Test",
                    "cantidad": 1,
                    "precio_unitario": 100.00
                }
            ]
        })

        data = response.json()
        assert "timestamp" in data
        assert data["timestamp"] is not None

    def test_response_detalle_completo(self):
        """Debe incluir todos los campos en detalle de items"""
        response = client.post("/calcular", json={
            "items": [
                {
                    "descripcion": "Test Item",
                    "cantidad": 1,
                    "precio_unitario": 100.00,
                    "porcentaje_impuesto": 10,
                    "porcentaje_descuento": 5
                }
            ]
        })

        data = response.json()
        detalle = data["detalle_items"][0]

        campos_requeridos = ["descripcion", "subtotal", "impuesto", "descuento", "total"]
        for campo in campos_requeridos:
            assert campo in detalle, f"Campo {campo} faltante en detalle"

    def test_response_tipos_correctos(self):
        """Debe retornar tipos de datos correctos"""
        response = client.post("/calcular", json={
            "items": [
                {
                    "descripcion": "Test",
                    "cantidad": 1,
                    "precio_unitario": 100.00
                }
            ]
        })

        data = response.json()
        # Verificar que los valores numéricos son números, no strings
        assert isinstance(data["subtotal_general"], (int, float))
        assert isinstance(data["total_impuestos"], (int, float))
        assert isinstance(data["total_descuentos"], (int, float))
        assert isinstance(data["total_final"], (int, float))


class TestCasosBorde:
    """Tests de casos borde adicionales"""

    def test_precio_muy_pequeno(self):
        """Debe manejar precios muy pequeños"""
        response = client.post("/calcular", json={
            "items": [
                {
                    "descripcion": "Item barato",
                    "cantidad": 1,
                    "precio_unitario": 0.01,
                    "porcentaje_impuesto": 19,
                    "porcentaje_descuento": 0
                }
            ]
        })

        assert response.status_code == 200
        data = response.json()
        assert Decimal(str(data["subtotal_general"])) == Decimal("0.01")

    def test_solo_impuesto_sin_descuento(self):
        """Debe calcular correctamente solo con impuesto"""
        response = client.post("/calcular", json={
            "items": [
                {
                    "descripcion": "Solo impuesto",
                    "cantidad": 1,
                    "precio_unitario": 1000.00,
                    "porcentaje_impuesto": 19,
                    "porcentaje_descuento": 0
                }
            ]
        })

        data = response.json()
        assert Decimal(str(data["total_impuestos"])) == Decimal("190.00")
        assert Decimal(str(data["total_descuentos"])) == Decimal("0.00")
        assert Decimal(str(data["total_final"])) == Decimal("1190.00")

    def test_solo_descuento_sin_impuesto(self):
        """Debe calcular correctamente solo con descuento"""
        response = client.post("/calcular", json={
            "items": [
                {
                    "descripcion": "Solo descuento",
                    "cantidad": 1,
                    "precio_unitario": 1000.00,
                    "porcentaje_impuesto": 0,
                    "porcentaje_descuento": 20
                }
            ]
        })

        data = response.json()
        assert Decimal(str(data["total_impuestos"])) == Decimal("0.00")
        assert Decimal(str(data["total_descuentos"])) == Decimal("200.00")
        assert Decimal(str(data["total_final"])) == Decimal("800.00")

    def test_impuesto_100_porciento(self):
        """Debe manejar impuesto del 100%"""
        response = client.post("/calcular", json={
            "items": [
                {
                    "descripcion": "Impuesto máximo",
                    "cantidad": 1,
                    "precio_unitario": 100.00,
                    "porcentaje_impuesto": 100,
                    "porcentaje_descuento": 0
                }
            ]
        })

        data = response.json()
        assert Decimal(str(data["total_impuestos"])) == Decimal("100.00")
        assert Decimal(str(data["total_final"])) == Decimal("200.00")

    def test_descuento_100_porciento(self):
        """Debe manejar descuento del 100%"""
        response = client.post("/calcular", json={
            "items": [
                {
                    "descripcion": "Descuento máximo",
                    "cantidad": 1,
                    "precio_unitario": 100.00,
                    "porcentaje_impuesto": 0,
                    "porcentaje_descuento": 100
                }
            ]
        })

        data = response.json()
        assert Decimal(str(data["total_descuentos"])) == Decimal("100.00")
        assert Decimal(str(data["total_final"])) == Decimal("0.00")


class TestCasosCombinados:
    """Tests de casos combinados complejos"""

    def test_items_con_diferentes_porcentajes(self):
        """Debe calcular correctamente items con diferentes porcentajes"""
        response = client.post("/calcular", json={
            "items": [
                {
                    "descripcion": "Item 1 - IVA 19%",
                    "cantidad": 1,
                    "precio_unitario": 1000.00,
                    "porcentaje_impuesto": 19,
                    "porcentaje_descuento": 0
                },
                {
                    "descripcion": "Item 2 - IVA 5%",
                    "cantidad": 1,
                    "precio_unitario": 1000.00,
                    "porcentaje_impuesto": 5,
                    "porcentaje_descuento": 10
                },
                {
                    "descripcion": "Item 3 - Exento",
                    "cantidad": 1,
                    "precio_unitario": 1000.00,
                    "porcentaje_impuesto": 0,
                    "porcentaje_descuento": 0
                }
            ]
        })

        assert response.status_code == 200
        data = response.json()
        # Subtotal: 3000
        assert Decimal(str(data["subtotal_general"])) == Decimal("3000.00")
        # Impuestos: 190 + 50 + 0 = 240
        assert Decimal(str(data["total_impuestos"])) == Decimal("240.00")
        # Descuentos: 0 + 100 + 0 = 100
        assert Decimal(str(data["total_descuentos"])) == Decimal("100.00")

    def test_multiples_unidades_mismo_item(self):
        """Debe calcular correctamente con múltiples unidades"""
        response = client.post("/calcular", json={
            "items": [
                {
                    "descripcion": "Laptop",
                    "cantidad": 5,
                    "precio_unitario": 1500.00,
                    "porcentaje_impuesto": 19,
                    "porcentaje_descuento": 15
                }
            ]
        })

        data = response.json()
        # Subtotal: 5 * 1500 = 7500
        assert Decimal(str(data["subtotal_general"])) == Decimal("7500.00")
        # Impuesto: 7500 * 0.19 = 1425
        assert Decimal(str(data["total_impuestos"])) == Decimal("1425.00")
        # Descuento: 7500 * 0.15 = 1125
        assert Decimal(str(data["total_descuentos"])) == Decimal("1125.00")
        # Total: 7500 + 1425 - 1125 = 7800
        assert Decimal(str(data["total_final"])) == Decimal("7800.00")
