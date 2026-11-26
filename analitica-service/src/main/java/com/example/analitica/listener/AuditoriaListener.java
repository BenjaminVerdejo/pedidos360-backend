package com.example.analitica.listener;

import com.example.analitica.config.KafkaTopicos;
import com.example.analitica.service.AuditoriaService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor del grupo "auditoria": escucha los DOS topicos. Al ser otro consumer group, Kafka le
 * entrega una copia de cada evento, independiente del grupo "analitica" (patron publicar/suscribir).
 */
@Component
public class AuditoriaListener {

    private final AuditoriaService auditoria;

    public AuditoriaListener(AuditoriaService auditoria) {
        this.auditoria = auditoria;
    }

    @KafkaListener(id = "auditoria-pedidos", topics = {KafkaTopicos.PEDIDOS_CREADOS, KafkaTopicos.PEDIDOS_ESTADOS},
            groupId = KafkaTopicos.GRUPO_AUDITORIA, concurrency = "1")
    public void auditar(ConsumerRecord<String, Object> registro) {
        String tipo = registro.value() != null ? registro.value().getClass().getSimpleName() : "desconocido";
        auditoria.registrar(tipo, registro.key(), registro.topic(), registro.partition(), registro.offset());
    }
}
