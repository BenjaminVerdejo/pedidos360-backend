package com.example.rabbitadmin;

import com.example.rabbitadmin.config.AdminProperties;
import com.example.rabbitadmin.exception.ConflictoException;
import com.example.rabbitadmin.exception.RecursoNoEncontradoException;
import com.example.rabbitadmin.service.RabbitAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** Reglas de negocio del servicio administrador, sin RabbitMQ real. */
class RabbitAdminServiceTests {

    private RabbitAdmin rabbitAdmin;
    private RabbitAdminService service;

    @BeforeEach
    void setUp() {
        rabbitAdmin = mock(RabbitAdmin.class);
        AdminProperties props = new AdminProperties(
                new AdminProperties.Protegidos(List.of("tickets.queue"), List.of("pedidos.exchange")));
        service = new RabbitAdminService(rabbitAdmin, props);
    }

    @Test
    void noPermiteEliminarColaProtegida() {
        assertThrows(ConflictoException.class, () -> service.eliminarCola("tickets.queue", false));
        verify(rabbitAdmin, never()).deleteQueue(anyString());
    }

    @Test
    void noPermiteEliminarExchangeProtegido() {
        assertThrows(ConflictoException.class, () -> service.eliminarExchange("pedidos.exchange"));
        verify(rabbitAdmin, never()).deleteExchange(anyString());
    }

    @Test
    void eliminarColaInexistenteLanza404() {
        when(rabbitAdmin.getQueueInfo("nada.queue")).thenReturn(null);
        assertThrows(RecursoNoEncontradoException.class, () -> service.eliminarCola("nada.queue", false));
        verify(rabbitAdmin, never()).deleteQueue(anyString());
    }

    @Test
    void noEliminaColaConMensajesSiSePideSoloSiVacia() {
        when(rabbitAdmin.getQueueInfo("a.queue")).thenReturn(new QueueInformation("a.queue", 5, 0));
        assertThrows(ConflictoException.class, () -> service.eliminarCola("a.queue", true));
        verify(rabbitAdmin, never()).deleteQueue(anyString());
    }

    @Test
    void eliminaColaExistente() {
        when(rabbitAdmin.getQueueInfo("a.queue")).thenReturn(new QueueInformation("a.queue", 0, 0));
        service.eliminarCola("a.queue", true);
        verify(rabbitAdmin).deleteQueue("a.queue");
    }
}
