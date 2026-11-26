package com.example.kafkaadmin.dto;

import java.util.List;

/**
 * Lag de un consumer group: cuantos mensajes faltan por procesar.
 * lag = offset final de la particion (log-end-offset) - offset confirmado por el grupo.
 */
public record LagResponse(String grupo, long lagTotal, List<LagParticion> particiones) {

    public record LagParticion(String topico, int particion, long offsetConsumido, long offsetFinal, long lag) {
    }
}
