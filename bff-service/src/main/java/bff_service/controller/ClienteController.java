package bff_service.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
public class ClienteController {

    private final RestClient restClient;

    public ClienteController(@Value("${pedidos.service.url}") String pedidosServiceUrl) {
        this.restClient = RestClient.create(pedidosServiceUrl);
    }

    @GetMapping("/api/clientes")
    public String getClientes(@AuthenticationPrincipal Jwt jwt) {
        return restClient.get()
                .uri("/api/clientes")
                .header("Authorization", "Bearer " + jwt.getTokenValue())
                .retrieve()
                .body(String.class);
    }
}