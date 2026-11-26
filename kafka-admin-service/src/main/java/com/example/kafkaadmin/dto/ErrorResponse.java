package com.example.kafkaadmin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/** Formato unico de error de la API (sin trazas ni detalles internos). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(int status, String error, String mensaje, Map<String, String> campos, Instant timestamp) {

    public static ErrorResponse de(int status, String error, String mensaje) {
        return new ErrorResponse(status, error, mensaje, null, Instant.now());
    }
}
