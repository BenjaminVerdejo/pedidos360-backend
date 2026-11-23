package com.example.demo.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Enmascaramiento de datos personales en logs (Ley 19.628 / Ley 21.719). */
class DatosPersonalesTest {

    @Test
    void enmascaraEmail() {
        assertEquals("a***@empresa.cl", DatosPersonales.enmascararEmail("ana.perez@empresa.cl"));
    }

    @Test
    void emailVacioONuloNoFalla() {
        assertEquals("(sin email)", DatosPersonales.enmascararEmail(null));
        assertEquals("(sin email)", DatosPersonales.enmascararEmail(" "));
        assertEquals("***", DatosPersonales.enmascararEmail("sin-arroba"));
    }

    @Test
    void enmascaraEmailsDentroDeUnJson() {
        String json = "{\"clienteEmail\":\"juan.soto@correo.cl\",\"descripcion\":\"ok\"}";
        String r = DatosPersonales.enmascararEmailsEnTexto(json);
        assertFalse(r.contains("juan.soto"));
        assertTrue(r.contains("j***@correo.cl"));
        assertTrue(r.contains("\"descripcion\":\"ok\""));
    }
}
