package com.example.demo.messaging;

import com.example.demo.exception.ErrorNoRecuperableException;
import com.example.demo.exception.ErrorRecuperableException;
import com.example.demo.messaging.config.MensajeriaProperties;
import com.example.demo.messaging.consumer.support.ConfirmadorMensajes;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifica las decisiones ACK / reintento / NACK->DLQ sin necesitar RabbitMQ.
 * Usa un Channel falso que solo anota las llamadas basicAck / basicNack.
 */
class ConfirmadorMensajesTest {

    private final List<String> llamadas = new ArrayList<>();
    private Channel channel;
    private ConfirmadorMensajes confirmador;

    @BeforeEach
    void setUp() {
        llamadas.clear();
        channel = (Channel) Proxy.newProxyInstance(Channel.class.getClassLoader(), new Class<?>[]{Channel.class},
                (proxy, metodo, args) -> {
                    switch (metodo.getName()) {
                        case "basicAck" -> llamadas.add("ack:" + args[0]);
                        case "basicNack" -> llamadas.add("nack:" + args[0] + ":requeue=" + args[2]);
                        default -> { }
                    }
                    return null;
                });
        MensajeriaProperties props = new MensajeriaProperties("ex", "direct", "dlx", null, null, null, null,
                new MensajeriaProperties.Consumidor(10, 2, 0));
        confirmador = new ConfirmadorMensajes(props);
    }

    @Test
    void procesamientoExitosoHaceAck() throws Exception {
        confirmador.procesar("test", 1L, channel, 7L, () -> {});
        assertEquals(List.of("ack:7"), llamadas);
    }

    @Test
    void errorNoRecuperableHaceNackSinRequeueYNoReintenta() throws Exception {
        AtomicInteger intentos = new AtomicInteger();
        confirmador.procesar("test", 1L, channel, 7L, () -> {
            intentos.incrementAndGet();
            throw new ErrorNoRecuperableException("datos invalidos");
        });
        assertEquals(1, intentos.get());
        assertEquals(List.of("nack:7:requeue=false"), llamadas);
    }

    @Test
    void errorRecuperableReintentaYSiSeRecuperaHaceAck() throws Exception {
        AtomicInteger intentos = new AtomicInteger();
        confirmador.procesar("test", 1L, channel, 7L, () -> {
            if (intentos.incrementAndGet() < 2) throw new ErrorRecuperableException("caido");
        });
        assertEquals(2, intentos.get());
        assertEquals(List.of("ack:7"), llamadas);
    }

    @Test
    void errorRecuperableAgotaReintentosYVaALaDlq() throws Exception {
        AtomicInteger intentos = new AtomicInteger();
        confirmador.procesar("test", 1L, channel, 7L, () -> {
            intentos.incrementAndGet();
            throw new ErrorRecuperableException("siempre caido");
        });
        assertEquals(3, intentos.get()); // 1 intento + 2 reintentos
        assertEquals(List.of("nack:7:requeue=false"), llamadas);
    }

    @Test
    void errorInesperadoVaALaDlq() throws Exception {
        confirmador.procesar("test", 1L, channel, 7L, () -> { throw new IllegalStateException("bug"); });
        assertEquals(List.of("nack:7:requeue=false"), llamadas);
    }
}
