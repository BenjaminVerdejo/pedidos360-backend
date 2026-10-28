package com.example.rabbitadmin.dto;

import java.time.Instant;
import java.util.Map;

/** Formato unico de error de la API. "campos" detalla los errores de validacion. */
public record ErrorResponse(int status, String error, String mensaje, Map<String, String> campos, String timestamp) {

    public static ErrorResponse de(int status, String error, String mensaje) {
        return new ErrorResponse(status, error, mensaje, Map.of(), Instant.now().toString());
    }

    public static ErrorResponse validacion(Map<String, String> campos) {
        return new ErrorResponse(400, "validacion", "La solicitud tiene datos invalidos", campos, Instant.now().toString());
    }
}
