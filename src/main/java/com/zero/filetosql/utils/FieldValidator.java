package com.zero.filetosql.utils;


public class FieldValidator {

    // Método para validar cadenas de texto ]
    public static boolean isValidText(String input) {
        return input != null && !input.isEmpty();
    }

    // Método para validar enteros
    public static boolean isValidInt(String input) {
        try {
            Integer.parseInt(input);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Método para validar dobles
    public static boolean isValidDouble(String input) {
        try {
            Double.parseDouble(input);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isValidBoolean(String input) {
        return "true".equalsIgnoreCase(input) || "false".equalsIgnoreCase(input);
    }



}
