package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class SecurityTests {

    private static final SimpleGrantedAuthority SCOPE = new SimpleGrantedAuthority("SCOPE_Pedidos.Access");
    private static final SimpleGrantedAuthority ADMIN = new SimpleGrantedAuthority("ROLE_Admin");

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/pedidos")).andExpect(status().isUnauthorized());
    }

    @Test
    void conTokenYScopeDevuelve200() throws Exception {
        mvc.perform(get("/api/pedidos").with(jwt().authorities(SCOPE)))
                .andExpect(status().isOk());
    }

    @Test
    void tokenSinScopeDevuelve403() throws Exception {
        mvc.perform(get("/api/pedidos").with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void borrarSinRolAdminDevuelve403() throws Exception {
        mvc.perform(delete("/api/pedidos/1").with(jwt().authorities(SCOPE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void borrarConRolAdminNoEsProhibido() throws Exception {
        // 404 porque el pedido no existe, lo importante es que NO sea 403
        mvc.perform(delete("/api/pedidos/999").with(jwt().authorities(SCOPE, ADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    void dosPedidosConElMismoClienteSePuedenCrear() throws Exception {
        String json = "{\"cliente\":{\"nombre\":\"Ana\",\"email\":\"ana@test.cl\"},\"descripcion\":\"Pedido\",\"total\":1000}";

        mvc.perform(post("/api/pedidos").with(jwt().authorities(SCOPE))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());

        // mismo email: antes fallaba con 500 por el email unico
        mvc.perform(post("/api/pedidos").with(jwt().authorities(SCOPE))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
    }
}
