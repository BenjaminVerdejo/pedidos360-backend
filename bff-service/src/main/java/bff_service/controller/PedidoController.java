package bff_service.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

@RestController
public class PedidoController {

    private final RestClient restClient;

    public PedidoController(@Value("${pedidos.service.url}") String pedidosServiceUrl) {
        this.restClient = RestClient.create(pedidosServiceUrl);
    }

    @GetMapping("/api/pedidos")
    public String getPedidos(@AuthenticationPrincipal Jwt jwt) {
        return restClient.get()
                .uri("/api/pedidos")
                .header("Authorization", "Bearer " + jwt.getTokenValue())
                .retrieve()
                .body(String.class);
    }

    @GetMapping("/api/pedidos/{id}")
    public ResponseEntity<String> getPedido(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return restClient.get()
                .uri("/api/pedidos/{id}", id)
                .header("Authorization", "Bearer " + jwt.getTokenValue())
                .retrieve()
                .toEntity(String.class);
    }

    @PostMapping("/api/pedidos")
    public ResponseEntity<String> crearPedido(@RequestBody String body, @AuthenticationPrincipal Jwt jwt) {
        return restClient.post()
                .uri("/api/pedidos")
                .header("Authorization", "Bearer " + jwt.getTokenValue())
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .toEntity(String.class);
    }

    @PatchMapping("/api/pedidos/{id}/estado")
    public ResponseEntity<String> cambiarEstado(
            @PathVariable Long id,
            @RequestParam String nuevoEstado,
            @AuthenticationPrincipal Jwt jwt) {
        return restClient.patch()
                .uri("/api/pedidos/{id}/estado?nuevoEstado={estado}", id, nuevoEstado)
                .header("Authorization", "Bearer " + jwt.getTokenValue())
                .retrieve()
                .toEntity(String.class);
    }

    @DeleteMapping("/api/pedidos/{id}")
    public ResponseEntity<Void> eliminarPedido(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        restClient.delete()
                .uri("/api/pedidos/{id}", id)
                .header("Authorization", "Bearer " + jwt.getTokenValue())
                .retrieve()
                .toBodilessEntity();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}