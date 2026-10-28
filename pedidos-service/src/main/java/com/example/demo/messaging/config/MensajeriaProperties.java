package com.example.demo.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Lee los nombres y parametros de mensajeria desde application.properties (prefijo app.rabbit).
 * Es el UNICO lugar del codigo Java donde se conocen exchanges, colas y routing keys.
 */
@ConfigurationProperties(prefix = "app.rabbit")
public record MensajeriaProperties(
        String exchange,
        String dlx,
        Routing routing,
        Cola notificaciones,
        Cola tickets,
        Consumidor consumidor) {

    public record Routing(String pedidoCreado, String pedidoEstado) {}

    public record Cola(String queue, String dlq, String bindingKey) {}

    /** Parametros de los consumidores: prefetch y politica de reintentos. */
    public record Consumidor(int prefetch, int maxReintentos, long esperaMs) {}
}
