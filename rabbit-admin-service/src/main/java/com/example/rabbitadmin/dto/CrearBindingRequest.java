package com.example.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear un binding exchange -> cola.
 * POST /api/admin/bindings
 * { "exchange": "pedidos.exchange", "cola": "facturas.queue", "routingKey": "pedido.creado" }
 *
 * routingKey puede ser "" (por ejemplo para exchanges FANOUT) pero no null.
 * Se permiten los comodines de topic: '*' y '#'.
 */
public record CrearBindingRequest(
        @NotBlank(message = "el exchange es obligatorio")
        @Size(max = ReglasNombre.LARGO_MAX)
        @Pattern(regexp = ReglasNombre.PATRON, message = "exchange " + ReglasNombre.MENSAJE_PATRON)
        String exchange,

        @NotBlank(message = "la cola es obligatoria")
        @Size(max = ReglasNombre.LARGO_MAX)
        @Pattern(regexp = ReglasNombre.PATRON, message = "cola " + ReglasNombre.MENSAJE_PATRON)
        String cola,

        @NotNull(message = "routingKey es obligatoria (use \"\" para FANOUT)")
        @Size(max = ReglasNombre.LARGO_MAX, message = "routingKey no puede superar 255 caracteres")
        @Pattern(regexp = "^[A-Za-z0-9._*#-]*$",
                 message = "routingKey solo puede contener letras, numeros, '.', '-', '_', '*' y '#'")
        String routingKey) {
}
