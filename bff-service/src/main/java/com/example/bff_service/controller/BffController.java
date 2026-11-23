package com.example.bff_service.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.regex.Pattern;

/**
 * BFF: recibe las peticiones del frontend (ya validadas con el JWT en SecurityConfig)
 * y las reenvia al pedidos-service con el mismo token.
 */
@RestController
public class BffController {

    private static final Logger log = LoggerFactory.getLogger(BffController.class);

    /**
     * Lista blanca de rutas que el BFF reenvia. Cualquier otra ruta responde 404 sin llegar a
     * pedidos-service (reduce la superficie de ataque y evita reenviar rutas manipuladas).
     */
    private static final Pattern RUTAS_PERMITIDAS =
            Pattern.compile("^/api/(test|pedidos(/\\d{1,18}(/estado)?)?)$");

    /** Tiempos maximos de espera: si pedidos-service no responde, no se bloquean hilos indefinidamente. */
    private static final Duration TIMEOUT_CONEXION = Duration.ofSeconds(3);
    private static final Duration TIMEOUT_LECTURA = Duration.ofSeconds(10);

    private final String pedidosServiceUrl;
    private final RestTemplate restTemplate;

    public BffController(@Value("${pedidos.service.url}") String pedidosServiceUrl) {
        this.pedidosServiceUrl = pedidosServiceUrl;

        // HTTP/1.1 y cliente del JDK: soporta PATCH (el de por defecto no)
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(TIMEOUT_CONEXION)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(TIMEOUT_LECTURA);
        this.restTemplate = new RestTemplate(factory);

        // no lanzar excepcion con 4xx/5xx: se devuelve el mismo status al frontend
        this.restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }
        });
    }

    @RequestMapping(value = "/api/**", method = {
            RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
            RequestMethod.PATCH, RequestMethod.DELETE})
    public ResponseEntity<String> reenviar(HttpServletRequest request,
                                           @RequestBody(required = false) String body,
                                           Authentication auth) {

        if (!RUTAS_PERMITIDAS.matcher(request.getRequestURI()).matches()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"not_found\",\"mensaje\":\"Recurso no disponible\"}");
        }

        String query = request.getQueryString();
        URI destino = URI.create(pedidosServiceUrl + request.getRequestURI() + (query != null ? "?" + query : ""));

        HttpHeaders headers = new HttpHeaders();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            headers.setBearerAuth(jwtAuth.getToken().getTokenValue());
        }
        if (request.getContentType() != null) {
            headers.set(HttpHeaders.CONTENT_TYPE, request.getContentType());
        }

        try {
            ResponseEntity<String> respuesta = restTemplate.exchange(
                    destino, HttpMethod.valueOf(request.getMethod()), new HttpEntity<>(body, headers), String.class);

            ResponseEntity.BodyBuilder salida = ResponseEntity.status(respuesta.getStatusCode());
            MediaType contentType = respuesta.getHeaders().getContentType();
            if (contentType != null) {
                salida.contentType(contentType);
            }
            return salida.body(respuesta.getBody());

        } catch (ResourceAccessException e) {
            log.error("No se pudo conectar con pedidos-service en {}: {}", pedidosServiceUrl, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"bad_gateway\",\"mensaje\":\"No se pudo conectar con pedidos-service\"}");
        }
    }
}
