package com.example.analitica.config;

/** Topicos y grupos de consumidores que usa analitica-service. */
public final class KafkaTopicos {

    public static final String PEDIDOS_CREADOS = "pedidos-creados";
    public static final String PEDIDOS_ESTADOS = "pedidos-estados";
    public static final String SUFIJO_DLT = ".DLT";

    /** Grupo que calcula las metricas: cada particion es procesada por un solo hilo del grupo. */
    public static final String GRUPO_ANALITICA = "analitica";
    /** Grupo independiente: recibe TODOS los eventos otra vez (publicar/suscribir), sin afectar a analitica. */
    public static final String GRUPO_AUDITORIA = "auditoria";

    private KafkaTopicos() {
    }
}
