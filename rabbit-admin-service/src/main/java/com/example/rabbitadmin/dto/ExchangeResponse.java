package com.example.rabbitadmin.dto;

public record ExchangeResponse(String nombre, TipoExchange tipo, boolean durable, boolean autoDelete) {}
