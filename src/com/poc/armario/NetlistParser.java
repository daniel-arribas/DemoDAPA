package com.poc.armario;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NetlistParser {
    public static List<Component> parse(String filePath) {
        List<Component> components = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("*")) continue; // Ignorar comentarios
                String[] parts = line.split("\\s+");
                if (parts.length >= 4 && (parts[0].startsWith("R") || parts[0].startsWith("C") || parts[0].startsWith("L"))) {
                    String tipo = getTipo(parts[0].charAt(0));
                    String valor = UnitNormalizer.normalize(parts[3]); // Valor es el 4to token
                    // Crear componente dummy (solo tipo y valor, id/cantidad se llenan de DB)
                    components.add(new Component(null, tipo, valor, 1, null, null, null));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return components;
    }

    private static String getTipo(char prefix) {
        switch (prefix) {
            case 'R': return "Resistencia";
            case 'C': return "Condensador";
            case 'L': return "Inductor";
            default: return "Desconocido";
        }
    }
}