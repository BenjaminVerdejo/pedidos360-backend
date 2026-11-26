package com.example.analitica.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Metricas en tiempo real que muestra la pantalla "Analitica en vivo" del frontend. */
public record ResumenAnalitica(
        long pedidosCreados,
        long pedidosEntregados,
        long cambiosDeEstado,
        BigDecimal ventasTotales,
        BigDecimal ventasEntregadas,
        BigDecimal ticketPromedio,
        Map<String, Long> pedidosPorEstado,
        long eventosDuplicadosDescartados,
        List<EventoProcesado> ultimosEventos,
        String actualizadoEn) {
}
