package com.example.analitica.service;

import com.example.analitica.exception.EventoInvalidoException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/** Valida el payload de cada evento contra su contrato (Bean Validation) antes de procesarlo. */
@Component
public class ValidadorEventos {

    private final Validator validator;

    public ValidadorEventos(Validator validator) {
        this.validator = validator;
    }

    public <T> T validar(T evento) {
        if (evento == null) {
            throw new EventoInvalidoException("Evento vacio (payload nulo)");
        }
        Set<ConstraintViolation<T>> errores = validator.validate(evento);
        if (!errores.isEmpty()) {
            String detalle = errores.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining(", "));
            throw new EventoInvalidoException(evento.getClass().getSimpleName() + " invalido: " + detalle);
        }
        return evento;
    }
}
