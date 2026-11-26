package com.example.kafkaadmin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

/** Modificar la configuracion de un topico existente (solo se cambian los campos enviados). */
public record ActualizarConfigRequest(
        @Min(-1) Long retencionMs,
        @Pattern(regexp = ReglasKafka.CLEANUP_POLICY, message = "cleanupPolicy debe ser delete o compact") String cleanupPolicy,
        @Min(1) @Max(3) Integer minInsyncReplicas,
        @Min(1024) @Max(10485760) Integer maxMessageBytes) {

    public boolean vacio() {
        return retencionMs == null && cleanupPolicy == null && minInsyncReplicas == null && maxMessageBytes == null;
    }
}
