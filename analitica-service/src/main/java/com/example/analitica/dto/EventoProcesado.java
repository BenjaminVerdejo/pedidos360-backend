package com.example.analitica.dto;

/** Resumen de un evento consumido: que paso, de que topico/particion/offset vino y cuando. */
public record EventoProcesado(
        String tipo,
        Long pedidoId,
        String detalle,
        String topico,
        int particion,
        long offset,
        String timestamp) {
}
