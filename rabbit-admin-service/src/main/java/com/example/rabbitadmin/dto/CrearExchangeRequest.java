package com.example.rabbitadmin.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear un exchange.
 * POST /api/admin/exchanges
 * { "nombre": "facturas.exchange", "tipo": "TOPIC", "durable": true }
 */
public record CrearExchangeRequest(
        @NotBlank(message = "el nombre del exchange es obligatorio")
        @Size(max = ReglasNombre.LARGO_MAX, message = "el nombre no puede superar 255 caracteres")
        @Pattern(regexp = ReglasNombre.PATRON, message = "el nombre " + ReglasNombre.MENSAJE_PATRON)
        String nombre,

        @NotNull(message = "el tipo es obligatorio: DIRECT, TOPIC, FANOUT o HEADERS")
        TipoExchange tipo,

        Boolean durable,
        Boolean autoDelete) {

    @AssertTrue(message = "el nombre no puede empezar con 'amq.' (reservado por RabbitMQ)")
    public boolean isNombreNoReservado() {
        return ReglasNombre.noReservado(nombre);
    }

    public boolean esDurable() {
        return !Boolean.FALSE.equals(durable);
    }

    public boolean esAutoDelete() {
        return Boolean.TRUE.equals(autoDelete);
    }
}
