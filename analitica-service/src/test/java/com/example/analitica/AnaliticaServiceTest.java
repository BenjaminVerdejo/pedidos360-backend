package com.example.analitica;

import com.example.analitica.dto.EstadoPedidoCambiadoEvento;
import com.example.analitica.dto.PedidoCreadoEvento;
import com.example.analitica.dto.ResumenAnalitica;
import com.example.analitica.service.AnaliticaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Logica de dominio de la analitica: metricas, transiciones de estado e idempotencia. */
class AnaliticaServiceTest {

    private AnaliticaService service;

    @BeforeEach
    void setUp() {
        service = new AnaliticaService();
    }

    private static PedidoCreadoEvento creado(long id, String total) {
        return new PedidoCreadoEvento(UUID.randomUUID().toString(), 1, "2026-11-25T10:00:00Z", id,
                "a***@correo.cl", "Pedido " + id, new BigDecimal(total), "PENDIENTE");
    }

    private static EstadoPedidoCambiadoEvento cambio(long id, String anterior, String nuevo) {
        return new EstadoPedidoCambiadoEvento(UUID.randomUUID().toString(), 1, "2026-11-25T10:05:00Z", id,
                anterior, nuevo, null);
    }

    @Test
    void cuentaPedidosYVentas() {
        service.registrarPedidoCreado(creado(1, "1000"), "pedidos-creados", 0, 0);
        service.registrarPedidoCreado(creado(2, "3000"), "pedidos-creados", 1, 0);

        ResumenAnalitica r = service.resumen();
        assertEquals(2, r.pedidosCreados());
        assertEquals(0, new BigDecimal("4000").compareTo(r.ventasTotales()));
        assertEquals(0, new BigDecimal("2000").compareTo(r.ticketPromedio()));
        assertEquals(2L, r.pedidosPorEstado().get("PENDIENTE"));
    }

    @Test
    void elCambioDeEstadoMueveElPedidoEntreContadores() {
        service.registrarPedidoCreado(creado(7, "5000"), "pedidos-creados", 2, 10);
        service.registrarCambioEstado(cambio(7, "PENDIENTE", "ENVIADO"), "pedidos-estados", 2, 3);
        service.registrarCambioEstado(cambio(7, "ENVIADO", "ENTREGADO"), "pedidos-estados", 2, 4);

        ResumenAnalitica r = service.resumen();
        assertEquals(0L, r.pedidosPorEstado().get("PENDIENTE"));
        assertEquals(1L, r.pedidosPorEstado().get("ENTREGADO"));
        assertEquals(1, r.pedidosEntregados());
        assertEquals(2, r.cambiosDeEstado());
        assertEquals(0, new BigDecimal("5000").compareTo(r.ventasEntregadas()));
    }

    @Test
    void unEventoDuplicadoNoAlteraLasMetricas() {
        PedidoCreadoEvento e = creado(3, "1500");
        assertTrue(service.registrarPedidoCreado(e, "pedidos-creados", 0, 5));
        assertFalse(service.registrarPedidoCreado(e, "pedidos-creados", 0, 5), "mismo eventoId = duplicado");

        ResumenAnalitica r = service.resumen();
        assertEquals(1, r.pedidosCreados());
        assertEquals(1, r.eventosDuplicadosDescartados());
    }

    @Test
    void guardaLosUltimosVeinteEventosConSuParticionYOffset() {
        for (int i = 1; i <= 25; i++) {
            service.registrarPedidoCreado(creado(i, "10"), "pedidos-creados", i % 3, i);
        }
        ResumenAnalitica r = service.resumen();
        assertEquals(20, r.ultimosEventos().size());
        assertEquals(25L, r.ultimosEventos().get(0).pedidoId(), "el mas reciente va primero");
        assertEquals(25L, r.ultimosEventos().get(0).offset());
    }

    @Test
    void cambioDeEstadoDeUnPedidoNoVistoTambienSeRegistra() {
        service.registrarCambioEstado(cambio(99, "PENDIENTE", "CANCELADO"), "pedidos-estados", 1, 0);
        assertEquals(1L, service.resumen().pedidosPorEstado().get("CANCELADO"));
    }
}
