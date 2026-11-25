package com.example.demo.model;

/**
 * Estados validos de un pedido. Antes el estado era un String libre: cualquier valor
 * (incluso texto malicioso) se guardaba en la base de datos y viajaba por RabbitMQ.
 * ENTREGADO y CANCELADO son estados finales: desde ahi no se permiten mas cambios.
 */
public enum EstadoPedido {
    PENDIENTE,
    ENVIADO,
    ENTREGADO,
    CANCELADO;

    public boolean esFinal() {
        return this == ENTREGADO || this == CANCELADO;
    }

    /** Convierte el valor guardado en BD; un valor desconocido se trata como PENDIENTE. */
    public static EstadoPedido desde(String valor) {
        if (valor == null) {
            return PENDIENTE;
        }
        try {
            return EstadoPedido.valueOf(valor);
        } catch (IllegalArgumentException e) {
            return PENDIENTE;
        }
    }
}
