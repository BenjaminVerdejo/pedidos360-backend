package com.example.analitica.config;

import com.example.analitica.exception.EventoInvalidoException;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejo de errores de los consumidores Kafka.
 *
 * 1. Errores transitorios (por ejemplo, una dependencia caida): se reintenta 3 veces con espera
 *    exponencial (1 s, 2 s, 4 s) sin bloquear la particion indefinidamente.
 * 2. Mensajes defectuosos (JSON invalido o evento que no pasa la validacion): NO se reintentan,
 *    porque fallarian siempre; van directo al topico de mensajes fallidos (DLT).
 * 3. Si se agotan los reintentos, el mensaje tambien va al DLT: <topico>.DLT, misma particion.
 *
 * Asi un mensaje "veneno" nunca detiene el consumo de los demas (el offset avanza) y queda guardado
 * en el DLT para revisarlo. Spring Boot aplica este DefaultErrorHandler a todos los @KafkaListener.
 */
@Configuration
public class KafkaConsumidorConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumidorConfig.class);

    /**
     * Productor que solo usa el recuperador para escribir en los DLT.
     * - byte[]: mensajes que ni siquiera se pudieron deserializar (se reenvian tal cual llegaron).
     * - Object: eventos deserializados que fallaron en la logica de negocio (se reenvian como JSON).
     */
    @Bean
    public KafkaTemplate<String, Object> dltKafkaTemplate(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "analitica-service-dlt");
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000);

        JsonSerializer<Object> json = new JsonSerializer<>();
        json.setAddTypeInfo(false);
        Map<Class<?>, Serializer<?>> porTipo = new LinkedHashMap<>();
        porTipo.put(byte[].class, new ByteArraySerializer());
        porTipo.put(Object.class, json);

        @SuppressWarnings({"unchecked", "rawtypes"})
        Serializer<Object> valor = (Serializer) new DelegatingByTypeSerializer(porTipo, true);
        DefaultKafkaProducerFactory<String, Object> factory =
                new DefaultKafkaProducerFactory<>(props, new StringSerializer(), valor);
        return new KafkaTemplate<>(factory);
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            KafkaTemplate<String, Object> dltKafkaTemplate,
            @Value("${app.kafka.reintentos:3}") int reintentos,
            @Value("${app.kafka.espera-inicial-ms:1000}") long esperaInicialMs) {

        DeadLetterPublishingRecoverer recuperador = new DeadLetterPublishingRecoverer(dltKafkaTemplate,
                (registro, error) -> {
                    log.error("Mensaje enviado al DLT: topico={} particion={} offset={} clave={} motivo={}",
                            registro.topic(), registro.partition(), registro.offset(), registro.key(),
                            error.getMessage());
                    return new TopicPartition(registro.topic() + KafkaTopicos.SUFIJO_DLT, registro.partition());
                });

        ExponentialBackOffWithMaxRetries espera = new ExponentialBackOffWithMaxRetries(reintentos);
        espera.setInitialInterval(esperaInicialMs);
        espera.setMultiplier(2.0);
        espera.setMaxInterval(10_000L);

        DefaultErrorHandler handler = new DefaultErrorHandler(recuperador, espera);
        // reintentar no sirve si el evento es invalido: va directo al DLT
        handler.addNotRetryableExceptions(EventoInvalidoException.class, IllegalArgumentException.class);
        handler.setRetryListeners((registro, error, intento) ->
                log.warn("Reintento {} del mensaje topico={} particion={} offset={}: {}",
                        intento, registro.topic(), registro.partition(), registro.offset(), error.getMessage()));
        return handler;
    }
}
