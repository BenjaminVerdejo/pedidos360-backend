package com.example.kafkaadmin;

import com.example.kafkaadmin.config.AdminKafkaProperties;
import com.example.kafkaadmin.dto.ActualizarConfigRequest;
import com.example.kafkaadmin.dto.CrearTopicoRequest;
import com.example.kafkaadmin.exception.ConflictoException;
import com.example.kafkaadmin.service.KafkaAdminService;
import org.apache.kafka.clients.admin.AdminClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/** Reglas de negocio de la administracion: se validan ANTES de llamar al cluster. */
class KafkaAdminServiceTest {

    private AdminClient admin;
    private KafkaAdminService service;

    @BeforeEach
    void setUp() {
        admin = mock(AdminClient.class);
        AdminKafkaProperties props = new AdminKafkaProperties(List.of("pedidos-creados", "pedidos-estados"), 5, 12);
        service = new KafkaAdminService(admin, props);
    }

    @Test
    void noSePuedeEliminarUnTopicoDelSistema() {
        assertThrows(ConflictoException.class, () -> service.eliminarTopico("pedidos-creados", "ana"));
        assertThrows(ConflictoException.class, () -> service.eliminarTopico("__consumer_offsets", "ana"));
        verifyNoInteractions(admin);
    }

    @Test
    void limiteDeParticiones() {
        CrearTopicoRequest req = new CrearTopicoRequest("grande", 40, (short) 3, null, null, null);
        assertThrows(IllegalArgumentException.class, () -> service.crearTopico(req, "ana"));
        verifyNoInteractions(admin);
    }

    @Test
    void minInsyncNoPuedeSuperarLasReplicas() {
        CrearTopicoRequest req = new CrearTopicoRequest("t", 3, (short) 2, null, "delete", 3);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.crearTopico(req, "ana"));
        assertTrue(ex.getMessage().contains("minInsyncReplicas"));
    }

    @Test
    void modificarSinCambiosEsInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> service.actualizarConfiguracion("t", new ActualizarConfigRequest(null, null, null, null), "ana"));
        verifyNoInteractions(admin);
    }

    @Test
    void topicosInternosSeConsideranProtegidos() {
        AdminKafkaProperties props = new AdminKafkaProperties(null, 0, 0);
        assertTrue(props.esProtegido("__consumer_offsets"));
        assertFalse(props.esProtegido("reportes"));
        assertEquals(10, props.timeoutSegundos());
        assertEquals(12, props.maxParticiones());
    }
}
