package com.castor.facturacion.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configuración de cache con Caffeine.
 *
 * Define caches para:
 * - clientesActivos: Cache de validaciones de clientes activos
 * - clientesExistentes: Cache de existencia de clientes
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    /**
     * Configuración del CacheManager con Caffeine
     */
    @Bean
    public CacheManager cacheManager() {
        log.info("Configurando CacheManager con Caffeine");

        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "clientesActivos",
            "clientesExistentes"
        );

        cacheManager.setCaffeine(caffeineCacheBuilder());

        log.info("Cache configurado - TTL: 5 minutos, Max size: 1000 entradas");

        return cacheManager;
    }

    /**
     * Configuración del builder de Caffeine
     */
    private Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
            // Tiempo de expiración después del último acceso
            .expireAfterWrite(5, TimeUnit.MINUTES)
            // Tamaño máximo del cache
            .maximumSize(1000)
            // Habilitar estadísticas
            .recordStats()
            // Listener de eventos de cache
            .removalListener((key, value, cause) ->
                log.debug("Cache entry removed - Key: {}, Cause: {}", key, cause));
    }
}
