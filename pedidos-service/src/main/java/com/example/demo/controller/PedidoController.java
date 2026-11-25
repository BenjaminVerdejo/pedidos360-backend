package com.example.demo.controller;

import com.example.demo.dto.CambioEstadoRequest;
import com.example.demo.dto.PedidoRequest;
import com.example.demo.model.Pedido;
import com.example.demo.service.PedidoService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
@PreAuthorize("hasAuthority('SCOPE_Pedidos.Access')")
public class PedidoController {

    /** Logger de auditoria: quien hizo que operacion sensible (trazabilidad, ISO 27001 A.8.15). */
    private static final Logger auditoria = LoggerFactory.getLogger("AUDITORIA");

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @GetMapping
    public List<Pedido> listar() {
        return pedidoService.listar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pedido> obtener(@PathVariable Long id) {
        return pedidoService.obtener(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Recibe un DTO validado (no la entidad JPA) para evitar asignacion masiva. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Pedido crear(@Valid @RequestBody PedidoRequest pedido, Authentication auth) {
        Pedido creado = pedidoService.crear(pedido);
        auditoria.info("accion=CREAR_PEDIDO pedido={} usuario={}", creado.getId(), usuario(auth));
        return creado;
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Pedido> cambiarEstado(@PathVariable Long id,
                                                @Valid @RequestBody CambioEstadoRequest body,
                                                Authentication auth) {
        return pedidoService.cambiarEstado(id, body.estado())
                .map(p -> {
                    auditoria.info("accion=CAMBIAR_ESTADO pedido={} estado={} usuario={}", id, body.estado(), usuario(auth));
                    return ResponseEntity.ok(p);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // el @PreAuthorize del metodo reemplaza al de la clase, por eso lleva las dos condiciones
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_Pedidos.Access') and hasRole('Admin')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, Authentication auth) {
        boolean eliminado = pedidoService.eliminar(id);
        if (eliminado) {
            auditoria.info("accion=ELIMINAR_PEDIDO pedido={} usuario={}", id, usuario(auth));
        }
        return eliminado ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    private static String usuario(Authentication auth) {
        return auth != null ? auth.getName() : "anonimo";
    }
}
