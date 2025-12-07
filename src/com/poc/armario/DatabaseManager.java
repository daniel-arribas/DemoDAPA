package com.poc.armario;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        try {
        	
        	String dbPath = "C:/Users/daniel/Desktop/Workspace/DAPA/DemoDAPA/inventario.db"; // ruta paraindicar la DB
            
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public List<Component> getAllComponents() {
        List<Component> components = new ArrayList<>();
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM inventario");
            while (rs.next()) {
                Component comp = new Component(
                        rs.getString("id"),
                        rs.getString("tipo"),
                        rs.getString("valor"),
                        rs.getInt("cantidad"),
                        rs.getString("ubicacion"),
                        rs.getString("foto"),
                        rs.getString("categoria")
                );
                components.add(comp);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return components;
    }

    public Component findComponent(String tipo, String valor) {
        if (tipo == null || valor == null) return null;

        String cleanValor = UnitNormalizer.cleanForSearch(valor);

        // Esta query es súper tolerante: ignora µ/Ω/espacios/mayúsculas
        String sql = "SELECT * FROM inventario " +
                     "WHERE tipo = ? " +
                     "AND LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(valor, 'µ', 'u'), 'Ω', ''), 'ohm', ''), ' ', ''), 'Ohm', '')) LIKE ? " +
                     "LIMIT 1";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, tipo);
            pstmt.setString(2, "%" + cleanValor + "%");

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Component(
                    rs.getString("id"),
                    rs.getString("tipo"),
                    rs.getString("valor"),
                    rs.getInt("cantidad"),
                    rs.getString("ubicacion"),
                    rs.getString("foto"),
                    rs.getString("categoria")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void updateQuantity(String id, int cantidadDispensed) {
        try {
            PreparedStatement pstmt = connection.prepareStatement("UPDATE inventario SET cantidad = cantidad - ? WHERE id = ?");
            pstmt.setInt(1, cantidadDispensed);
            pstmt.setString(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void dispenseComponent(String id, int quantity) {
        try {
            PreparedStatement pstmt = connection.prepareStatement(
                "UPDATE inventario SET cantidad = cantidad - ? WHERE id = ? AND cantidad >= ?");
            pstmt.setInt(1, quantity);
            pstmt.setString(2, id);
            pstmt.setInt(3, quantity);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}