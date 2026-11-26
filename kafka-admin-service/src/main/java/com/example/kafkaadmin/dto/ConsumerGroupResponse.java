package com.example.kafkaadmin.dto;

import java.util.List;

/** Consumer group: estado (Stable, Empty, PreparingRebalance...), miembros y asignacion de particiones. */
public record ConsumerGroupResponse(String grupo, String estado, Integer coordinador, String asignador,
                                    List<MiembroInfo> miembros) {

    public record MiembroInfo(String consumidorId, String clientId, String host, List<String> particiones) {
    }
}
