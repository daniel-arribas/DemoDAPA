package com.poc.armario;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NetlistParser {

    public static List<ComponentRequest> parse(String filePath) {
        List<ComponentRequest> requests = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();

                // Ignorar comentarios, líneas vacías y comandos SPICE
                if (line.isEmpty() || 
                    line.startsWith("*") || 
                    line.startsWith(".") || 
                    line.toUpperCase().startsWith("TITLE")) {
                    continue;
                }

                // Dividir por espacios (puede haber más de uno)
                String[] parts = line.split("\\s+");
                if (parts.length < 4) continue;

                // El primer carácter del nombre del componente (R1, C2, L5...)
                char prefix = parts[0].toUpperCase().charAt(0);
                String valueStr = parts[3].toUpperCase();

                String tipo = null;

                // Compatible con Java 8+
                if (prefix == 'R') {
                    tipo = "Resistencia";
                } else if (prefix == 'C') {
                    tipo = "Condensador";
                } else if (prefix == 'L') {
                    tipo = "Inductor";
                }

                if (tipo != null) {
                    requests.add(new ComponentRequest(tipo, valueStr));
                }
            }
        } catch (IOException e) {
            System.err.println("Error leyendo netlist: " + filePath);
            e.printStackTrace();
        }

        return requests;
    }

    // Clase interna para representar una petición de componente
    public static class ComponentRequest {
        public final String tipo;
        public final String valor;

        public ComponentRequest(String tipo, String valor) {
            this.tipo = tipo;
            this.valor = valor;
        }

        @Override
        public String toString() {
            return tipo + " " + valor;
        }
    }
}