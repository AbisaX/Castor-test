FROM eclipse-temurin:17-jre-alpine

# Metadata
LABEL maintainer="Castor Team <contacto@castor.com>"
LABEL description="Clientes Service - Microservicio de gestión de clientes"
LABEL version="1.0.0"

# Variables de entorno
ENV JAVA_OPTS="-Xms256m -Xmx512m" \
    SPRING_PROFILES_ACTIVE=production

# Crear usuario no-root
RUN addgroup -S spring && adduser -S spring -G spring

# Directorio de trabajo
WORKDIR /app

# Copiar JAR
COPY target/clientes-service-1.0.0.jar app.jar

# Cambiar permisos
RUN chown -R spring:spring /app

# Cambiar a usuario no-root
USER spring:spring

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# Exponer puerto
EXPOSE 8081

# Comando de inicio
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
