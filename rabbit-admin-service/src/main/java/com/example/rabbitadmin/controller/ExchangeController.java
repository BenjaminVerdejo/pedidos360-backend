package com.example.rabbitadmin.controller;

import com.example.rabbitadmin.dto.CrearExchangeRequest;
import com.example.rabbitadmin.dto.ExchangeResponse;
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
 * Recurso EXCHANGES.
 *   POST   /api/admin/exchanges              -> crea un exchange   (201)
 *   DELETE /api/admin/exchanges/{nombre}     -> elimina exchange   (204)
 */
@RestController
@RequestMapping("/api/admin/exchanges")
public class ExchangeController {

    private final RabbitAdminService service;

    public ExchangeController(RabbitAdminService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ExchangeResponse> crear(@Valid @RequestBody CrearExchangeRequest request) {
        ExchangeResponse ex = service.crearExchange(request);
        return ResponseEntity.created(URI.create("/api/admin/exchanges/" + ex.nombre())).body(ex);
    }

    @DeleteMapping("/{nombre}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(
            @PathVariable @Size(max = ReglasNombre.LARGO_MAX)
            @Pattern(regexp = ReglasNombre.PATRON, message = "el nombre " + ReglasNombre.MENSAJE_PATRON)
            String nombre) {
        service.eliminarExchange(nombre);
    }
}
