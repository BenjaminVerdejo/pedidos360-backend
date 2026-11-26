package com.example.analitica.listener;

import com.example.analitica.config.KafkaTopicos;
import com.example.analitica.dto.EstadoPedidoCambiadoEvento;
import com.example.analitica.dto.PedidoCreadoEvento;
import com.example.analitica.service.AnaliticaService;
import com.example.analitica.service.ValidadorEventos;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumidores del grupo "analitica" (capa de mensajeria). Solo reciben el mensaje, lo validan y
 * delegan en AnaliticaService (capa de dominio). Si el procesamiento lanza una excepcion, el
 * DefaultErrorHandler reintenta o envia el mensaje al DLT (ver KafkaConsumidorConfig).
 *
 * concurrency = 3: un hilo por particion. Si se levanta una segunda instancia del servicio,
 * Kafka reparte las 3 particiones entre ambas (balanceo de carga del consumer group).
 */
@Component
public class PedidoEventosListener {

    private final AnaliticaService analitica;
    private final ValidadorEventos validador;

    public PedidoEventosListener(AnaliticaService analitica, ValidadorEventos validador) {
        this.analitica = analitica;
        this.validador = validador;
    }

    @KafkaListener(id = "analitica-pedidos-creados", topics = KafkaTopicos.PEDIDOS_CREADOS,
            groupId = KafkaTopicos.GRUPO_ANALITICA, concurrency = "${app.kafka.concurrencia:3}")
    public void onPedidoCreado(@Payload PedidoCreadoEvento evento,
                               @Header(KafkaHeaders.RECEIVED_TOPIC) String topico,
                               @Header(KafkaHeaders.RECEIVED_PARTITION) int particion,
                               @Header(KafkaHeaders.OFFSET) long offset) {
        analitica.registrarPedidoCreado(validador.validar(evento), topico, particion, offset);
    }

    @KafkaListener(id = "analitica-pedidos-estados", topics = KafkaTopicos.PEDIDOS_ESTADOS,
            groupId = KafkaTopicos.GRUPO_ANALITICA, concurrency = "${app.kafka.concurrencia:3}")
    public void onEstadoCambiado(@Payload EstadoPedidoCambiadoEvento evento,
                                 @Header(KafkaHeaders.RECEIVED_TOPIC) String topico,
                                 @Header(KafkaHeaders.RECEIVED_PARTITION) int particion,
                                 @Header(KafkaHeaders.OFFSET) long offset) {
        analitica.registrarCambioEstado(validador.validar(evento), topico, particion, offset);
    }
}
