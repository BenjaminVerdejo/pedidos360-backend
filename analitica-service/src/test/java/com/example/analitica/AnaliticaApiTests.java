package com.example.analitica;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Seguridad de la API de analitica (Kafka no se conecta: listeners con auto-startup=false). */
@SpringBootTest
class AnaliticaApiTests {

    private static final SimpleGrantedAuthority SCOPE = new SimpleGrantedAuthority("SCOPE_Pedidos.Access");
    private static final SimpleGrantedAuthority ADMIN = new SimpleGrantedAuthority("ROLE_Admin");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private DefaultErrorHandler kafkaErrorHandler;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/analitica/resumen")).andExpect(status().isUnauthorized());
    }

    @Test
    void conScopeDevuelveElResumen() throws Exception {
        mvc.perform(get("/api/analitica/resumen").with(jwt().authorities(SCOPE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pedidosCreados").value(0))
                .andExpect(jsonPath("$.pedidosPorEstado.PENDIENTE").value(0))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void sinScopeDevuelve403() throws Exception {
        mvc.perform(get("/api/analitica/resumen").with(jwt().authorities(ADMIN)))
                .andExpect(status().isForbidden());
    }

    @Test
    void auditoriaExigeRolAdmin() throws Exception {
        mvc.perform(get("/api/analitica/auditoria").with(jwt().authorities(SCOPE)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/analitica/auditoria").with(jwt().authorities(SCOPE, ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void corsRechazaOrigenNoAutorizado() throws Exception {
        mvc.perform(options("/api/analitica/resumen").header("Origin", "https://sitio-atacante.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void elManejadorDeErroresDeKafkaEstaConfigurado() {
        assertNotNull(kafkaErrorHandler);
    }
}
