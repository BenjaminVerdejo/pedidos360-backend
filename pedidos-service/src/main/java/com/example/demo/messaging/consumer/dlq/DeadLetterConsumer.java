package com.example.demo.messaging.consumer.dlq;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Dominio DLQ (mensajes no entregados).
 * Escucha notificaciones.dlq y tickets.dlq y REGISTRA EN LOG cada mensaje fallido
 * con la cola de origen, el motivo (x-death) y el contenido, para poder auditarlo
 * o reprocesarlo manualmente. Luego hace ACK para no acumularlo indefinidamente.
 */
@Component
public class DeadLetterConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterConsumer.class);

    @RabbitListener(queues = {"${app.rabbit.notificaciones.dlq}", "${app.rabbit.tickets.dlq}"})
    public void registrar(Message mensaje, Channel channel) throws IOException {
        MessageProperties props = mensaje.getMessageProperties();
        String cuerpo = new String(mensaje.getBody(), StandardCharsets.UTF_8);

        String colaOrigen = "desconocida";
        String motivo = "desconocido";
        Object veces = 1;
        List<Map<String, ?>> xDeath = props.getXDeathHeader();
        if (xDeath != null && !xDeath.isEmpty()) {
            Map<String, ?> ultimo = xDeath.get(0);
            colaOrigen = String.valueOf(ultimo.get("queue"));
            motivo = String.valueOf(ultimo.get("reason"));
            veces = ultimo.get("count");
        }

        log.error("[DLQ {}] Mensaje no entregado | cola origen: {} | motivo: {} | veces: {} | contenido: {}",
                props.getConsumerQueue(), colaOrigen, motivo, veces, cuerpo);

        channel.basicAck(props.getDeliveryTag(), false);
    }
}
