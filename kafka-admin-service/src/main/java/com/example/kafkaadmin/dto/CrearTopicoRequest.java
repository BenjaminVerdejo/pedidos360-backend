package com.example.kafkaadmin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Crear un topico.
 * @param retencionMs -1 = retener para siempre; null = valor por defecto del broker
 */
public record CrearTopicoRequest(
        @NotBlank @Pattern(regexp = ReglasKafka.NOMBRE_TOPICO, message = "nombre de topico invalido") String nombre,
        @NotNull @Min(1) @Max(50) Integer particiones,
        @NotNull @Min(1) @Max(3) Short replicas,
        @Min(-1) Long retencionMs,
        @Pattern(regexp = ReglasKafka.CLEANUP_POLICY, message = "cleanupPolicy debe ser delete o compact") String cleanupPolicy,
        @Min(1) @Max(3) Integer minInsyncReplicas) {
}
