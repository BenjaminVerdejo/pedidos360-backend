package com.example.demo.messaging.consumer.documentos;

import com.example.demo.messaging.consumer.support.ConfirmadorMensajes;
import com.example.demo.messaging.dto.PedidoEvento;
import com.example.demo.service.DocumentoService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Dominio DOCUMENTOS.
 * Escucha documentos.queue (exchange DIRECT pedidos.direct, clave pedido.entregado)
 * y genera el comprobante de entrega del pedido.
 */
@Component
public class DocumentoConsumer {

    private final DocumentoService documentoService;
    private final ConfirmadorMensajes confirmador;

    public DocumentoConsumer(DocumentoService documentoService, ConfirmadorMensajes confirmador) {
        this.documentoService = documentoService;
        this.confirmador = confirmador;
    }

    @RabbitListener(queues = "${app.rabbit.documentos.queue}")
    public void recibir(PedidoEvento evento, Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        confirmador.procesar("documentos", evento.pedidoId(), channel, deliveryTag,
                () -> documentoService.generarComprobanteEntrega(evento));
    }
}
