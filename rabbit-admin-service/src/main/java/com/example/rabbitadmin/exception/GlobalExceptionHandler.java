package com.example.rabbitadmin.exception;

import com.example.rabbitadmin.dto.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.LinkedHashMap;
import java.util.Map;

/** Traduce las excepciones a respuestas JSON claras con el status HTTP correcto. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** @Valid @RequestBody con errores (nombre vacio, ttl negativo, etc.). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> cuerpoInvalido(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> campos.merge(e.getField(), e.getDefaultMessage(), (a, b) -> a + "; " + b));
        ex.getBindingResult().getGlobalErrors()
                .forEach(e -> campos.merge(e.getObjectName(), e.getDefaultMessage(), (a, b) -> a + "; " + b));
        return ResponseEntity.badRequest().body(ErrorResponse.validacion(campos));
    }

    /** @PathVariable / @RequestParam con @Pattern, @NotBlank, etc. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> parametroInvalido(HandlerMethodValidationException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getAllValidationResults().forEach(r -> r.getResolvableErrors().forEach(e ->
                campos.merge(String.valueOf(r.getMethodParameter().getParameterName()),
                        e.getDefaultMessage(), (a, b) -> a + "; " + b)));
        return ResponseEntity.badRequest().body(ErrorResponse.validacion(campos));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> restriccion(ConstraintViolationException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v ->
                campos.merge(v.getPropertyPath().toString(), v.getMessage(), (a, b) -> a + "; " + b));
        return ResponseEntity.badRequest().body(ErrorResponse.validacion(campos));
    }

    /** JSON mal formado o enum invalido (ej. "tipo": "CUALQUIERA"). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> jsonInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.de(400, "json_invalido",
                "El cuerpo no es un JSON valido o tiene valores no permitidos (tipo: DIRECT, TOPIC, FANOUT, HEADERS)"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> faltaParametro(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.de(400, "parametro_faltante",
                "Falta el parametro obligatorio '" + ex.getParameterName() + "'"));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.de(404, "no_encontrado", ex.getMessage()));
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ErrorResponse> conflicto(ConflictoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.de(409, "conflicto", ex.getMessage()));
    }

    @ExceptionHandler(AmqpConnectException.class)
    public ResponseEntity<ErrorResponse> rabbitCaido(AmqpConnectException ex) {
        log.error("RabbitMQ no disponible: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.de(503, "rabbitmq_no_disponible", "No se pudo conectar con RabbitMQ"));
    }

    @ExceptionHandler(AmqpException.class)
    public ResponseEntity<ErrorResponse> errorRabbit(AmqpException ex) {
        log.error("Error de RabbitMQ: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ErrorResponse.de(502, "error_rabbitmq", "RabbitMQ rechazo la operacion: " + ex.getMessage()));
    }
}
