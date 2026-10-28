package com.example.demo.exception;

/**
 * Error permanente (mensaje invalido, datos incompletos).
 * Reintentar no sirve: el mensaje se rechaza (NACK sin requeue) y va directo a la DLQ.
 */
public class ErrorNoRecuperableException extends RuntimeException {
    public ErrorNoRecuperableException(String mensaje) { super(mensaje); }
}
