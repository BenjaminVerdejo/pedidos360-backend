package com.example.kafkaadmin.dto;

/** Expresiones regulares compartidas por los DTO de entrada. */
public final class ReglasKafka {

    /** Nombres validos de topico en Kafka: letras, numeros, punto, guion y guion bajo (max 249). */
    public static final String NOMBRE_TOPICO = "^[a-zA-Z0-9._-]{1,249}$";
    public static final String CLEANUP_POLICY = "^(delete|compact|compact,delete|delete,compact)$";
    public static final String DESTINO_OFFSET = "^(earliest|latest)$";

    private ReglasKafka() {
    }
}
