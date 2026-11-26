package com.example.analitica.service;

import com.example.analitica.dto.EstadoPedidoCambiadoEvento;
import com.example.analitica.dto.EventoProcesado;
import com.example.analitica.dto.PedidoCreadoEvento;
import com.example.analitica.dto.ResumenAnalitica;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Logica de dominio de la analitica (no conoce Kafka: recibe DTO ya validados).
 *
 * Mantiene en memoria el estado actual de cada pedido y las metricas agregadas. Es idempotente:
 * si Kafka entrega un evento dos veces (por ejemplo, despues de un rebalanceo), el eventoId repetido
 * se descarta y las metricas no se duplican.
 *
 * Los metodos son synchronized porque el listener corre con 3 hilos (uno por particion).
 */
@Service
public class AnaliticaService {

    private static final Logger log = LoggerFactory.getLogger(AnaliticaService.class);
    private static final int MAX_EVENTOS_RECIENTES = 20;
    private static final int MAX_IDS_RECORDADOS = 10_000;

    private final Map<Long, String> estadoPorPedido = new HashMap<>();
    private final Map<Long, BigDecimal> totalPorPedido = new HashMap<>();
    private final Deque<EventoProcesado> recientes = new ArrayDeque<>();
    /** Ultimos eventoId procesados (LRU acotado) para descartar duplicados. */
    private final Map<String, Boolean> procesados = new LinkedHashMap<>(16, 0.75f, false) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            return size() > MAX_IDS_RECORDADOS;
        }
    };

    private long pedidosCreados;
    private long cambiosDeEstado;
    private long duplicados;
    private BigDecimal ventasTotales = BigDecimal.ZERO;
    private String actualizadoEn = Instant.now().toString();

    /** @return false si el evento ya habia sido procesado (duplicado) */
    public synchronized boolean registrarPedidoCreado(PedidoCreadoEvento e, String topico, int particion, long offset) {
        if (esDuplicado(e.eventoId())) {
            return false;
        }
        boolean nuevo = !estadoPorPedido.containsKey(e.pedidoId());
        estadoPorPedido.putIfAbsent(e.pedidoId(), e.estado());
        totalPorPedido.put(e.pedidoId(), e.total());
        if (nuevo) {
            pedidosCreados++;
            ventasTotales = ventasTotales.add(e.total());
        }
        agregarReciente(new EventoProcesado("PedidoCreado", e.pedidoId(),
                "Nuevo pedido por $" + e.total().toPlainString(), topico, particion, offset, e.timestamp()));
        log.info("Analitica: pedido {} creado (total {}) [particion {}, offset {}]", e.pedidoId(), e.total(), particion, offset);
        return true;
    }

    public synchronized boolean registrarCambioEstado(EstadoPedidoCambiadoEvento e, String topico, int particion, long offset) {
        if (esDuplicado(e.eventoId())) {
            return false;
        }
        estadoPorPedido.put(e.pedidoId(), e.estadoNuevo());
        if (e.total() != null) {
            totalPorPedido.putIfAbsent(e.pedidoId(), e.total());
        }
        cambiosDeEstado++;
        String anterior = e.estadoAnterior() != null ? e.estadoAnterior() : "?";
        agregarReciente(new EventoProcesado("EstadoCambiado", e.pedidoId(),
                anterior + " -> " + e.estadoNuevo(), topico, particion, offset, e.timestamp()));
        log.info("Analitica: pedido {} {} -> {} [particion {}, offset {}]",
                e.pedidoId(), anterior, e.estadoNuevo(), particion, offset);
        return true;
    }

    public synchronized ResumenAnalitica resumen() {
        Map<String, Long> porEstado = new TreeMap<>();
        for (String estado : List.of("PENDIENTE", "ENVIADO", "ENTREGADO", "CANCELADO")) {
            porEstado.put(estado, 0L);
        }
        estadoPorPedido.values().forEach(estado -> porEstado.merge(estado, 1L, Long::sum));

        BigDecimal ventasEntregadas = estadoPorPedido.entrySet().stream()
                .filter(en -> "ENTREGADO".equals(en.getValue()))
                .map(en -> totalPorPedido.getOrDefault(en.getKey(), BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ticket = pedidosCreados == 0 ? BigDecimal.ZERO
                : ventasTotales.divide(BigDecimal.valueOf(pedidosCreados), 2, RoundingMode.HALF_UP);

        return new ResumenAnalitica(
                pedidosCreados,
                porEstado.getOrDefault("ENTREGADO", 0L),
                cambiosDeEstado,
                ventasTotales,
                ventasEntregadas,
                ticket,
                porEstado,
                duplicados,
                new ArrayList<>(recientes),
                actualizadoEn);
    }

    private boolean esDuplicado(String eventoId) {
        if (procesados.containsKey(eventoId)) {
            duplicados++;
            log.warn("Evento duplicado descartado: {}", eventoId);
            return true;
        }
        procesados.put(eventoId, Boolean.TRUE);
        actualizadoEn = Instant.now().toString();
        return false;
    }

    private void agregarReciente(EventoProcesado evento) {
        recientes.addFirst(evento);
        while (recientes.size() > MAX_EVENTOS_RECIENTES) {
            recientes.removeLast();
        }
    }
}
