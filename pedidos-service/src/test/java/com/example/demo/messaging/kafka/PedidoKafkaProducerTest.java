package com.example.demo.messaging.kafka;

import com.example.demo.messaging.kafka.dto.EstadoPedidoCambiadoEvento;
import com.example.demo.messaging.kafka.dto.PedidoCreadoEvento;
import com.example.demo.model.Cliente;
import com.example.demo.model.Pedido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Productor Kafka: topico, clave, contenido del DTO y tolerancia a fallos (KafkaTemplate simulado). */
class PedidoKafkaProducerTest {

    private KafkaTemplate<String, Object> template;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        template = mock(KafkaTemplate.class);
        CompletableFuture<SendResult<String, Object>> ok = CompletableFuture.completedFuture(null);
        when(template.send(anyString(), anyString(), any())).thenReturn(ok);
    }

    private static Pedido pedido(String estado) {
        Cliente c = new Cliente();
        c.setNombre("Ana");
        c.setEmail("ana.perez@empresa.cl");
        Pedido p = new Pedido();
        p.setId(42L);
        p.setCliente(c);
        p.setDescripcion("Notebook");
        p.setTotal(new BigDecimal("599990"));
        p.setEstado(estado);
        return p;
    }

    @Test
    void pedidoCreadoSePublicaEnSuTopicoConElIdComoClave() {
        new PedidoKafkaProducer(template, true).publicarPedidoCreado(pedido("PENDIENTE"));

        ArgumentCaptor<Object> evento = ArgumentCaptor.forClass(Object.class);
        verify(template).send(eq(KafkaTopicos.PEDIDOS_CREADOS), eq("42"), evento.capture());
        PedidoCreadoEvento e = (PedidoCreadoEvento) evento.getValue();
        assertEquals(42L, e.pedidoId());
        assertEquals("PENDIENTE", e.estado());
        assertEquals(new BigDecimal("599990"), e.total());
        assertEquals(PedidoCreadoEvento.VERSION, e.version());
        assertNotNull(e.eventoId());
        assertEquals("a***@empresa.cl", e.cliente(), "el email debe viajar enmascarado");
    }

    @Test
    void cambioDeEstadoIncluyeEstadoAnteriorYNuevo() {
        new PedidoKafkaProducer(template, true).publicarCambioEstado(pedido("ENTREGADO"), "ENVIADO");

        ArgumentCaptor<Object> evento = ArgumentCaptor.forClass(Object.class);
        verify(template).send(eq(KafkaTopicos.PEDIDOS_ESTADOS), eq("42"), evento.capture());
        EstadoPedidoCambiadoEvento e = (EstadoPedidoCambiadoEvento) evento.getValue();
        assertEquals("ENVIADO", e.estadoAnterior());
        assertEquals("ENTREGADO", e.estadoNuevo());
    }

    @Test
    void cadaEventoTieneUnIdDistinto() {
        PedidoKafkaProducer producer = new PedidoKafkaProducer(template, true);
        producer.publicarPedidoCreado(pedido("PENDIENTE"));
        producer.publicarPedidoCreado(pedido("PENDIENTE"));

        ArgumentCaptor<Object> eventos = ArgumentCaptor.forClass(Object.class);
        verify(template, times(2)).send(anyString(), anyString(), eventos.capture());
        assertNotEquals(((PedidoCreadoEvento) eventos.getAllValues().get(0)).eventoId(),
                ((PedidoCreadoEvento) eventos.getAllValues().get(1)).eventoId());
    }

    @Test
    void deshabilitadoNoPublica() {
        new PedidoKafkaProducer(template, false).publicarPedidoCreado(pedido("PENDIENTE"));
        verifyNoInteractions(template);
    }

    @Test
    void siKafkaFallaElNegocioNoSeInterrumpe() {
        when(template.send(anyString(), anyString(), any())).thenThrow(new RuntimeException("cluster no disponible"));
        assertDoesNotThrow(() -> new PedidoKafkaProducer(template, true).publicarPedidoCreado(pedido("PENDIENTE")));
    }

    @Test
    void errorAsincronoDeEnvioSoloSeRegistra() {
        CompletableFuture<SendResult<String, Object>> fallo = new CompletableFuture<>();
        fallo.completeExceptionally(new RuntimeException("NotEnoughReplicas"));
        when(template.send(anyString(), anyString(), any())).thenReturn(fallo);
        assertDoesNotThrow(() -> new PedidoKafkaProducer(template, true).publicarCambioEstado(pedido("ENVIADO"), "PENDIENTE"));
    }
}
