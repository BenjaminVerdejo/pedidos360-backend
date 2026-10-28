package com.example.demo.service;

import com.example.demo.exception.ErrorNoRecuperableException;
import com.example.demo.exception.ErrorRecuperableException;
import com.example.demo.messaging.dto.PedidoEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Logica de negocio de generacion de tickets/comprobantes de pedido.
 * No conoce RabbitMQ.
 */
@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** Palabra clave para simular en la demo un generador de documentos caido (error recuperable). */
    static final String SIMULAR_FALLA_TEMPORAL = "#falla-ticket";

    public String generarTicket(PedidoEvento evento) {
        if (evento.pedidoId() == null) {
            throw new ErrorNoRecuperableException("El evento no trae id de pedido");
        }
        if (evento.total() == null || evento.total().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ErrorNoRecuperableException("Total invalido para el pedido " + evento.pedidoId());
        }
        if (evento.descripcion() != null && evento.descripcion().contains(SIMULAR_FALLA_TEMPORAL)) {
            throw new ErrorRecuperableException("Generador de documentos no disponible (simulado)");
        }

        String fecha = evento.fecha() != null ? evento.fecha().format(FORMATO) : "SINFECHA";
        String codigo = "TCK-" + fecha + "-" + evento.pedidoId();

        // Aqui se generaria el PDF/ticket real. Se simula con un log.
        log.info("Ticket {} generado | cliente: {} | total: {}", codigo, evento.clienteNombre(), evento.total());
        return codigo;
    }
}
