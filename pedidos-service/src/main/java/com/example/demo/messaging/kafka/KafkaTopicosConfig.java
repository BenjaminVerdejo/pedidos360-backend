package com.example.demo.messaging.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declaracion de los topicos con sus parametros de administracion. Al arrancar, KafkaAdmin
 * los crea si no existen (el script kafka-init de docker-compose hace lo mismo, asi el cluster
 * queda listo aunque este servicio aun no haya arrancado).
 *
 * - 3 particiones: hasta 3 consumidores de un mismo grupo procesan en paralelo.
 * - 3 replicas + min.insync.replicas=2: con acks=all un evento se confirma solo cuando esta en
 *   al menos 2 brokers; el cluster tolera la caida de 1 broker sin perder datos.
 */
@Configuration
@EnableConfigurationProperties(KafkaTopicosProperties.class)
@ConditionalOnProperty(prefix = "app.kafka", name = "habilitado", havingValue = "true", matchIfMissing = true)
public class KafkaTopicosConfig {

    /** Historial de pedidos creados: se borran los eventos con mas de 7 dias (analitica de la semana). */
    @Bean
    public NewTopic topicoPedidosCreados(KafkaTopicosProperties props) {
        return TopicBuilder.name(KafkaTopicos.PEDIDOS_CREADOS)
                .partitions(props.particiones())
                .replicas(props.replicas())
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(props.retencionCreadosMs()))
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, props.minInsyncReplicas())
                .build();
    }

    /**
     * Estados de pedidos: topico COMPACTADO. Kafka conserva al menos el ultimo evento de cada
     * clave (id del pedido), por lo que un consumidor nuevo puede reconstruir el estado actual
     * de todos los pedidos leyendo el topico desde el inicio.
     */
    @Bean
    public NewTopic topicoPedidosEstados(KafkaTopicosProperties props) {
        return TopicBuilder.name(KafkaTopicos.PEDIDOS_ESTADOS)
                .partitions(props.particiones())
                .replicas(props.replicas())
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT)
                .config(TopicConfig.MIN_COMPACTION_LAG_MS_CONFIG, "60000")
                .config(TopicConfig.SEGMENT_MS_CONFIG, "3600000")
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, props.minInsyncReplicas())
                .build();
    }

    /** Topicos de mensajes fallidos (DLT) que usa analitica-service: misma cantidad de particiones. */
    @Bean
    public NewTopic topicoPedidosCreadosDlt(KafkaTopicosProperties props) {
        return topicoDlt(KafkaTopicos.PEDIDOS_CREADOS + ".DLT", props);
    }

    @Bean
    public NewTopic topicoPedidosEstadosDlt(KafkaTopicosProperties props) {
        return topicoDlt(KafkaTopicos.PEDIDOS_ESTADOS + ".DLT", props);
    }

    private static NewTopic topicoDlt(String nombre, KafkaTopicosProperties props) {
        return TopicBuilder.name(nombre)
                .partitions(props.particiones())
                .replicas(props.replicas())
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(props.retencionDltMs()))
                .build();
    }
}
