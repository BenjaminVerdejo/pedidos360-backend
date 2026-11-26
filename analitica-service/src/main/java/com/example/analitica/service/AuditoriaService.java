package com.example.analitica.service;

import com.example.analitica.dto.EventoProcesado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Auditoria: guarda (en memoria, ultimos 200) y registra en el log cada evento de ambos topicos.
 * Corre en un consumer group distinto ("auditoria"), por lo que recibe todos los eventos aunque el
 * grupo "analitica" ya los haya consumido, y su avance (offset/lag) es independiente.
 */
@Service
public class AuditoriaService {

    private static final Logger auditoria = LoggerFactory.getLogger("AUDITORIA-KAFKA");
    private static final int MAX = 200;

    private final Deque<EventoProcesado> registros = new ArrayDeque<>();

    public synchronized void registrar(String tipo, String clave, String topico, int particion, long offset) {
        Long pedidoId = parsear(clave);
        registros.addFirst(new EventoProcesado(tipo, pedidoId, "auditado", topico, particion, offset, Instant.now().toString()));
        while (registros.size() > MAX) {
            registros.removeLast();
        }
        auditoria.info("evento={} pedido={} topico={} particion={} offset={}", tipo, clave, topico, particion, offset);
    }

    public synchronized List<EventoProcesado> ultimos() {
        return new ArrayList<>(registros);
    }

    private static Long parsear(String clave) {
        try {
            return clave != null ? Long.valueOf(clave) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
