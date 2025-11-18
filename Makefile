JAVA_SERVICES := clientes-service facturacion-service api-gateway
PY_SERVICE := tax-calculator-service

.PHONY: help build test test-coverage sonar docker-build docker-up docker-down docker-logs \
	k8s-deploy k8s-delete k8s-status client-test python-test clean dev-postgres dev-oracle \
	dev-clientes dev-facturacion dev-gateway dev-python

help:
	@echo "Comandos disponibles:"
	@echo "  make build            - Compilar microservicios Java"
	@echo "  make test             - Ejecutar pruebas de clientes y facturacion"
	@echo "  make test-coverage    - Generar reportes JaCoCo"
	@echo "  make sonar            - Enviar analisis a SonarQube"
	@echo "  make docker-build     - Construir imagenes Docker"
	@echo "  make docker-up        - Levantar toda la plataforma"
	@echo "  make docker-down      - Detener servicios Docker"
	@echo "  make docker-logs      - Ver logs de Docker Compose"
	@echo "  make k8s-deploy       - Desplegar en Kubernetes"
	@echo "  make k8s-delete       - Eliminar despliegues de Kubernetes"
	@echo "  make k8s-status       - Ver estado del namespace facturacion"
	@echo "  make python-test      - Ejecutar pytest en tax-calculator"
	@echo "  make client-test      - Ejecutar el cliente Node.js"
	@echo "  make clean            - Limpiar artefactos"

build:
	@for service in $(JAVA_SERVICES); do \
		echo ">> Compilando $$service"; \
		( cd $$service && mvn clean package -DskipTests ); \
	done

test:
	@for service in clientes-service facturacion-service; do \
		echo ">> Pruebas en $$service"; \
		( cd $$service && mvn test ); \
	done

test-coverage:
	@for service in clientes-service facturacion-service; do \
		echo ">> Cobertura en $$service"; \
		( cd $$service && mvn clean test jacoco:report ); \
	done

sonar:
	@if [ -z "$$SONAR_TOKEN" ]; then echo "Defina SONAR_TOKEN antes de ejecutar make sonar"; exit 1; fi
	@for service in clientes-service facturacion-service; do \
		echo ">> SonarQube para $$service"; \
		( cd $$service && mvn clean verify sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.login=$$SONAR_TOKEN ); \
	done

docker-build:
	docker build -t clientes-service:latest ./clientes-service
	docker build -t facturacion-service:latest ./facturacion-service
	docker build -t tax-calculator-service:latest ./tax-calculator-service
	docker build -t api-gateway:latest ./api-gateway

docker-up:
	docker-compose up -d

docker-down:
	docker-compose down

docker-logs:
	docker-compose logs -f

k8s-deploy:
	kubectl apply -f k8s/namespace.yaml
	kubectl apply -f k8s/postgres/
	kubectl apply -f k8s/oracle/
	kubectl apply -f k8s/kafka/
	kubectl apply -f k8s/zipkin/
	kubectl apply -f k8s/clientes-service/
	kubectl apply -f k8s/facturacion-service/
	kubectl apply -f k8s/tax-calculator-service/
	kubectl apply -f k8s/api-gateway/

k8s-delete:
	-kubectl delete -f k8s/api-gateway/
	-kubectl delete -f k8s/tax-calculator-service/
	-kubectl delete -f k8s/facturacion-service/
	-kubectl delete -f k8s/clientes-service/
	-kubectl delete -f k8s/zipkin/
	-kubectl delete -f k8s/kafka/
	-kubectl delete -f k8s/oracle/
	-kubectl delete -f k8s/postgres/
	-kubectl delete -f k8s/namespace.yaml

k8s-status:
	kubectl get all -n facturacion

client-test:
	cd client-nodejs && npm install && npm start

python-test:
	cd $(PY_SERVICE) && pip install -r requirements.txt && pytest --maxfail=1 -v

clean:
	@for service in $(JAVA_SERVICES); do \
		echo ">> Limpiando $$service"; \
		( cd $$service && mvn clean ); \
	done
	rm -rf client-nodejs/node_modules
	docker-compose down -v || true

dev-postgres:
	docker run -d --name postgres-dev -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=facturacion_db -p 5432:5432 postgres:15-alpine

dev-oracle:
	docker run -d --name oracle-dev -e ORACLE_PASSWORD=oracle -e APP_USER=castor_facturacion -e APP_USER_PASSWORD=castor_pass -p 1521:1521 gvenzl/oracle-xe:21-slim-faststart

dev-clientes:
	cd clientes-service && mvn spring-boot:run

dev-facturacion:
	cd facturacion-service && mvn spring-boot:run

dev-gateway:
	cd api-gateway && mvn spring-boot:run

dev-python:
	cd tax-calculator-service && uvicorn main:app --reload --port 5000
