package com.example.kafkaadmin.dto;

import java.util.List;

/** Estado de una particion: broker lider, replicas asignadas y replicas sincronizadas (ISR). */
public record ParticionInfo(int particion, Integer lider, List<Integer> replicas, List<Integer> isr) {
}
