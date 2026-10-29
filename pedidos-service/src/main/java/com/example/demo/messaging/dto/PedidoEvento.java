package com.example.demo.messaging.dto;

import com.example.demo.model.Pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Mensaje que viaja por RabbitMQ cuando ocurre algo con un pedido. */
public record PedidoEvento(
        String tipo,
        Long pedidoId,
        String clienteNombre,
        String clienteEmail,
        String descripcion,
        BigDecimal total,
        String estado,
        LocalDateTime fecha) {

    // Tipos de evento (un solo lugar)
    public static final String PEDIDO_CREADO = "PEDIDO_CREADO";
    public static final String PEDIDO_ESTADO_CAMBIADO = "PEDIDO_ESTADO_CAMBIADO";
    public static final String PEDIDO_ENTREGADO = "PEDIDO_ENTREGADO";

    public static PedidoEvento de(String tipo, Pedido p) {
        return new PedidoEvento(
                tipo,
                p.getId(),
                p.getCliente().getNombre(),
                p.getCliente().getEmail(),
                p.getDescripcion(),
                p.getTotal(),
                p.getEstado(),
                p.getFecha());
    }
}