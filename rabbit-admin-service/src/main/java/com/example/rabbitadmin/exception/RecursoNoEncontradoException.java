package com.example.rabbitadmin.exception;

/** 404: la cola / exchange indicado no existe. */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) { super(mensaje); }
}
