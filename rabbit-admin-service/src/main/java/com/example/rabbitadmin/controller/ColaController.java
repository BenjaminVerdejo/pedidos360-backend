package com.example.rabbitadmin.controller;

import com.example.rabbitadmin.dto.ColaResponse;
import com.example.rabbitadmin.dto.CrearColaRequest;
import com.example.rabbitadmin.dto.ReglasNombre;
import com.example.rabbitadmin.service.RabbitAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * Recurso COLAS.
 *   POST   /api/admin/queues                 -> crea una cola           (201)
 *   GET    /api/admin/queues/{nombre}        -> estado de la cola       (200)
 *   DELETE /api/admin/queues/{nombre}        -> elimina la cola         (204)
 *          ?soloSiVacia=true                    no borra si tiene mensajes (409)
 */
@RestController
@RequestMapping("/api/admin/queues")
public class ColaController {

    private final RabbitAdminService service;

    public ColaController(RabbitAdminService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ColaResponse> crear(@Valid @RequestBody CrearColaRequest request) {
        ColaResponse cola = service.crearCola(request);
        return ResponseEntity.created(URI.create("/api/admin/queues/" + cola.nombre())).body(cola);
    }

    @GetMapping("/{nombre}")
    public ColaResponse obtener(
            @PathVariable @Size(max = ReglasNombre.LARGO_MAX)
            @Pattern(regexp = ReglasNombre.PATRON, message = "el nombre " + ReglasNombre.MENSAJE_PATRON)
            String nombre) {
        return service.obtenerCola(nombre);
    }

    @DeleteMapping("/{nombre}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(
            @PathVariable @Size(max = ReglasNombre.LARGO_MAX)
            @Pattern(regexp = ReglasNombre.PATRON, message = "el nombre " + ReglasNombre.MENSAJE_PATRON)
            String nombre,
            @RequestParam(defaultValue = "false") boolean soloSiVacia) {
        service.eliminarCola(nombre, soloSiVacia);
    }
}
