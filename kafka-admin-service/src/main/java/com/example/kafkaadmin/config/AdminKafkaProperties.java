package com.example.kafkaadmin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Reglas de la API administradora (application.yml, prefijo app.admin).
 *
 * @param topicosProtegidos topicos del sistema que NO se pueden eliminar por API
 * @param timeoutSegundos   tiempo maximo de espera de cada operacion contra el cluster
 * @param maxParticiones    limite de particiones por topico (evita configuraciones abusivas)
 */
@ConfigurationProperties(prefix = "app.admin")
public record AdminKafkaProperties(List<String> topicosProtegidos, long timeoutSegundos, int maxParticiones) {

    public AdminKafkaProperties {
        topicosProtegidos = topicosProtegidos == null ? List.of() : List.copyOf(topicosProtegidos);
        timeoutSegundos = timeoutSegundos <= 0 ? 10 : timeoutSegundos;
        maxParticiones = maxParticiones <= 0 ? 12 : maxParticiones;
    }

    public boolean esProtegido(String topico) {
        return topico.startsWith("__") || topico.startsWith("_confluent") || topicosProtegidos.contains(topico);
    }
}
