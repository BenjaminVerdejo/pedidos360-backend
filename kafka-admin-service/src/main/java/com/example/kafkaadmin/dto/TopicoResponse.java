package com.example.kafkaadmin.dto;

import java.util.List;
import java.util.Map;

/** Descripcion de un topico con sus particiones y su configuracion relevante. */
public record TopicoResponse(
        String nombre,
        int particiones,
        int replicas,
        boolean protegido,
        Map<String, String> configuracion,
        List<ParticionInfo> detalleParticiones) {
}
