package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Formato unico de error de la API. Nunca incluye trazas, nombres de clases ni SQL
 * (OWASP A05: no filtrar detalles internos en los mensajes de error).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int status,
        String error,
        String mensaje,
        Map<String, String> campos,
        Instant timestamp) {

    public static ErrorResponse de(int status, String error, String mensaje) {
        return new ErrorResponse(status, error, mensaje, null, Instant.now());
    }

    public static ErrorResponse validacion(Map<String, String> campos) {
        return new ErrorResponse(400, "validacion", "La peticion tiene datos invalidos", campos, Instant.now());
    }
}
