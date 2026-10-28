package com.example.demo.exception;

/**
 * Error temporal (servicio externo caido, timeout, etc.).
 * El mensaje se REINTENTA hasta app.rabbit.consumidor.max-reintentos; si sigue fallando va a la DLQ.
 */
public class ErrorRecuperableException extends RuntimeException {
    public ErrorRecuperableException(String mensaje) { super(mensaje); }
    public ErrorRecuperableException(String mensaje, Throwable causa) { super(mensaje, causa); }
}
