package com.example.kafkaadmin.controller;

import com.example.kafkaadmin.dto.ConsumerGroupResponse;
import com.example.kafkaadmin.dto.LagResponse;
import com.example.kafkaadmin.dto.ResetOffsetsRequest;
import com.example.kafkaadmin.service.KafkaAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Supervision y gestion de consumer groups: estado, miembros, lag y reposicion de offsets. */
@RestController
@RequestMapping("/api/kafka/consumer-groups")
@Validated
public class ConsumerGroupController {

    private static final String NOMBRE_GRUPO = "^[a-zA-Z0-9._-]{1,200}$";

    private final KafkaAdminService service;

    public ConsumerGroupController(KafkaAdminService service) {
        this.service = service;
    }

    @GetMapping
    public List<ConsumerGroupResponse> listar() {
        return service.listarConsumerGroups();
    }

    @GetMapping("/{grupo}")
    public ConsumerGroupResponse obtener(@PathVariable @Pattern(regexp = NOMBRE_GRUPO) String grupo) {
        return service.obtenerConsumerGroup(grupo);
    }

    @GetMapping("/{grupo}/lag")
    public LagResponse lag(@PathVariable @Pattern(regexp = NOMBRE_GRUPO) String grupo) {
        return service.calcularLag(grupo);
    }

    @PostMapping("/{grupo}/reset-offsets")
    public LagResponse resetearOffsets(@PathVariable @Pattern(regexp = NOMBRE_GRUPO) String grupo,
                                       @Valid @RequestBody ResetOffsetsRequest req, Authentication auth) {
        return service.resetearOffsets(grupo, req, auth.getName());
    }
}
