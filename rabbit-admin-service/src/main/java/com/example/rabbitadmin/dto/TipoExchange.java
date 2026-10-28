package com.example.rabbitadmin.dto;

/** Tipos de exchange soportados. Cualquier otro valor se rechaza con 400. */
public enum TipoExchange {
    DIRECT, TOPIC, FANOUT, HEADERS
}
