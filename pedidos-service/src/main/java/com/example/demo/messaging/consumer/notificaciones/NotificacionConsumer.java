package com.example.demo.messaging.consumer.notificaciones;

import com.example.demo.messaging.consumer.support.ConfirmadorMensajes;
import com.example.demo.messaging.dto.PedidoEvento;
import com.example.demo.service.NotificacionService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Dominio NOTIFICACIONES.
 * Escucha notificaciones.queue (binding pedido.#: creados y cambios de estado)
 * y delega en NotificacionService. El ACK/NACK lo decide ConfirmadorMensajes.
 */
@Component
public class NotificacionConsumer {

    private final NotificacionService notificacionService;
    private final ConfirmadorMensajes confirmador;

    public NotificacionConsumer(NotificacionService notificacionService, ConfirmadorMensajes confirmador) {
        this.notificacionService = notificacionService;
        this.confirmador = confirmador;
    }

    @RabbitListener(queues = "${app.rabbit.notificaciones.queue}")
    public void recibir(PedidoEvento evento, Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        confirmador.procesar("notificaciones", evento.pedidoId(), channel, deliveryTag,
                () -> notificacionService.notificar(evento));
    }
}
