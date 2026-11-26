package com.example.analitica;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * analitica-service: consume los eventos de pedidos desde Kafka y mantiene metricas en tiempo
 * real (consumer group "analitica") y un registro de auditoria (consumer group "auditoria").
 */
@SpringBootApplication
public class AnaliticaServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnaliticaServiceApplication.class, args);
    }
}
