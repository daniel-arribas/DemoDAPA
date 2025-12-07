package com.poc.armario;

public class UnitNormalizer {

    /**
     * Normaliza el valor del componente.
     * Si es un número con unidad (1k, 100u, 4.7M, etc.) lo convierte a valor numérico en forma estándar.
     * Si es "Desconocido" o cualquier texto no numérico, lo devuelve tal cual.
     */
    public static String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "0";
        }

        String original = value.trim();
        
        // Si ya es "Desconocido" o contiene texto que no es número → no tocar
        if (original.equalsIgnoreCase("Desconocido") || 
            original.contains("Desconocido") || 
            !original.matches(".*\\d.*")) {  // no tiene ningún dígito
            return original;
        }

        // Quitar símbolos comunes que puedan estar en la DB
        String clean = original.replaceAll("[ΩµFnHpFkKmM]", "").trim();

        double multiplier = 1.0;

        char lastChar = original.charAt(original.length() - 1);
        switch (Character.toLowerCase(lastChar)) {
            case 'k': multiplier = 1_000;      break;
            case 'm': 
                if (original.toLowerCase().contains("meg")) multiplier = 1_000_000;
                else multiplier = 0.001; 
                break;
            case 'u': case 'µ': multiplier = 0.000_001; break;
            case 'n': multiplier = 0.000_000_001; break;
            case 'p': multiplier = 0.000_000_000_001; break;
            case 'f': multiplier = 0.000_000_000_000_001; break;
        }

        // Si el último carácter era letra, quitarlo para parsear el número
        if (Character.isLetter(lastChar)) {
            clean = clean.substring(0, clean.length() - 1);
        }

        try {
            double numericValue = Double.parseDouble(clean) * multiplier;
            return String.valueOf(numericValue);
        } catch (NumberFormatException e) {
            // Si por cualquier motivo falla, devolvemos el valor original
            return original;
        }
    }
    
    public static String cleanForSearch(String value) {
        if (value == null) return "";
        return value
                .replace("µ", "u")
                .replace("Ω", "")
                .replace("ohm", "")
                .replace("Ohm", "")
                .replace(" ", "")
                .replace("F", "")
                .replace("H", "")
                .trim()
                .toLowerCase();
    }
}