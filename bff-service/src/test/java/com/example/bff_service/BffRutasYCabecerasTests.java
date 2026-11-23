package com.example.bff_service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas de seguridad del BFF: lista blanca de rutas, cabeceras HTTP y CORS (EP5). */
@SpringBootTest
class BffRutasYCabecerasTests {

    private static final SimpleGrantedAuthority SCOPE = new SimpleGrantedAuthority("SCOPE_Pedidos.Access");

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    @DisplayName("SEG-11 el BFF no reenvia rutas fuera de la lista blanca")
    void rutaNoPermitidaDevuelve404() throws Exception {
        mvc.perform(get("/api/admin/usuarios").with(jwt().authorities(SCOPE)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("SEG-11 id no numerico no se reenvia")
    void idNoNumericoDevuelve404() throws Exception {
        mvc.perform(get("/api/pedidos/abc").with(jwt().authorities(SCOPE)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("SEG-11 las rutas permitidas si se reenvian (502: pedidos-service caido en el test)")
    void rutaPermitidaSeReenvia() throws Exception {
        mvc.perform(patch("/api/pedidos/1/estado").with(jwt().authorities(SCOPE))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"ENVIADO\"}"))
                .andExpect(status().isBadGateway());
    }

    @Test
    @DisplayName("SEG-09 el BFF responde con cabeceras de seguridad")
    void cabecerasDeSeguridad() throws Exception {
        mvc.perform(get("/api/pedidos/abc").secure(true).with(jwt().authorities(SCOPE)))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")))
                .andExpect(header().string("Strict-Transport-Security", containsString("max-age")));
    }

    @Test
    @DisplayName("SEG-10 CORS rechaza origenes no autorizados en el BFF")
    void corsRechazaOrigenNoAutorizado() throws Exception {
        mvc.perform(options("/api/pedidos").header("Origin", "https://sitio-atacante.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
