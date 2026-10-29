package com.example.demo.messaging.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia RabbitMQ: exchanges, colas, bindings y DLQ.
 * Toda la configuracion de mensajeria vive aqui; la logica de negocio no declara nada.
 *
 * Flujo:
 *   pedidos.exchange (topic) --pedido.#------> notificaciones.queue --(rechazo)--> pedidos.dlx --> notificaciones.dlq
 *   pedidos.exchange (topic) --pedido.creado-> tickets.queue        --(rechazo)--> pedidos.dlx --> tickets.dlq
 *   pedidos.direct  (direct) --pedido.entregado-> documentos.queue  --(rechazo)--> pedidos.dlx --> documentos.dlq
 *
 * Rutas de mensajeria (caso de uso -> productor -> cola -> consumidor):
 *   1. Notificar al cliente    : PedidoEventPublisher -> notificaciones.queue -> NotificacionConsumer
 *   2. Generar ticket/documento: PedidoEventPublisher -> tickets.queue        -> TicketConsumer
 *   3. Comprobante de entrega  : PedidoEventPublisher -> documentos.queue     -> DocumentoConsumer
 *   4. Mensajes fallidos       : (nack sin requeue)   -> *.dlq                -> DeadLetterConsumer (log)
 */
@Configuration
@EnableConfigurationProperties(MensajeriaProperties.class)
public class RabbitConfig {

    // ---------- Exchanges ----------
    @Bean
    public TopicExchange pedidosExchange(MensajeriaProperties p) {
        return new TopicExchange(p.exchange(), true, false);
    }

    @Bean
    public DirectExchange pedidosDirectExchange(MensajeriaProperties p) {
        return new DirectExchange(p.directExchange(), true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange(MensajeriaProperties p) {
        return new DirectExchange(p.dlx(), true, false);
    }

    // ---------- Notificaciones ----------
    @Bean
    public Queue notificacionesQueue(MensajeriaProperties p) {
        return QueueBuilder.durable(p.notificaciones().queue())
                .deadLetterExchange(p.dlx())
                .deadLetterRoutingKey(p.notificaciones().dlq())
                .build();
    }

    @Bean
    public Queue notificacionesDlq(MensajeriaProperties p) {
        return QueueBuilder.durable(p.notificaciones().dlq()).build();
    }

    @Bean
    public Binding notificacionesBinding(Queue notificacionesQueue, TopicExchange pedidosExchange,
                                         MensajeriaProperties p) {
        return BindingBuilder.bind(notificacionesQueue).to(pedidosExchange)
                .with(p.notificaciones().bindingKey());
    }

    @Bean
    public Binding notificacionesDlqBinding(Queue notificacionesDlq, DirectExchange deadLetterExchange,
                                            MensajeriaProperties p) {
        return BindingBuilder.bind(notificacionesDlq).to(deadLetterExchange)
                .with(p.notificaciones().dlq());
    }

    // ---------- Tickets ----------
    @Bean
    public Queue ticketsQueue(MensajeriaProperties p) {
        return QueueBuilder.durable(p.tickets().queue())
                .deadLetterExchange(p.dlx())
                .deadLetterRoutingKey(p.tickets().dlq())
                .build();
    }

    @Bean
    public Queue ticketsDlq(MensajeriaProperties p) {
        return QueueBuilder.durable(p.tickets().dlq()).build();
    }

    @Bean
    public Binding ticketsBinding(Queue ticketsQueue, TopicExchange pedidosExchange, MensajeriaProperties p) {
        return BindingBuilder.bind(ticketsQueue).to(pedidosExchange).with(p.tickets().bindingKey());
    }

    @Bean
    public Binding ticketsDlqBinding(Queue ticketsDlq, DirectExchange deadLetterExchange, MensajeriaProperties p) {
        return BindingBuilder.bind(ticketsDlq).to(deadLetterExchange).with(p.tickets().dlq());
    }

    // ---------- Documentos (exchange DIRECT) ----------
    @Bean
    public Queue documentosQueue(MensajeriaProperties p) {
        return QueueBuilder.durable(p.documentos().queue())
                .deadLetterExchange(p.dlx())
                .deadLetterRoutingKey(p.documentos().dlq())
                .build();
    }

    @Bean
    public Queue documentosDlq(MensajeriaProperties p) {
        return QueueBuilder.durable(p.documentos().dlq()).build();
    }

    @Bean
    public Binding documentosBinding(Queue documentosQueue, DirectExchange pedidosDirectExchange,
                                     MensajeriaProperties p) {
        return BindingBuilder.bind(documentosQueue).to(pedidosDirectExchange).with(p.documentos().bindingKey());
    }

    @Bean
    public Binding documentosDlqBinding(Queue documentosDlq, DirectExchange deadLetterExchange,
                                        MensajeriaProperties p) {
        return BindingBuilder.bind(documentosDlq).to(deadLetterExchange).with(p.documentos().dlq());
    }

    // ---------- Mensajes en JSON ----------
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    // ---------- Contenedor de listeners: ACK MANUAL ----------
    /**
     * Todos los @RabbitListener usan esta fabrica:
     *  - AcknowledgeMode.MANUAL: el consumidor decide ack / nack explicitamente.
     *  - defaultRequeueRejected=false: un mensaje rechazado nunca vuelve a la cola
     *    (evita bucles infinitos); va al DLX y termina en la DLQ.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter,
            MensajeriaProperties p) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setDefaultRequeueRejected(false);
        factory.setPrefetchCount(p.consumidor().prefetch());
        return factory;
    }
}
