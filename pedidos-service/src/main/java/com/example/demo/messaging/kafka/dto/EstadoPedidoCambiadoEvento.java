package com.example.demo.messaging.kafka.dto;

import com.example.demo.model.Pedido;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Evento publicado en el topico pedidos-estados cada vez que un pedido cambia de estado.
 * Incluye el estado anterior y el nuevo para que la analitica pueda mover el pedido entre
 * contadores sin consultar la base de datos de pedidos-service.
 */
public record EstadoPedidoCambiadoEvento(
        String eventoId,
        int version,
        String timestamp,
        Long pedidoId,
        String estadoAnterior,
        String estadoNuevo,
        BigDecimal total) {

    public static final int VERSION = 1;

    public static EstadoPedidoCambiadoEvento de(Pedido p, String estadoAnterior) {
        return new EstadoPedidoCambiadoEvento(
                UUID.randomUUID().toString(),
                VERSION,
                Instant.now().toString(),
                p.getId(),
                estadoAnterior,
                p.getEstado(),
                p.getTotal());
    }
}
