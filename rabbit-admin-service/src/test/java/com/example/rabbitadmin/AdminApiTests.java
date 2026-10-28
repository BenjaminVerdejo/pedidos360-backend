package com.example.rabbitadmin;

import com.example.rabbitadmin.dto.ColaResponse;
import com.example.rabbitadmin.service.RabbitAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Seguridad + validacion de entrada de la API administradora (RabbitMQ simulado con un mock). */
@SpringBootTest
class AdminApiTests {

    private static final SimpleGrantedAuthority SCOPE = new SimpleGrantedAuthority("SCOPE_Pedidos.Access");
    private static final SimpleGrantedAuthority ADMIN = new SimpleGrantedAuthority("ROLE_Admin");

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private RabbitAdminService service;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private MockHttpServletRequestBuilder postJson(String url, String json) {
        return post(url).with(jwt().authorities(SCOPE, ADMIN)).contentType(MediaType.APPLICATION_JSON).content(json);
    }

    // ---------- Seguridad ----------

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(post("/api/admin/queues").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sinRolAdminDevuelve403() throws Exception {
        mvc.perform(post("/api/admin/queues").with(jwt().authorities(SCOPE))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"x.queue\"}"))
                .andExpect(status().isForbidden());
    }

    // ---------- Colas ----------

    @Test
    void crearColaValidaDevuelve201() throws Exception {
        when(service.crearCola(any())).thenReturn(new ColaResponse("facturas.queue", 0, 0));
        mvc.perform(postJson("/api/admin/queues", "{\"nombre\":\"facturas.queue\",\"ttlMs\":60000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("facturas.queue"));
    }

    @Test
    void crearColaConNombreVacioDevuelve400() throws Exception {
        mvc.perform(postJson("/api/admin/queues", "{\"nombre\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nombre").exists());
        verifyNoInteractions(service);
    }

    @Test
    void crearColaSinNombreDevuelve400() throws Exception {
        mvc.perform(postJson("/api/admin/queues", "{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void crearColaConEspaciosEnElNombreDevuelve400() throws Exception {
        mvc.perform(postJson("/api/admin/queues", "{\"nombre\":\"mi cola\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void crearColaConPrefijoReservadoDevuelve400() throws Exception {
        mvc.perform(postJson("/api/admin/queues", "{\"nombre\":\"amq.hack\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nombreNoReservado").exists());
        verifyNoInteractions(service);
    }

    @Test
    void crearColaConTtlNegativoDevuelve400() throws Exception {
        mvc.perform(postJson("/api/admin/queues", "{\"nombre\":\"a.queue\",\"ttlMs\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.ttlMs").exists());
        verifyNoInteractions(service);
    }

    @Test
    void routingKeyDeDlqSinExchangeDevuelve400() throws Exception {
        mvc.perform(postJson("/api/admin/queues", "{\"nombre\":\"a.queue\",\"deadLetterRoutingKey\":\"a.dlq\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.deadLetterConsistente").exists());
        verifyNoInteractions(service);
    }

    @Test
    void eliminarColaConNombreInvalidoDevuelve400() throws Exception {
        mvc.perform(delete("/api/admin/queues/{nombre}", "mala cola").with(jwt().authorities(SCOPE, ADMIN)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void eliminarColaValidaDevuelve204() throws Exception {
        mvc.perform(delete("/api/admin/queues/facturas.queue").with(jwt().authorities(SCOPE, ADMIN)))
                .andExpect(status().isNoContent());
    }

    // ---------- Exchanges ----------

    @Test
    void crearExchangeSinTipoDevuelve400() throws Exception {
        mvc.perform(postJson("/api/admin/exchanges", "{\"nombre\":\"facturas.exchange\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.tipo").exists());
        verifyNoInteractions(service);
    }

    @Test
    void crearExchangeConTipoInexistenteDevuelve400() throws Exception {
        mvc.perform(postJson("/api/admin/exchanges", "{\"nombre\":\"facturas.exchange\",\"tipo\":\"RARO\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    // ---------- Bindings ----------

    @Test
    void crearBindingSinColaDevuelve400() throws Exception {
        mvc.perform(postJson("/api/admin/bindings", "{\"exchange\":\"pedidos.exchange\",\"routingKey\":\"pedido.#\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.cola").exists());
        verifyNoInteractions(service);
    }

    @Test
    void eliminarBindingSinExchangeDevuelve400() throws Exception {
        mvc.perform(delete("/api/admin/bindings").param("cola", "a.queue").with(jwt().authorities(SCOPE, ADMIN)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
