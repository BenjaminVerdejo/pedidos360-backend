package com.example.rabbitadmin.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion de la conexion administrativa con RabbitMQ.
 * Solo RabbitAdminService usa este bean; los controladores no lo conocen.
 */
@Configuration
public class RabbitAdminConfig {

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        // las declaraciones se hacen bajo demanda desde la API, no al arrancar
        admin.setAutoStartup(false);
        return admin;
    }
}
