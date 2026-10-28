package com.example.rabbitadmin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Configuracion centralizada del administrador (prefijo app.admin en application.yml). */
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(Protegidos protegidos) {

    public AdminProperties {
        if (protegidos == null) {
            protegidos = new Protegidos(List.of(), List.of());
        }
    }

    /** Colas y exchanges que la API NO puede eliminar. */
    public record Protegidos(List<String> colas, List<String> exchanges) {
        public Protegidos {
            colas = colas == null ? List.of() : List.copyOf(colas);
            exchanges = exchanges == null ? List.of() : List.copyOf(exchanges);
        }
    }
}
