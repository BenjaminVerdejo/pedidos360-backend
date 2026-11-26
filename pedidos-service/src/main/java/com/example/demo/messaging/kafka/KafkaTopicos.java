package com.example.demo.messaging.kafka;

/**
 * Nombres de los topicos Kafka de Pedidos360 (unico lugar donde se escriben en el codigo).
 *
 * pedidos-creados : un evento por cada pedido nuevo. Retencion por tiempo (delete, 7 dias).
 * pedidos-estados : un evento por cada cambio de estado. Compactado: Kafka conserva siempre
 *                   el ultimo estado de cada pedido (clave = id del pedido).
 */
public final class KafkaTopicos {

    public static final String PEDIDOS_CREADOS = "pedidos-creados";
    public static final String PEDIDOS_ESTADOS = "pedidos-estados";

    /** Tipos de evento (se envian en la cabecera __TypeId__ para que el consumidor sepa a que DTO mapear). */
    public static final String TIPO_PEDIDO_CREADO = "pedidoCreado";
    public static final String TIPO_ESTADO_CAMBIADO = "estadoCambiado";

    private KafkaTopicos() {
    }
}
