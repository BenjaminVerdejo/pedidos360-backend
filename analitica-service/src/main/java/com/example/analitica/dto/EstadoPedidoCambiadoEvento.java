package com.example.analitica.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** Contrato del evento del topico pedidos-estados (lado consumidor). */
public record EstadoPedidoCambiadoEvento(
        @NotBlank String eventoId,
        @Positive int version,
        @NotBlank String timestamp,
        @NotNull @Positive Long pedidoId,
        String estadoAnterior,
        @NotBlank String estadoNuevo,
        @PositiveOrZero BigDecimal total) {
}
