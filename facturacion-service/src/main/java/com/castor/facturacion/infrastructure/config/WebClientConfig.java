package com.castor.facturacion.infrastructure.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Configuración de WebClient para llamadas HTTP a servicios externos.
 *
 * Configura:
 * - Timeouts (connection, read, write)
 * - Logging de requests y responses
 * - HTTP connection pool
 */
@Configuration
public class WebClientConfig {

    private static final Logger log = LoggerFactory.getLogger(WebClientConfig.class);

    private static final int CONNECTION_TIMEOUT = 5000;  // 5 segundos
    private static final int READ_TIMEOUT = 10000;       // 10 segundos
    private static final int WRITE_TIMEOUT = 10000;      // 10 segundos

    /**
     * Bean de WebClient.Builder con configuración personalizada
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        log.info("Configurando WebClient.Builder con timeouts y logging");

        // Configurar cliente HTTP con timeouts
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECTION_TIMEOUT)
            .responseTimeout(Duration.ofMillis(READ_TIMEOUT))
            .doOnConnected(conn -> conn
                .addHandlerLast(new ReadTimeoutHandler(READ_TIMEOUT, TimeUnit.MILLISECONDS))
                .addHandlerLast(new WriteTimeoutHandler(WRITE_TIMEOUT, TimeUnit.MILLISECONDS))
            );

        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .filter(logRequest())
            .filter(logResponse());
    }

    /**
     * Filter para loguear requests salientes
     */
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            if (log.isDebugEnabled()) {
                log.debug("WebClient Request: {} {}", clientRequest.method(), clientRequest.url());
                clientRequest.headers().forEach((name, values) ->
                    values.forEach(value -> log.debug("Header: {}={}", name, value))
                );
            }
            return Mono.just(clientRequest);
        });
    }

    /**
     * Filter para loguear responses recibidas
     */
    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (log.isDebugEnabled()) {
                log.debug("WebClient Response: Status {}", clientResponse.statusCode());
                clientResponse.headers().asHttpHeaders().forEach((name, values) ->
                    values.forEach(value -> log.debug("Header: {}={}", name, value))
                );
            }
            return Mono.just(clientResponse);
        });
    }
}
