"""
Tests para Tax Calculator Service
Usa pytest y httpx para tests asíncronos
"""

import pytest
from fastapi.testclient import TestClient
from decimal import Decimal
from main import app

client = TestClient(app)


class TestHealthEndpoint:
    """Tests para el endpoint de health check"""

    def test_health_check_retorna_200(self):
        """Debe retornar 200 OK"""
        response = client.get("/health")
        assert response.status_code == 200

    def test_health_check_estructura(self):
        """Debe retornar estructura correcta"""
        response = client.get("/health")
        data = response.json()

        assert "status" in data
        assert "service" in data
        assert "version" in data
        assert "timestamp" in data
        assert data["status"] == "healthy"
        assert data["service"] == "tax-calculator"


class TestCalcularImpuestosEndpoint:
    """Tests para el endpoint de cálculo de impuestos"""

    def test_calculo_exitoso_un_item(self):
        """Debe calcular correctamente con un item"""
        response = client.post("/calcular", json={
            "items": [
                {
                    "descripcion": "Producto Test",
                    "cantidad": 2,
                    "precio_unitario": 100.00,
                    "porcentaje_impuesto": 19,
                    "porcentaje_descuento": 10
                }
            ]
        })

        assert response.status_code == 200
        data = response.json()

        # Subtotal: 2 * 100 = 200
        assert Decimal(str(data["subtotal_general"])) == Decimal("200.00")
        # Impuesto: 200 * 19% = 38
        assert Decimal(str(data["total_impuestos"])) == Decimal("38.00")
        # Descuento: 200 * 10% = 20
        assert Decimal(str(data["total_descuentos"])) == Decimal("20.00")
        # Total: 200 + 38 - 20 = 218
        assert Decimal(str(data["total_final"])) == Decimal("218.00")

    def test_calculo_multiple_items(self):
        """Debe calcular correctamente con múltiples items"""
        response = client.post("/calcular", json={
            "items": [
                {
                    "descripcion": "Item 1",
                    "cantidad": 1,
                    "precio_unitario": 1000.00,
                    "porcentaje_impuesto": 19,
                    "porcentaje_descuento": 0
                },
                {
                    "descripcion": "Item 2",
                    "cantidad": 2,
                    "precio_unitario": 500.00,
                    "porcentaje_impuesto": 19,
                    "porcentaje_descuento": 5
                }
            ]
        })

        assert response.status_code == 200
        data = response.json()

        # Subtotal: 1000 + 1000 = 2000
        assert Decimal(str(data["subtotal_general"])) == Decimal("2000.00")
        # Impuestos: (1000 * 0.19) + (1000 * 0.19) = 380
        assert Decimal(str(data["total_impuestos"])) == Decimal("380.00")
        # Descuentos: 0 + (1000 * 0.05) = 50
        assert Decimal(str(data["total_descuentos"])) == Decimal("50.00")

    def test_detalle_items_incluido(self):
        """Debe incluir detalle de cada item"""
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
        assert "detalle_items" in data
        assert len(data["detalle_items"]) == 1

        detalle = data["detalle_items"][0]
        assert detalle["descripcion"] == "Test Item"
        assert "subtotal" in detalle
        assert "impuesto" in detalle
        assert "descuento" in detalle
        assert "total" in detalle


class TestValidaciones:
    """Tests de validaciones de input"""

    def test_cantidad_negativa_falla(self):
        """Debe rechazar cantidad negativa"""
        response = client.post("/calcular", json={
            "items": [
                {
                    "descripcion": "Item",
                    "cantidad": -1,
                    "precio_unitario": 100.00
                }
            ]
        })

        assert response.status_code == 422  # Unprocessable Entity

    def test_precio_cero_falla(self):
        """Debe rechazar precio cero"""
        response = client.post("/calcular", json={
            "items": [
                {
                    "descripcion": "Item",
                    "cantidad": 1,
                    "precio_unitario": 0
                }
            ]
        })

        assert response.status_code == 422

    def test_porcentaje_mayor_100_falla(self):
        """Debe rechazar porcentaje mayor a 100"""
        response = client.post("/calcular", json={
            "items": [
                {
                    "descripcion": "Item",
                    "cantidad": 1,
                    "precio_unitario": 100.00,
                    "porcentaje_impuesto": 150
                }
            ]
        })

        assert response.status_code == 422

    def test_lista_vacia_falla(self):
        """Debe rechazar lista vacía de items"""
        response = client.post("/calcular", json={
            "items": []
        })

        assert response.status_code == 422

    def test_descripcion_vacia_falla(self):
        """Debe rechazar descripción vacía"""
        response = client.post("/calcular", json={
            "items": [
                {
                    "descripcion": "",
                    "cantidad": 1,
                    "precio_unitario": 100.00
                }
            ]
        })

        assert response.status_code == 422


class TestEdgeCases:
    """Tests de casos límite"""

    def test_sin_impuesto_ni_descuento(self):
        """Debe calcular correctamente sin impuesto ni descuento"""
        response = client.post("/calcular", json={
            "items": [
                {
                    "descripcion": "Item",
                    "cantidad": 1,
                    "precio_unitario": 100.00,
                    "porcentaje_impuesto": 0,
                    "porcentaje_descuento": 0
                }
            ]
        })

        data = response.json()
        assert Decimal(str(data["subtotal_general"])) == Decimal("100.00")
        assert Decimal(str(data["total_impuestos"])) == Decimal("0.00")
        assert Decimal(str(data["total_descuentos"])) == Decimal("0.00")
        assert Decimal(str(data["total_final"])) == Decimal("100.00")

    def test_porcentajes_por_defecto(self):
        """Debe usar porcentajes por defecto (0)"""
        response = client.post("/calcular", json={
            "items": [
                {
                    "descripcion": "Item",
                    "cantidad": 1,
                    "precio_unitario": 100.00
                }
            ]
        })

        assert response.status_code == 200
        data = response.json()
        assert Decimal(str(data["total_impuestos"])) == Decimal("0.00")
        assert Decimal(str(data["total_descuentos"])) == Decimal("0.00")

    def test_decimales_redondeados(self):
        """Debe redondear decimales correctamente"""
        response = client.post("/calcular", json={
            "items": [
                {
                    "descripcion": "Item",
                    "cantidad": 3,
                    "precio_unitario": 33.33,
                    "porcentaje_impuesto": 19,
                    "porcentaje_descuento": 0
                }
            ]
        })

        data = response.json()
        # Todos los valores deben tener exactamente 2 decimales
        assert "." in str(data["subtotal_general"])
        assert "." in str(data["total_impuestos"])
        assert "." in str(data["total_final"])


class TestRootEndpoint:
    """Tests para el endpoint raíz"""

    def test_root_retorna_info(self):
        """Debe retornar información del servicio"""
        response = client.get("/")
        assert response.status_code == 200

        data = response.json()
        assert "service" in data
        assert "version" in data
        assert "docs" in data


# Fixtures para tests
@pytest.fixture
def item_ejemplo():
    """Fixture con un item de ejemplo"""
    return {
        "descripcion": "Laptop Dell",
        "cantidad": 1,
        "precio_unitario": 2500000.00,
        "porcentaje_impuesto": 19,
        "porcentaje_descuento": 5
    }


@pytest.fixture
def request_valido(item_ejemplo):
    """Fixture con un request válido"""
    return {"items": [item_ejemplo]}


def test_con_fixture(request_valido):
    """Test usando fixtures"""
    response = client.post("/calcular", json=request_valido)
    assert response.status_code == 200
