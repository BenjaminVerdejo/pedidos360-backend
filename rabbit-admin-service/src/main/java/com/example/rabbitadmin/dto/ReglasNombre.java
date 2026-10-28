package com.example.rabbitadmin.dto;

/** Reglas de nombres compartidas por todos los DTO (un solo lugar). */
public final class ReglasNombre {

    /** Letras, numeros, punto, guion y guion bajo. Sin espacios. */
    public static final String PATRON = "^[A-Za-z0-9._-]+$";
    public static final String MENSAJE_PATRON =
            "solo puede contener letras, numeros, '.', '-' y '_' (sin espacios)";
    public static final int LARGO_MAX = 255;
    /** RabbitMQ reserva el prefijo amq. para sus objetos internos. */
    public static final String PREFIJO_RESERVADO = "amq.";

    private ReglasNombre() {}

    public static boolean noReservado(String nombre) {
        return nombre == null || !nombre.toLowerCase().startsWith(PREFIJO_RESERVADO);
    }
}
