package com.example.demo.exception;

/** Regla de negocio: un pedido ENTREGADO o CANCELADO no puede volver a cambiar de estado. */
public class TransicionEstadoInvalidaException extends RuntimeException {
    public TransicionEstadoInvalidaException(String mensaje) {
        super(mensaje);
    }
}
