package com.example.demo.messaging.kafka;

import com.example.demo.messaging.kafka.dto.EstadoPedidoCambiadoEvento;
import com.example.demo.messaging.kafka.dto.PedidoCreadoEvento;
import com.example.demo.model.Pedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

/**
 * Productor Kafka de pedidos-service. Complementa a RabbitMQ (no lo reemplaza):
 * - RabbitMQ: tareas que se procesan una vez (notificaciones, tickets, comprobantes) con ACK y DLQ.
 * - Kafka: registro ordenado y re-leible de todos los eventos, consumido por analitica y auditoria.
 *
 * La clave del mensaje es el id del pedido: Kafka garantiza el orden dentro de una particion,
 * asi PENDIENTE -> ENVIADO -> ENTREGADO de un mismo pedido se consumen en ese orden.
 *
 * El envio es asincrono: si Kafka no esta disponible, el pedido igual se guarda y el error queda
 * en el log (el productor reintenta segun la configuracion retries/delivery.timeout.ms).
 */
@Component
public class PedidoKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(PedidoKafkaProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final boolean habilitado;

    public PedidoKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate,
                               @Value("${app.kafka.habilitado:true}") boolean habilitado) {
        this.kafkaTemplate = kafkaTemplate;
        this.habilitado = habilitado;
    }

    public void publicarPedidoCreado(Pedido pedido) {
        enviar(KafkaTopicos.PEDIDOS_CREADOS, pedido.getId(), PedidoCreadoEvento.de(pedido));
    }

    public void publicarCambioEstado(Pedido pedido, String estadoAnterior) {
        enviar(KafkaTopicos.PEDIDOS_ESTADOS, pedido.getId(), EstadoPedidoCambiadoEvento.de(pedido, estadoAnterior));
    }

    private void enviar(String topico, Long pedidoId, Object evento) {
        if (!habilitado) {
            log.debug("Kafka deshabilitado (app.kafka.habilitado=false): no se publica en {}", topico);
            return;
        }
        String clave = String.valueOf(pedidoId);
        try {
            kafkaTemplate.send(topico, clave, evento)
                    .whenComplete((resultado, error) -> registrarResultado(topico, clave, resultado, error));
        } catch (RuntimeException e) {
            // por ejemplo, timeout obteniendo metadatos del cluster (max.block.ms)
            log.error("No se pudo publicar en Kafka (topico {}, pedido {}): {}", topico, clave, e.getMessage());
        }
    }

    private void registrarResultado(String topico, String clave, SendResult<String, Object> resultado, Throwable error) {
        if (error != null) {
            log.error("Kafka rechazo el evento del pedido {} en {}: {}", clave, topico, error.getMessage());
        } else if (resultado != null && resultado.getRecordMetadata() != null) {
            log.info("Evento publicado en Kafka: topico={} particion={} offset={} pedido={}",
                    topico, resultado.getRecordMetadata().partition(), resultado.getRecordMetadata().offset(), clave);
        }
    }
}
