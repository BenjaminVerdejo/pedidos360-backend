package com.example.kafkaadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Reposicionar un consumer group (el grupo debe estar sin consumidores activos). */
public record ResetOffsetsRequest(
        @NotBlank @Pattern(regexp = ReglasKafka.NOMBRE_TOPICO) String topico,
        @NotBlank @Pattern(regexp = ReglasKafka.DESTINO_OFFSET, message = "destino debe ser earliest o latest") String destino) {
}
