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

    /** Topic exchange: lo reciben notificaciones (pedido.#) y tickets (pedido.creado). */
    public void publicarPedidoCreado(Pedido pedido) {
        publicar(props.exchange(), props.routing().pedidoCreado(),
                PedidoEvento.de(PedidoEvento.PEDIDO_CREADO, pedido));
    }

    /** Topic exchange: lo recibe notificaciones (pedido.#). */
    public void publicarCambioEstado(Pedido pedido) {
        publicar(props.exchange(), props.routing().pedidoEstado(),
                PedidoEvento.de(PedidoEvento.PEDIDO_ESTADO_CAMBIADO, pedido));
    }

    /** Direct exchange: lo recibe solo documentos (clave exacta pedido.entregado). */
    public void publicarPedidoEntregado(Pedido pedido) {
        publicar(props.directExchange(), props.routing().pedidoEntregado(),
                PedidoEvento.de(PedidoEvento.PEDIDO_ENTREGADO, pedido));
    }

    private void publicar(String exchange, String routingKey, PedidoEvento evento) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, evento);
            log.info("Evento {} publicado (pedido {}, exchange {}, routingKey {})",
                    evento.tipo(), evento.pedidoId(), exchange, routingKey);
        } catch (AmqpException e) {
            log.error("No se pudo publicar el evento {} del pedido {}: {}", evento.tipo(), evento.pedidoId(), e.getMessage());
        }
    }
}