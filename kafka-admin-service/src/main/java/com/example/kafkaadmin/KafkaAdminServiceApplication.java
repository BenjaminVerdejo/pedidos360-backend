package com.example.kafkaadmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** kafka-admin-service: API para administrar topicos, configuraciones y consumer groups de Kafka. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class KafkaAdminServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(KafkaAdminServiceApplication.class, args);
    }
}
