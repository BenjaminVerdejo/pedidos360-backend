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