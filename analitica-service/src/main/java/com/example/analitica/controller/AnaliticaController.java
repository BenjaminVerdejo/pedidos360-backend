package com.example.analitica.controller;

import com.example.analitica.dto.EventoProcesado;
import com.example.analitica.dto.ResumenAnalitica;
import com.example.analitica.service.AnaliticaService;
import com.example.analitica.service.AuditoriaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** API de lectura de la analitica (la consulta el frontend cada pocos segundos). */
@RestController
@RequestMapping("/api/analitica")
public class AnaliticaController {

    private final AnaliticaService analitica;
    private final AuditoriaService auditoria;

    public AnaliticaController(AnaliticaService analitica, AuditoriaService auditoria) {
        this.analitica = analitica;
        this.auditoria = auditoria;
    }

    @GetMapping("/resumen")
    public ResumenAnalitica resumen() {
        return analitica.resumen();
    }

    /** Registro de auditoria: solo rol Admin (ver SecurityConfig). */
    @GetMapping("/auditoria")
    public List<EventoProcesado> auditoria() {
        return auditoria.ultimos();
    }
}
