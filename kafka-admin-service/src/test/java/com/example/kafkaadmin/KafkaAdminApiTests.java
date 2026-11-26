package com.example.kafkaadmin;

import com.example.kafkaadmin.dto.LagResponse;
import com.example.kafkaadmin.dto.TopicoResponse;
import com.example.kafkaadmin.exception.ConflictoException;
import com.example.kafkaadmin.service.KafkaAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Seguridad y validacion de la API administradora de Kafka (el cluster se simula con un mock). */
@SpringBootTest
class KafkaAdminApiTests {

    private static final SimpleGrantedAuthority SCOPE = new SimpleGrantedAuthority("SCOPE_Pedidos.Access");
    private static final SimpleGrantedAuthority ADMIN = new SimpleGrantedAuthority("ROLE_Admin");
    private static final String URL = "/api/kafka/topicos";

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private KafkaAdminService service;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static TopicoResponse topico(String nombre) {
        return new TopicoResponse(nombre, 3, 3, false, Map.of("retention.ms", "604800000"), List.of());
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void sinRolAdminDevuelve403() throws Exception {
        mvc.perform(get(URL).with(jwt().authorities(SCOPE))).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void adminListaTopicos() throws Exception {
        when(service.listarTopicos()).thenReturn(List.of(topico("pedidos-creados")));
        mvc.perform(get(URL).with(jwt().authorities(SCOPE, ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("pedidos-creados"))
                .andExpect(jsonPath("$[0].particiones").value(3));
    }

    @Test
    void crearTopicoValidoDevuelve201() throws Exception {
        when(service.crearTopico(any(), anyString())).thenReturn(topico("reportes"));
        mvc.perform(post(URL).with(jwt().authorities(SCOPE, ADMIN)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"reportes\",\"particiones\":3,\"replicas\":3,\"retencionMs\":86400000,\"cleanupPolicy\":\"delete\",\"minInsyncReplicas\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("reportes"));
    }

    @Test
    void nombreDeTopicoInvalidoDevuelve400() throws Exception {
        mvc.perform(post(URL).with(jwt().authorities(SCOPE, ADMIN)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"topico con espacios!\",\"particiones\":3,\"replicas\":3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nombre").exists());
        verifyNoInteractions(service);
    }

    @Test
    void replicasFueraDeRangoYCleanupInvalidoDevuelven400() throws Exception {
        mvc.perform(post(URL).with(jwt().authorities(SCOPE, ADMIN)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"x\",\"particiones\":3,\"replicas\":5,\"cleanupPolicy\":\"borrar\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.replicas").exists())
                .andExpect(jsonPath("$.campos.cleanupPolicy").exists());
    }

    @Test
    void modificarConfiguracion() throws Exception {
        when(service.actualizarConfiguracion(eq("pedidos-creados"), any(), anyString())).thenReturn(topico("pedidos-creados"));
        mvc.perform(put(URL + "/pedidos-creados/configuracion").with(jwt().authorities(SCOPE, ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"retencionMs\":172800000}"))
                .andExpect(status().isOk());
    }

    @Test
    void eliminarTopicoProtegidoDevuelve409() throws Exception {
        doThrow(new ConflictoException("protegido")).when(service).eliminarTopico(eq("pedidos-creados"), anyString());
        mvc.perform(delete(URL + "/pedidos-creados").with(jwt().authorities(SCOPE, ADMIN)))
                .andExpect(status().isConflict());
    }

    @Test
    void eliminarTopicoDevuelve204() throws Exception {
        mvc.perform(delete(URL + "/temporal").with(jwt().authorities(SCOPE, ADMIN)))
                .andExpect(status().isNoContent());
    }

    @Test
    void lagDeUnConsumerGroup() throws Exception {
        when(service.calcularLag("analitica")).thenReturn(new LagResponse("analitica", 5,
                List.of(new LagResponse.LagParticion("pedidos-creados", 0, 10, 15, 5))));
        mvc.perform(get("/api/kafka/consumer-groups/analitica/lag").with(jwt().authorities(SCOPE, ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lagTotal").value(5))
                .andExpect(jsonPath("$.particiones[0].lag").value(5));
    }

    @Test
    void resetConDestinoInvalidoDevuelve400() throws Exception {
        mvc.perform(post("/api/kafka/consumer-groups/analitica/reset-offsets").with(jwt().authorities(SCOPE, ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"topico\":\"pedidos-creados\",\"destino\":\"ayer\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
