package com.example.kafkaadmin.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Prevencion de log injection (CWE-117): los valores del usuario no pueden crear lineas falsas en el log. */
class ParaLogTest {

    @Test
    void reemplazaSaltosDeLineaYCaracteresDeControl() {
        String ataque = "reportes\nINFO accion=ELIMINAR_TOPICO usuario=admin";
        String limpio = KafkaAdminService.paraLog(ataque);
        assertFalse(limpio.contains("\n"));
        assertTrue(limpio.startsWith("reportes_INFO"));
    }

    @Test
    void valoresNormalesNoCambianYLosLargosSeRecortan() {
        assertEquals("pedidos-creados", KafkaAdminService.paraLog("pedidos-creados"));
        assertEquals("null", KafkaAdminService.paraLog(null));
        assertEquals(203, KafkaAdminService.paraLog("x".repeat(500)).length());
    }
}
