package com.example.demo.messaging.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parametros de administracion de los topicos (application.properties, prefijo app.kafka).
 * Se definen por flujo de datos porque cada uno tiene requerimientos distintos.
 */
@ConfigurationProperties(prefix = "app.kafka")
public record KafkaTopicosProperties(
        boolean habilitado,
        int particiones,
        short replicas,
        String minInsyncReplicas,
        long retencionCreadosMs,
        long retencionDltMs) {
}
