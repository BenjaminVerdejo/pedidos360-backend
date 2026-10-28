package com.example.demo.messaging.consumer.tickets;

import com.example.demo.messaging.consumer.support.ConfirmadorMensajes;
import com.example.demo.messaging.dto.PedidoEvento;
import com.example.demo.service.TicketService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Dominio TICKETS / DOCUMENTOS.
 * Escucha tickets.queue (binding pedido.creado) y genera el ticket del pedido.
 */
@Component
public class TicketConsumer {

    private final TicketService ticketService;
    private final ConfirmadorMensajes confirmador;

    public TicketConsumer(TicketService ticketService, ConfirmadorMensajes confirmador) {
        this.ticketService = ticketService;
        this.confirmador = confirmador;
    }

    @RabbitListener(queues = "${app.rabbit.tickets.queue}")
    public void recibir(PedidoEvento evento, Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        confirmador.procesar("tickets", evento.pedidoId(), channel, deliveryTag,
                () -> ticketService.generarTicket(evento));
    }
}
