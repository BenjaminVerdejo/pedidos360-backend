package com.example.rabbitadmin.controller;

import com.example.rabbitadmin.dto.BindingResponse;
import com.example.rabbitadmin.dto.CrearBindingRequest;
import com.example.rabbitadmin.dto.ReglasNombre;
import com.example.rabbitadmin.service.RabbitAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Recurso BINDINGS (exchange -> cola).
 *   POST   /api/admin/bindings                                       -> crea binding    (201)
 *   DELETE /api/admin/bindings?exchange=X&cola=Y&routingKey=Z        -> elimina binding (204)
 */
@RestController
@RequestMapping("/api/admin/bindings")
public class BindingController {

    private final RabbitAdminService service;

    public BindingController(RabbitAdminService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BindingResponse crear(@Valid @RequestBody CrearBindingRequest request) {
        return service.crearBinding(request);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(
            @RequestParam @NotBlank(message = "el exchange es obligatorio") @Size(max = ReglasNombre.LARGO_MAX)
            @Pattern(regexp = ReglasNombre.PATRON, message = "exchange " + ReglasNombre.MENSAJE_PATRON)
            String exchange,
            @RequestParam @NotBlank(message = "la cola es obligatoria") @Size(max = ReglasNombre.LARGO_MAX)
            @Pattern(regexp = ReglasNombre.PATRON, message = "cola " + ReglasNombre.MENSAJE_PATRON)
            String cola,
            @RequestParam(defaultValue = "") @Size(max = ReglasNombre.LARGO_MAX)
            @Pattern(regexp = "^[A-Za-z0-9._*#-]*$", message = "routingKey tiene caracteres no permitidos")
            String routingKey) {
        service.eliminarBinding(exchange, cola, routingKey);
    }
}
