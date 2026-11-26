package com.example.kafkaadmin.controller;

import com.example.kafkaadmin.dto.ActualizarConfigRequest;
import com.example.kafkaadmin.dto.AumentarParticionesRequest;
import com.example.kafkaadmin.dto.CrearTopicoRequest;
import com.example.kafkaadmin.dto.ReglasKafka;
import com.example.kafkaadmin.dto.TopicoResponse;
import com.example.kafkaadmin.service.KafkaAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Administracion de topicos: crear, listar, describir, modificar configuracion, particiones y eliminar. */
@RestController
@RequestMapping("/api/kafka/topicos")
@Validated
public class TopicoController {

    private final KafkaAdminService service;

    public TopicoController(KafkaAdminService service) {
        this.service = service;
    }

    @GetMapping
    public List<TopicoResponse> listar() {
        return service.listarTopicos();
    }

    @GetMapping("/{nombre}")
    public TopicoResponse obtener(@PathVariable @Pattern(regexp = ReglasKafka.NOMBRE_TOPICO) String nombre) {
        return service.obtenerTopico(nombre);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TopicoResponse crear(@Valid @RequestBody CrearTopicoRequest req, Authentication auth) {
        return service.crearTopico(req, auth.getName());
    }

    @PutMapping("/{nombre}/configuracion")
    public TopicoResponse actualizarConfiguracion(@PathVariable @Pattern(regexp = ReglasKafka.NOMBRE_TOPICO) String nombre,
                                                  @Valid @RequestBody ActualizarConfigRequest req, Authentication auth) {
        return service.actualizarConfiguracion(nombre, req, auth.getName());
    }

    @PostMapping("/{nombre}/particiones")
    public TopicoResponse aumentarParticiones(@PathVariable @Pattern(regexp = ReglasKafka.NOMBRE_TOPICO) String nombre,
                                              @Valid @RequestBody AumentarParticionesRequest req, Authentication auth) {
        return service.aumentarParticiones(nombre, req.total(), auth.getName());
    }

    @DeleteMapping("/{nombre}")
    public ResponseEntity<Void> eliminar(@PathVariable @Pattern(regexp = ReglasKafka.NOMBRE_TOPICO) String nombre,
                                         Authentication auth) {
        service.eliminarTopico(nombre, auth.getName());
        return ResponseEntity.noContent().build();
    }
}
