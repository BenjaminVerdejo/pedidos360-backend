package com.example.analitica.exception;

/** El evento recibido no cumple el contrato (campos obligatorios ausentes o valores invalidos). */
public class EventoInvalidoException extends RuntimeException {
    public EventoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
