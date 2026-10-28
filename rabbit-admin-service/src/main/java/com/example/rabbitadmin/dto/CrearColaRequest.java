package com.example.rabbitadmin.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear una cola.
 * POST /api/admin/queues
 * { "nombre": "facturas.queue", "durable": true, "ttlMs": 60000, "maxLength": 1000,
 *   "deadLetterExchange": "pedidos.dlx", "deadLetterRoutingKey": "facturas.dlq" }
 */
public record CrearColaRequest(
        @NotBlank(message = "el nombre de la cola es obligatorio")
        @Size(max = ReglasNombre.LARGO_MAX, message = "el nombre no puede superar 255 caracteres")
        @Pattern(regexp = ReglasNombre.PATRON, message = "el nombre " + ReglasNombre.MENSAJE_PATRON)
        String nombre,

        Boolean durable,
        Boolean autoDelete,

        @Positive(message = "ttlMs debe ser mayor que 0")
        @Max(value = 604_800_000, message = "ttlMs no puede superar 7 dias (604800000 ms)")
        Integer ttlMs,

        @Positive(message = "maxLength debe ser mayor que 0")
        @Max(value = 1_000_000, message = "maxLength no puede superar 1.000.000 mensajes")
        Integer maxLength,

        @Size(max = ReglasNombre.LARGO_MAX)
        @Pattern(regexp = ReglasNombre.PATRON, message = "deadLetterExchange " + ReglasNombre.MENSAJE_PATRON)
        String deadLetterExchange,

        @Size(max = ReglasNombre.LARGO_MAX)
        @Pattern(regexp = ReglasNombre.PATRON, message = "deadLetterRoutingKey " + ReglasNombre.MENSAJE_PATRON)
        String deadLetterRoutingKey) {

    @AssertTrue(message = "el nombre no puede empezar con 'amq.' (reservado por RabbitMQ)")
    public boolean isNombreNoReservado() {
        return ReglasNombre.noReservado(nombre);
    }

    @AssertTrue(message = "deadLetterRoutingKey requiere indicar deadLetterExchange")
    public boolean isDeadLetterConsistente() {
        return deadLetterRoutingKey == null || deadLetterExchange != null;
    }

    public boolean esDurable() {
        return !Boolean.FALSE.equals(durable);   // por defecto true
    }

    public boolean esAutoDelete() {
        return Boolean.TRUE.equals(autoDelete);  // por defecto false
    }
}
