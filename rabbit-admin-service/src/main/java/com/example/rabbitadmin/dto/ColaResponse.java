package com.example.rabbitadmin.dto;

/** Estado de una cola. */
public record ColaResponse(String nombre, long mensajes, int consumidores) {}
