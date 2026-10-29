package com.example.demo.service;

import com.example.demo.exception.ErrorNoRecuperableException;
import com.example.demo.exception.ErrorRecuperableException;
import com.example.demo.messaging.dto.PedidoEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Logica de negocio: genera el comprobante de entrega cuando un pedido pasa a ENTREGADO.
 * No conoce RabbitMQ.
 */
@Service
public class DocumentoService {

    private static final Logger log = LoggerFactory.getLogger(DocumentoService.class);

    /** Palabra clave para simular en la demo un generador de PDF caido (error recuperable). */
    static final String SIMULAR_FALLA_TEMPORAL = "#falla-documento";

    public String generarComprobanteEntrega(PedidoEvento evento) {
        if (evento.pedidoId() == null) {
            throw new ErrorNoRecuperableException("El evento no trae id de pedido");
        }
        if (!PedidoEvento.PEDIDO_ENTREGADO.equals(evento.tipo())) {
            throw new ErrorNoRecuperableException("Evento inesperado en documentos: " + evento.tipo());
        }
        if (evento.descripcion() != null && evento.descripcion().contains(SIMULAR_FALLA_TEMPORAL)) {
            throw new ErrorRecuperableException("Generador de documentos PDF no disponible (simulado)");
        }

        String documento = "ENTREGA-" + evento.pedidoId() + ".pdf";
        // Aqui se generaria el PDF real. Se simula con un log.
        log.info("Comprobante de entrega {} generado | cliente: {} | total: {}",
                documento, evento.clienteNombre(), evento.total());
        return documento;
    }
}
