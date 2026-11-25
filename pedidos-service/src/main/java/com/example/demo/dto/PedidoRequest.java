package com.example.demo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Cuerpo de POST /api/pedidos.
 * Solo contiene los campos que el cliente puede controlar. id, estado y fecha los asigna
 * el backend, asi se evita la asignacion masiva (CWE-915) que existia al recibir la entidad JPA.
 */
public record PedidoRequest(
        @NotNull(message = "El cliente es obligatorio")
        @Valid
        ClienteRequest cliente,

        @NotBlank(message = "La descripcion es obligatoria")
        @Size(max = 255, message = "La descripcion no puede superar 255 caracteres")
        String descripcion,

        @NotNull(message = "El total es obligatorio")
        @Positive(message = "El total debe ser mayor que 0")
        @DecimalMax(value = "99999999.99", message = "El total supera el maximo permitido")
        @Digits(integer = 8, fraction = 2, message = "El total admite hasta 2 decimales")
        BigDecimal total) {
}
