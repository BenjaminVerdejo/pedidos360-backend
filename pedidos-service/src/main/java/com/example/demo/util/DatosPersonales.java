package com.example.demo.util;

import java.util.regex.Pattern;

/**
 * Enmascara datos personales antes de escribirlos en logs.
 * Ley 19.628 / Ley 21.719 (proteccion de datos personales, Chile): principio de minimizacion.
 * Ejemplo: ana.perez@empresa.cl -> a***@empresa.cl
 */
public final class DatosPersonales {

    private static final Pattern EMAIL = Pattern.compile("([A-Za-z0-9._%+-])[A-Za-z0-9._%+-]*@([A-Za-z0-9.-]+)");

    private DatosPersonales() {
    }

    public static String enmascararEmail(String email) {
        if (email == null || email.isBlank()) {
            return "(sin email)";
        }
        int arroba = email.indexOf('@');
        if (arroba <= 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(arroba);
    }

    /** Reemplaza todos los emails que aparezcan dentro de un texto (ej. el JSON de un mensaje). */
    public static String enmascararEmailsEnTexto(String texto) {
        if (texto == null) {
            return null;
        }
        return EMAIL.matcher(texto).replaceAll("$1***@$2");
    }
}
