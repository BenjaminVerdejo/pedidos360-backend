package com.example.demo.messaging.kafka.dto;

import com.example.demo.model.Pedido;
import com.example.demo.util.DatosPersonales;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Evento publicado en el topico pedidos-creados.
 *
 * @param eventoId  UUID unico: permite a los consumidores descartar duplicados (idempotencia)
 * @param version   version del esquema: permite evolucionar el evento sin romper consumidores
 * @param timestamp momento del hecho (ISO-8601, UTC)
 * @param pedidoId  clave del mensaje: todos los eventos de un pedido van a la misma particion
 * @param cliente   email ENMASCARADO (Ley 21.719: minimizacion de datos personales)
 * @param descripcion detalle del pedido
 * @param total     monto del pedido (lo usa la analitica de ventas)
 * @param estado    estado inicial (siempre PENDIENTE)
 */
public record PedidoCreadoEvento(
        String eventoId,
        int version,
        String timestamp,
        Long pedidoId,
        String cliente,
        String descripcion,
        BigDecimal total,
        String estado) {

    public static final int VERSION = 1;

    public static PedidoCreadoEvento de(Pedido p) {
        return new PedidoCreadoEvento(
                UUID.randomUUID().toString(),
                VERSION,
                Instant.now().toString(),
                p.getId(),
                p.getCliente() != null ? DatosPersonales.enmascararEmail(p.getCliente().getEmail()) : null,
                p.getDescripcion(),
                p.getTotal(),
                p.getEstado());
    }
}
