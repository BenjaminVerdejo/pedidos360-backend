package com.example.analitica;

import com.example.analitica.dto.EstadoPedidoCambiadoEvento;
import com.example.analitica.dto.PedidoCreadoEvento;
import com.example.analitica.exception.EventoInvalidoException;
import com.example.analitica.listener.AuditoriaListener;
import com.example.analitica.listener.PedidoEventosListener;
import com.example.analitica.service.AnaliticaService;
import com.example.analitica.service.AuditoriaService;
import com.example.analitica.service.ValidadorEventos;
import jakarta.validation.Validation;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/** Consumidores: validan el payload y delegan en la capa de dominio. */
class ListenerYValidacionTest {

    private AnaliticaService analitica;
    private PedidoEventosListener listener;

    @BeforeEach
    void setUp() {
        analitica = new AnaliticaService();
        ValidadorEventos validador = new ValidadorEventos(Validation.buildDefaultValidatorFactory().getValidator());
        listener = new PedidoEventosListener(analitica, validador);
    }

    @Test
    void eventoValidoActualizaLaAnalitica() {
        listener.onPedidoCreado(new PedidoCreadoEvento("e-1", 1, "2026-11-25T10:00:00Z", 10L,
                "a***@c.cl", "x", new BigDecimal("990"), "PENDIENTE"), "pedidos-creados", 1, 0);
        assertEquals(1, analitica.resumen().pedidosCreados());
    }

    @Test
    void eventoSinPedidoIdEsInvalidoYNoSeProcesa() {
        PedidoCreadoEvento malo = new PedidoCreadoEvento("e-2", 1, "2026-11-25T10:00:00Z", null,
                null, "x", new BigDecimal("990"), "PENDIENTE");
        EventoInvalidoException ex = assertThrows(EventoInvalidoException.class,
                () -> listener.onPedidoCreado(malo, "pedidos-creados", 0, 1));
        assertTrue(ex.getMessage().contains("pedidoId"));
        assertEquals(0, analitica.resumen().pedidosCreados());
    }

    @Test
    void totalNegativoEsInvalido() {
        PedidoCreadoEvento malo = new PedidoCreadoEvento("e-3", 1, "2026-11-25T10:00:00Z", 5L,
                null, "x", new BigDecimal("-1"), "PENDIENTE");
        assertThrows(EventoInvalidoException.class, () -> listener.onPedidoCreado(malo, "pedidos-creados", 0, 2));
    }

    @Test
    void cambioDeEstadoSinEstadoNuevoEsInvalido() {
        EstadoPedidoCambiadoEvento malo = new EstadoPedidoCambiadoEvento("e-4", 1, "2026-11-25T10:00:00Z", 5L,
                "PENDIENTE", " ", null);
        assertThrows(EventoInvalidoException.class, () -> listener.onEstadoCambiado(malo, "pedidos-estados", 0, 3));
    }

    @Test
    void payloadNuloEsInvalido() {
        assertThrows(EventoInvalidoException.class, () -> listener.onPedidoCreado(null, "pedidos-creados", 0, 4));
    }

    @Test
    void auditoriaRegistraEventosDeAmbosTopicos() {
        AuditoriaService auditoria = new AuditoriaService();
        AuditoriaListener aud = new AuditoriaListener(auditoria);
        aud.auditar(new ConsumerRecord<String, Object>("pedidos-creados", 2, 15L, "8", "payload"));
        aud.auditar(new ConsumerRecord<String, Object>("pedidos-estados", 1, 3L, "8", "payload"));

        assertEquals(2, auditoria.ultimos().size());
        assertEquals("pedidos-estados", auditoria.ultimos().get(0).topico());
        assertEquals(8L, auditoria.ultimos().get(1).pedidoId());
    }
}
