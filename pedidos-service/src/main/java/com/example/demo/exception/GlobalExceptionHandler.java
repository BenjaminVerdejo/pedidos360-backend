package com.example.demo.exception;

import com.example.demo.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Traduce las excepciones a respuestas JSON con el status correcto.
 * Antes, un JSON mal formado o un id no numerico terminaba en la pagina de error por defecto;
 * ahora la respuesta es controlada y no expone detalles internos (CWE-209).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** @Valid con errores: email invalido, total negativo, textos demasiado largos, etc. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> cuerpoInvalido(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> campos.merge(e.getField(), String.valueOf(e.getDefaultMessage()), (a, b) -> a + "; " + b));
        return ResponseEntity.badRequest().body(ErrorResponse.validacion(campos));
    }

    /** JSON mal formado o estado fuera del enum (ej. "estado": "HACKEADO"). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> jsonInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.de(400, "json_invalido",
                "El cuerpo no es un JSON valido o tiene valores no permitidos "
                        + "(estado: PENDIENTE, ENVIADO, ENTREGADO, CANCELADO)"));
    }

    /** Parametro de ruta con tipo incorrecto, ej. /api/pedidos/1 OR 1=1 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> tipoInvalido(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.de(400, "parametro_invalido",
                "El parametro '" + ex.getName() + "' no tiene un formato valido"));
    }

    @ExceptionHandler(TransicionEstadoInvalidaException.class)
    public ResponseEntity<ErrorResponse> transicionInvalida(TransicionEstadoInvalidaException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.de(409, "transicion_invalida", ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integridad(DataIntegrityViolationException ex) {
        log.warn("Violacion de integridad de datos: {}", ex.getMostSpecificCause().getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.de(409, "conflicto", "La operacion entra en conflicto con datos existentes"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> metodoNoPermitido(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ErrorResponse.de(405, "metodo_no_permitido", "Metodo HTTP no permitido para este recurso"));
    }

    /**
     * @PreAuthorize lanza AccessDeniedException dentro del controlador. Se responde 403 aqui
     * para que el manejador generico de abajo no lo convierta en 500.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> accesoDenegado(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.de(403, "forbidden", "No tienes permisos para este recurso"));
    }

    /** Cualquier otro error: se registra con un id de incidente y al cliente solo se le entrega ese id. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> inesperado(Exception ex) {
        // Excepciones propias de Spring MVC (404 ruta inexistente, 415 content-type, etc.)
        // ya traen su status HTTP: se respeta en vez de responder 500.
        if (ex instanceof org.springframework.web.ErrorResponse mvc) {
            int status = mvc.getStatusCode().value();
            return ResponseEntity.status(status)
                    .body(ErrorResponse.de(status, "error_peticion", "La peticion no se pudo procesar"));
        }
        String incidente = UUID.randomUUID().toString();
        log.error("Error inesperado [incidente {}]", incidente, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.de(500, "error_interno", "Error interno. Codigo de incidente: " + incidente));
    }
}
