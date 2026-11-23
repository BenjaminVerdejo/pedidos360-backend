package com.example.demo.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas automatizadas de seguridad de la API (EP5).
 * Cada prueba corresponde a un caso SEG-xx del informe y verifica un control concreto:
 * validacion de entrada, asignacion masiva, manejo seguro de errores, cabeceras HTTP y CORS.
 */
@SpringBootTest
@ActiveProfiles("test")
class ApiValidacionSeguridadTests {

    private static final SimpleGrantedAuthority SCOPE = new SimpleGrantedAuthority("SCOPE_Pedidos.Access");
    private static final SimpleGrantedAuthority ADMIN = new SimpleGrantedAuthority("ROLE_Admin");
    private static final String URL = "/api/pedidos";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static String pedidoJson(String nombre, String email, String descripcion, String total) {
        return "{\"cliente\":{\"nombre\":\"" + nombre + "\",\"email\":\"" + email + "\"},"
                + "\"descripcion\":\"" + descripcion + "\",\"total\":" + total + "}";
    }

    private MvcResult crearPedido(String json) throws Exception {
        return mvc.perform(post(URL).with(jwt().authorities(SCOPE))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn();
    }

    // ---------- Validacion de entrada (OWASP A03) ----------

    @Test
    @DisplayName("SEG-04 email con formato invalido responde 400 indicando el campo")
    void emailInvalidoDevuelve400() throws Exception {
        mvc.perform(post(URL).with(jwt().authorities(SCOPE)).contentType(MediaType.APPLICATION_JSON)
                        .content(pedidoJson("Ana", "no-es-un-email", "Pedido", "1000")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validacion"))
                .andExpect(jsonPath("$.campos['cliente.email']").exists());
    }

    @Test
    @DisplayName("SEG-04 total negativo responde 400")
    void totalNegativoDevuelve400() throws Exception {
        mvc.perform(post(URL).with(jwt().authorities(SCOPE)).contentType(MediaType.APPLICATION_JSON)
                        .content(pedidoJson("Ana", "ana@test.cl", "Pedido", "-50")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.total").exists());
    }

    @Test
    @DisplayName("SEG-04 descripcion de mas de 255 caracteres responde 400")
    void descripcionDemasiadoLargaDevuelve400() throws Exception {
        mvc.perform(post(URL).with(jwt().authorities(SCOPE)).contentType(MediaType.APPLICATION_JSON)
                        .content(pedidoJson("Ana", "ana@test.cl", "x".repeat(300), "1000")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.descripcion").exists());
    }

    @Test
    @DisplayName("SEG-05 estado fuera del catalogo responde 400")
    void estadoInvalidoDevuelve400() throws Exception {
        mvc.perform(patch(URL + "/1/estado").with(jwt().authorities(SCOPE))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"HACKEADO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("json_invalido"));
    }

    @Test
    @DisplayName("SEG-06 inyeccion SQL en el parametro de ruta responde 400 sin llegar a la BD")
    void inyeccionSqlEnIdDevuelve400() throws Exception {
        mvc.perform(get(URL + "/{id}", "1 OR 1=1").with(jwt().authorities(SCOPE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("parametro_invalido"));
    }

    // ---------- Asignacion masiva (CWE-915) ----------

    @Test
    @DisplayName("SEG-03 el cliente no puede fijar id, estado ni fecha al crear un pedido")
    void asignacionMasivaEsIgnorada() throws Exception {
        String json = "{\"id\":999,\"estado\":\"ENTREGADO\",\"fecha\":\"2000-01-01T00:00:00\","
                + "\"cliente\":{\"id\":1,\"nombre\":\"Eva\",\"email\":\"eva@test.cl\"},"
                + "\"descripcion\":\"Pedido\",\"total\":1500}";
        MvcResult r = crearPedido(json);
        String cuerpo = r.getResponse().getContentAsString();
        Integer id = JsonPath.read(cuerpo, "$.id");
        String estado = JsonPath.read(cuerpo, "$.estado");
        String fecha = JsonPath.read(cuerpo, "$.fecha");
        org.junit.jupiter.api.Assertions.assertNotEquals(999, id);
        org.junit.jupiter.api.Assertions.assertEquals("PENDIENTE", estado);
        org.junit.jupiter.api.Assertions.assertFalse(fecha.startsWith("2000"));
    }

    // ---------- Reglas de negocio ----------

    @Test
    @DisplayName("CP-05 un pedido ENTREGADO no puede volver a cambiar de estado (409)")
    void pedidoEntregadoNoCambiaDeEstado() throws Exception {
        Integer id = JsonPath.read(crearPedido(pedidoJson("Luis", "luis@test.cl", "Pedido", "2000"))
                .getResponse().getContentAsString(), "$.id");

        mvc.perform(patch(URL + "/" + id + "/estado").with(jwt().authorities(SCOPE))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"ENTREGADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENTREGADO"));

        mvc.perform(patch(URL + "/" + id + "/estado").with(jwt().authorities(SCOPE))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"ENVIADO\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("transicion_invalida"));
    }

    // ---------- Manejo seguro de errores (CWE-209) ----------

    @Test
    @DisplayName("SEG-07 JSON mal formado responde 400 sin trazas ni nombres de clases")
    void jsonMalFormadoNoExponeDetallesInternos() throws Exception {
        mvc.perform(post(URL).with(jwt().authorities(SCOPE))
                        .contentType(MediaType.APPLICATION_JSON).content("{malo"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("com.example"))))
                .andExpect(content().string(not(containsString("jackson"))));
    }

    @Test
    @DisplayName("SEG-07 ruta inexistente responde 404 controlado (no 500)")
    void rutaInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/api/no-existe").with(jwt().authorities(SCOPE)))
                .andExpect(status().isNotFound());
    }

    // ---------- XSS almacenado / cabeceras de seguridad (OWASP A05) ----------

    @Test
    @DisplayName("SEG-08 un payload XSS se devuelve como JSON con nosniff (el navegador no lo ejecuta)")
    void payloadXssSeDevuelveComoJson() throws Exception {
        mvc.perform(post(URL).with(jwt().authorities(SCOPE)).contentType(MediaType.APPLICATION_JSON)
                        .content(pedidoJson("Mia", "mia@test.cl", "<script>alert(1)</script>", "100")))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/json")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    @DisplayName("SEG-09 las respuestas incluyen las cabeceras de seguridad HTTP")
    void respuestasIncluyenCabecerasDeSeguridad() throws Exception {
        mvc.perform(get(URL).secure(true).with(jwt().authorities(SCOPE)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'none'")))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Strict-Transport-Security", containsString("max-age=31536000")))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(header().string("Permissions-Policy", notNullValue()));
    }

    // ---------- CORS (OWASP A05) ----------

    @Test
    @DisplayName("SEG-10 CORS rechaza origenes no autorizados")
    void corsRechazaOrigenNoAutorizado() throws Exception {
        mvc.perform(options(URL).header("Origin", "https://sitio-atacante.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("SEG-10 CORS permite el origen del frontend")
    void corsPermiteOrigenDelFrontend() throws Exception {
        mvc.perform(options(URL).header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }

    // ---------- Control de acceso (OWASP A01) ----------

    @Test
    @DisplayName("SEG-02 rol Admin sin el scope Pedidos.Access no puede listar (403)")
    void rolAdminSinScopeDevuelve403() throws Exception {
        mvc.perform(get(URL).with(jwt().authorities(ADMIN)))
                .andExpect(status().isForbidden());
    }
}
