package com.example.demo.service;

import com.example.demo.exception.ErrorNoRecuperableException;
import com.example.demo.exception.ErrorRecuperableException;
import com.example.demo.messaging.dto.PedidoEvento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/** Reglas de negocio de los servicios que usan los consumidores (sin RabbitMQ). */
class ServiciosConsumidoresTest {

    private static PedidoEvento evento(String tipo, String descripcion, String email, BigDecimal total) {
        return new PedidoEvento(tipo, 1L, "Ana", email, descripcion, total, "PENDIENTE",
                LocalDateTime.of(2026, 10, 28, 12, 0));
    }

    @Test
    void ticketSeGeneraConCodigo() {
        String codigo = new TicketService().generarTicket(
                evento(PedidoEvento.PEDIDO_CREADO, "ok", "a@a.cl", BigDecimal.TEN));
        assertEquals("TCK-20261028-1", codigo);
    }

    @Test
    void ticketConTotalInvalidoEsNoRecuperable() {
        assertThrows(ErrorNoRecuperableException.class, () -> new TicketService().generarTicket(
                evento(PedidoEvento.PEDIDO_CREADO, "ok", "a@a.cl", BigDecimal.ZERO)));
    }

    @Test
    void notificacionSinEmailEsNoRecuperable() {
        assertThrows(ErrorNoRecuperableException.class, () -> new NotificacionService().notificar(
                evento(PedidoEvento.PEDIDO_CREADO, "ok", " ", BigDecimal.TEN)));
    }

    @Test
    void fallaSimuladaDeCorreoEsRecuperable() {
        assertThrows(ErrorRecuperableException.class, () -> new NotificacionService().notificar(
                evento(PedidoEvento.PEDIDO_CREADO, "x #falla-correo", "a@a.cl", BigDecimal.TEN)));
    }

    @Test
    void documentoDeEntregaSeGenera() {
        String doc = new DocumentoService().generarComprobanteEntrega(
                evento(PedidoEvento.PEDIDO_ENTREGADO, "ok", "a@a.cl", BigDecimal.TEN));
        assertEquals("ENTREGA-1.pdf", doc);
    }

    @Test
    void documentoConEventoEquivocadoEsNoRecuperable() {
        assertThrows(ErrorNoRecuperableException.class, () -> new DocumentoService().generarComprobanteEntrega(
                evento(PedidoEvento.PEDIDO_CREADO, "ok", "a@a.cl", BigDecimal.TEN)));
    }
}
