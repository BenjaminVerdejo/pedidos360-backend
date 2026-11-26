package com.example.kafkaadmin.exception;

/** El cluster no respondio a tiempo o rechazo la operacion por un problema de infraestructura. */
public class OperacionKafkaException extends RuntimeException {
    public OperacionKafkaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
