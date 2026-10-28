package com.example.demo.messaging.consumer.support;

import com.example.demo.messaging.config.MensajeriaProperties;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.demo.exception.ErrorNoRecuperableException;
import com.example.demo.exception.ErrorRecuperableException;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Centraliza la politica de confirmacion de mensajes para TODOS los consumidores.
 *
 * Decisiones:
 *  1. Procesamiento OK                 -> basicAck  (el mensaje se elimina de la cola)
 *  2. ErrorRecuperableException        -> reintento local con espera, hasta maxReintentos
 *  3. Reintentos agotados              -> basicNack(requeue=false) -> DLX -> DLQ
 *  4. ErrorNoRecuperableException      -> basicNack(requeue=false) -> DLX -> DLQ (sin reintentar)
 *  5. Cualquier otra excepcion         -> se trata como no recuperable (no se arriesga un bucle)
 *
 * Nunca se usa requeue=true: asi un mensaje "venenoso" no queda dando vueltas para siempre.
 */
@Component
public class ConfirmadorMensajes {

    private static final Logger log = LoggerFactory.getLogger(ConfirmadorMensajes.class);

    private final MensajeriaProperties props;

    public ConfirmadorMensajes(MensajeriaProperties props) {
        this.props = props;
    }

    /** Accion de negocio a ejecutar sobre el mensaje. */
    @FunctionalInterface
    public interface Procesamiento {
        void ejecutar();
    }

    public void procesar(String consumidor, Object idMensaje, Channel channel, long deliveryTag,
                         Procesamiento procesamiento) throws IOException {
        int maxReintentos = props.consumidor().maxReintentos();
        int intento = 1;
        while (true) {
            try {
                procesamiento.ejecutar();
                channel.basicAck(deliveryTag, false);
                log.info("[{}] ACK mensaje {} (intento {})", consumidor, idMensaje, intento);
                return;

            } catch (ErrorRecuperableException e) {
                if (intento > maxReintentos) {
                    log.error("[{}] NACK mensaje {} -> DLQ. Reintentos agotados ({}). Causa: {}",
                            consumidor, idMensaje, maxReintentos, e.getMessage());
                    channel.basicNack(deliveryTag, false, false);
                    return;
                }
                log.warn("[{}] Error recuperable en mensaje {} (intento {}/{}): {}. Reintentando...",
                        consumidor, idMensaje, intento, maxReintentos + 1, e.getMessage());
                esperar();
                intento++;

            } catch (ErrorNoRecuperableException e) {
                log.error("[{}] NACK mensaje {} -> DLQ. Error no recuperable: {}",
                        consumidor, idMensaje, e.getMessage());
                channel.basicNack(deliveryTag, false, false);
                return;

            } catch (RuntimeException e) {
                log.error("[{}] NACK mensaje {} -> DLQ. Error inesperado: {}",
                        consumidor, idMensaje, e.toString());
                channel.basicNack(deliveryTag, false, false);
                return;
            }
        }
    }

    private void esperar() {
        try {
            Thread.sleep(props.consumidor().esperaMs());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
