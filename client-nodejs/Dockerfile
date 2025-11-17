# ============================================================================
# Dockerfile para Cliente Node.js - Sistema de Facturación
# Imagen optimizada multi-stage para cliente de pruebas
# ============================================================================

# Stage 1: Build (aunque este cliente no necesita build, mantiene consistencia)
FROM node:20-alpine AS builder

# Metadata
LABEL maintainer="Castor"
LABEL description="Cliente Node.js para probar API de facturación"
LABEL version="1.0.0"

# Set working directory
WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
# Usar npm ci para instalación determinística en CI/CD
RUN npm ci --only=production && \
    npm cache clean --force

# Copy application code
COPY index.js ./

# ============================================================================
# Stage 2: Runtime
FROM node:20-alpine

# Set working directory
WORKDIR /app

# Create non-root user for security
RUN addgroup -g 1001 -S nodejs && \
    adduser -S nodejs -u 1001 && \
    chown -R nodejs:nodejs /app

# Copy from builder
COPY --from=builder --chown=nodejs:nodejs /app/node_modules ./node_modules
COPY --from=builder --chown=nodejs:nodejs /app/package*.json ./
COPY --from=builder --chown=nodejs:nodejs /app/index.js ./

# Switch to non-root user
USER nodejs

# Environment variables (can be overridden at runtime)
ENV NODE_ENV=production
ENV API_URL=http://api-gateway:8080

# Health check (verifica que Node.js puede ejecutarse)
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD node -e "process.exit(0)" || exit 1

# Default command
CMD ["node", "index.js"]
