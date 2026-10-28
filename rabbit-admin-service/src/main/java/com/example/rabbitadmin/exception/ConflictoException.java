package com.example.rabbitadmin.exception;

/** 409: ya existe, esta protegido o tiene mensajes pendientes. */
public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) { super(mensaje); }
}
