package com.example.demo.dto;

import com.example.demo.model.EstadoPedido;
import jakarta.validation.constraints.NotNull;

/** Cuerpo de PATCH /api/pedidos/{id}/estado. Un valor fuera del enum responde 400. */
public record CambioEstadoRequest(
        @NotNull(message = "El estado es obligatorio")
        EstadoPedido estado) {
}
