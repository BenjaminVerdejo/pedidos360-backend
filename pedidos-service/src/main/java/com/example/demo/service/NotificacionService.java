package com.example.demo.service;

import com.example.demo.exception.ErrorNoRecuperableException;
import com.example.demo.exception.ErrorRecuperableException;
import com.example.demo.messaging.dto.PedidoEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Logica de negocio de notificaciones al cliente.
 * No conoce RabbitMQ: recibe un evento ya deserializado y lanza excepciones de negocio.
 */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    /** Palabra clave para simular en la demo un servidor de correo caido (error recuperable). */
    static final String SIMULAR_FALLA_TEMPORAL = "#falla-correo";

    public void notificar(PedidoEvento evento) {
        if (evento.pedidoId() == null) {
            throw new ErrorNoRecuperableException("El evento no trae id de pedido");
        }
        if (evento.clienteEmail() == null || evento.clienteEmail().isBlank()) {
            throw new ErrorNoRecuperableException("El pedido " + evento.pedidoId() + " no tiene email de cliente");
        }
        if (evento.descripcion() != null && evento.descripcion().contains(SIMULAR_FALLA_TEMPORAL)) {
            throw new ErrorRecuperableException("Servidor de correo no disponible (simulado)");
        }

        String asunto = switch (evento.tipo()) {
            case "PEDIDO_CREADO" -> "Recibimos tu pedido #" + evento.pedidoId();
            case "PEDIDO_ESTADO_CAMBIADO" -> "Tu pedido #" + evento.pedidoId() + " cambio a " + evento.estado();
            default -> throw new ErrorNoRecuperableException("Tipo de evento desconocido: " + evento.tipo());
        };

        // Aqui iria el envio real (SES, SMTP, etc.). Se simula con un log.
        log.info("Notificacion enviada a {} | asunto: '{}'", evento.clienteEmail(), asunto);
    }
}
