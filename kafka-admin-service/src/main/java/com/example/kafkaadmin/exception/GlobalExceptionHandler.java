package com.example.kafkaadmin.exception;

import com.example.kafkaadmin.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Traduce excepciones a respuestas JSON con el status correcto y sin exponer detalles internos. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> campos.merge(e.getField(), String.valueOf(e.getDefaultMessage()), (a, b) -> a + "; " + b));
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(400, "validacion", "La peticion tiene datos invalidos", campos, Instant.now()));
    }

    /** Parametros de ruta invalidos (por ejemplo, un nombre de topico con caracteres no permitidos). */
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> parametroInvalido() {
        return ResponseEntity.badRequest().body(ErrorResponse.de(400, "parametro_invalido", "Nombre de recurso invalido"));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> peticionInvalida(Exception ex) {
        String mensaje = ex instanceof IllegalArgumentException ? ex.getMessage() : "JSON invalido";
        return ResponseEntity.badRequest().body(ErrorResponse.de(400, "peticion_invalida", mensaje));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.de(404, "no_encontrado", ex.getMessage()));
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ErrorResponse> conflicto(ConflictoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.de(409, "conflicto", ex.getMessage()));
    }

    @ExceptionHandler(OperacionKafkaException.class)
    public ResponseEntity<ErrorResponse> kafkaNoDisponible(OperacionKafkaException ex) {
        log.error("Operacion contra Kafka fallida: {}", ex.getMessage(), ex.getCause());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.de(503, "kafka_no_disponible", ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> accesoDenegado() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse.de(403, "forbidden", "Se requiere el rol Admin"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> inesperado(Exception ex) {
        if (ex instanceof org.springframework.web.ErrorResponse mvc) {
            int status = mvc.getStatusCode().value();
            return ResponseEntity.status(status).body(ErrorResponse.de(status, "error_peticion", "La peticion no se pudo procesar"));
        }
        String incidente = UUID.randomUUID().toString();
        log.error("Error inesperado [incidente {}]", incidente, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.de(500, "error_interno", "Error interno. Codigo de incidente: " + incidente));
    }
}
