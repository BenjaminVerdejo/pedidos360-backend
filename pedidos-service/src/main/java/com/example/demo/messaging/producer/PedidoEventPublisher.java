package com.example.demo.messaging.producer;

import com.example.demo.messaging.config.MensajeriaProperties;
import com.example.demo.messaging.dto.PedidoEvento;
import com.example.demo.model.Pedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Productor: publica eventos de pedidos. Si RabbitMQ no esta disponible
 * NO rompe el flujo de negocio: el pedido igual se guarda y el fallo queda en el log.
 */
@Component
public class PedidoEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PedidoEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final MensajeriaProperties props;

    public PedidoEventPublisher(RabbitTemplate rabbitTemplate, MensajeriaProperties props) {
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
    }

    public void publicarPedidoCreado(Pedido pedido) {
        publicar(props.routing().pedidoCreado(), PedidoEvento.de("PEDIDO_CREADO", pedido));
    }

    public void publicarCambioEstado(Pedido pedido) {
        publicar(props.routing().pedidoEstado(), PedidoEvento.de("PEDIDO_ESTADO_CAMBIADO", pedido));
    }

    private void publicar(String routingKey, PedidoEvento evento) {
        try {
            rabbitTemplate.convertAndSend(props.exchange(), routingKey, evento);
            log.info("Evento {} publicado (pedido {}, routingKey {})", evento.tipo(), evento.pedidoId(), routingKey);
        } catch (AmqpException e) {
            log.error("No se pudo publicar el evento {} del pedido {}: {}", evento.tipo(), evento.pedidoId(), e.getMessage());
        }
    }
}