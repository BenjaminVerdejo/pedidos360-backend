package com.example.analitica.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Contrato del evento del topico pedidos-creados (lado consumidor).
 * Se valida al recibirlo: un evento sin id, sin pedido o con total negativo va al DLT.
 */
public record PedidoCreadoEvento(
        @NotBlank String eventoId,
        @Positive int version,
        @NotBlank String timestamp,
        @NotNull @Positive Long pedidoId,
        String cliente,
        String descripcion,
        @NotNull @PositiveOrZero BigDecimal total,
        @NotBlank String estado) {
}
