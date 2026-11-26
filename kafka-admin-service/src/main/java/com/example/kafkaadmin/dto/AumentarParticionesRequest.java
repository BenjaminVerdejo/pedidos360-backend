package com.example.kafkaadmin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Kafka solo permite AUMENTAR particiones (nunca disminuir). */
public record AumentarParticionesRequest(@NotNull @Min(2) @Max(50) Integer total) {
}
