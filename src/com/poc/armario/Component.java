package com.poc.armario;

public class Component {
    private String id;
    private String tipo;
    private String valor;
    private int cantidad;
    private String ubicacion;
    private String foto;
    private String categoria;

 // En Component.java → constructor
    public Component(String id, String tipo, String valor, int cantidad, String ubicacion, String foto, String categoria) {
        this.id = id;
        this.tipo = tipo;
        this.valor = valor;  // Ya no llamamos a UnitNormalizer aquí
        this.cantidad = cantidad;
        this.ubicacion = ubicacion;
        this.foto = foto;
        this.categoria = categoria;
    }

    // Getters y Setters
    public String getId() { return id; }
    public String getTipo() { return tipo; }
    public String getValor() { return valor; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public String getUbicacion() { return ubicacion; }
    public String getFoto() { return foto; }
    public String getCategoria() { return categoria; }
}