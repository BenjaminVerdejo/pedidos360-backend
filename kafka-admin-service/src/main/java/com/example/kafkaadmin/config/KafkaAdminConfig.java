package com.example.kafkaadmin.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

/**
 * Cliente de administracion de Kafka. Reutiliza la configuracion de spring.kafka.* (bootstrap servers,
 * timeouts) que Spring Boot carga en KafkaAdmin. El AdminClient se conecta de forma perezosa: el
 * servicio arranca aunque el cluster aun no este disponible.
 */
@Configuration
public class KafkaAdminConfig {

    @Bean(destroyMethod = "close")
    public AdminClient adminClient(KafkaAdmin kafkaAdmin) {
        return AdminClient.create(kafkaAdmin.getConfigurationProperties());
    }
}
