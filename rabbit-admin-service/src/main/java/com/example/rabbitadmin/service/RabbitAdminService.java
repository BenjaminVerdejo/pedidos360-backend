package com.example.rabbitadmin.service;

import com.example.rabbitadmin.config.AdminProperties;
import com.example.rabbitadmin.dto.BindingResponse;
import com.example.rabbitadmin.dto.ColaResponse;
import com.example.rabbitadmin.dto.CrearBindingRequest;
import com.example.rabbitadmin.dto.CrearColaRequest;
import com.example.rabbitadmin.dto.CrearExchangeRequest;
import com.example.rabbitadmin.dto.ExchangeResponse;
import com.example.rabbitadmin.exception.ConflictoException;
import com.example.rabbitadmin.exception.RecursoNoEncontradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpIOException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.HeadersExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Service;

/**
 * Unico punto del microservicio que habla con RabbitMQ.
 * Encapsula la creacion y eliminacion de colas, exchanges y bindings;
 * los controladores solo llaman a estos metodos de alto nivel.
 */
@Service
public class RabbitAdminService {

    private static final Logger log = LoggerFactory.getLogger(RabbitAdminService.class);

    private final RabbitAdmin rabbitAdmin;
    private final AdminProperties props;

    public RabbitAdminService(RabbitAdmin rabbitAdmin, AdminProperties props) {
        this.rabbitAdmin = rabbitAdmin;
        this.props = props;
    }

    // ===================== COLAS =====================

    public ColaResponse crearCola(CrearColaRequest req) {
        if (existeCola(req.nombre())) {
            throw new ConflictoException("La cola '" + req.nombre() + "' ya existe");
        }
        if (req.deadLetterExchange() != null && !existeExchange(req.deadLetterExchange())) {
            throw new RecursoNoEncontradoException(
                    "El exchange de dead letter '" + req.deadLetterExchange() + "' no existe");
        }

        QueueBuilder builder = req.esDurable()
                ? QueueBuilder.durable(req.nombre())
                : QueueBuilder.nonDurable(req.nombre());
        if (req.esAutoDelete()) builder.autoDelete();
        if (req.ttlMs() != null) builder.ttl(req.ttlMs());
        if (req.maxLength() != null) builder.maxLength(req.maxLength());
        if (req.deadLetterExchange() != null) builder.deadLetterExchange(req.deadLetterExchange());
        if (req.deadLetterRoutingKey() != null) builder.deadLetterRoutingKey(req.deadLetterRoutingKey());

        Queue cola = builder.build();
        rabbitAdmin.declareQueue(cola);
        log.info("Cola creada: {} (durable={}, args={})", cola.getName(), cola.isDurable(), cola.getArguments());
        return new ColaResponse(cola.getName(), 0, 0);
    }

    public ColaResponse obtenerCola(String nombre) {
        QueueInformation info = rabbitAdmin.getQueueInfo(nombre);
        if (info == null) {
            throw new RecursoNoEncontradoException("La cola '" + nombre + "' no existe");
        }
        return new ColaResponse(info.getName(), info.getMessageCount(), info.getConsumerCount());
    }

    /**
     * @param soloSiVacia si es true y la cola tiene mensajes, no se elimina (409).
     */
    public void eliminarCola(String nombre, boolean soloSiVacia) {
        if (props.protegidos().colas().contains(nombre)) {
            throw new ConflictoException("La cola '" + nombre + "' es parte del sistema y no se puede eliminar");
        }
        ColaResponse cola = obtenerCola(nombre); // lanza 404 si no existe
        if (soloSiVacia && cola.mensajes() > 0) {
            throw new ConflictoException("La cola '" + nombre + "' tiene " + cola.mensajes() + " mensajes pendientes");
        }
        rabbitAdmin.deleteQueue(nombre);
        log.info("Cola eliminada: {}", nombre);
    }

    // ===================== EXCHANGES =====================

    public ExchangeResponse crearExchange(CrearExchangeRequest req) {
        if (existeExchange(req.nombre())) {
            throw new ConflictoException("El exchange '" + req.nombre() + "' ya existe");
        }
        boolean durable = req.esDurable();
        boolean autoDelete = req.esAutoDelete();
        Exchange exchange = switch (req.tipo()) {
            case DIRECT -> new DirectExchange(req.nombre(), durable, autoDelete);
            case TOPIC -> new TopicExchange(req.nombre(), durable, autoDelete);
            case FANOUT -> new FanoutExchange(req.nombre(), durable, autoDelete);
            case HEADERS -> new HeadersExchange(req.nombre(), durable, autoDelete);
        };
        rabbitAdmin.declareExchange(exchange);
        log.info("Exchange creado: {} ({})", req.nombre(), req.tipo());
        return new ExchangeResponse(req.nombre(), req.tipo(), durable, autoDelete);
    }

    public void eliminarExchange(String nombre) {
        if (props.protegidos().exchanges().contains(nombre)) {
            throw new ConflictoException("El exchange '" + nombre + "' es parte del sistema y no se puede eliminar");
        }
        if (!existeExchange(nombre)) {
            throw new RecursoNoEncontradoException("El exchange '" + nombre + "' no existe");
        }
        rabbitAdmin.deleteExchange(nombre);
        log.info("Exchange eliminado: {}", nombre);
    }

    // ===================== BINDINGS =====================

    public BindingResponse crearBinding(CrearBindingRequest req) {
        validarExtremos(req.exchange(), req.cola());
        rabbitAdmin.declareBinding(binding(req.exchange(), req.cola(), req.routingKey()));
        log.info("Binding creado: {} --[{}]--> {}", req.exchange(), req.routingKey(), req.cola());
        return new BindingResponse(req.exchange(), req.cola(), req.routingKey());
    }

    public void eliminarBinding(String exchange, String cola, String routingKey) {
        validarExtremos(exchange, cola);
        rabbitAdmin.removeBinding(binding(exchange, cola, routingKey));
        log.info("Binding eliminado: {} --[{}]--> {}", exchange, routingKey, cola);
    }

    // ===================== Apoyo interno =====================

    private void validarExtremos(String exchange, String cola) {
        if (!existeExchange(exchange)) {
            throw new RecursoNoEncontradoException("El exchange '" + exchange + "' no existe");
        }
        if (!existeCola(cola)) {
            throw new RecursoNoEncontradoException("La cola '" + cola + "' no existe");
        }
    }

    private static Binding binding(String exchange, String cola, String routingKey) {
        return new Binding(cola, Binding.DestinationType.QUEUE, exchange, routingKey, null);
    }

    private boolean existeCola(String nombre) {
        return rabbitAdmin.getQueueInfo(nombre) != null;
    }

    /** RabbitMQ no tiene "get exchange": se usa una declaracion pasiva (falla si no existe). */
    private boolean existeExchange(String nombre) {
        try {
            rabbitAdmin.getRabbitTemplate().execute(channel -> channel.exchangeDeclarePassive(nombre));
            return true;
        } catch (AmqpIOException e) {
            return false;
        }
    }
}
