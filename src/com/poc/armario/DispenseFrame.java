package com.poc.armario;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class DispenseFrame extends JFrame {

    private DefaultTableModel inventoryModel;
    private DefaultTableModel cartModel;
    private JTable inventoryTable;
    private JTable cartTable;
    private final List<CartItem> cart = new ArrayList<>();

    private static class CartItem {
        Component component;
        int requestedQuantity;
        CartItem(Component c, int q) { this.component = c; this.requestedQuantity = q; }
    }

    public DispenseFrame() {
        setTitle("ARMARIO INTELIGENTE - Dispensación de Componentes");
        setSize(1400, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(1, 2, 10, 0));

        // === PANEL IZQUIERDO: INVENTARIO ===
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(0, 120, 215), 3),
            " INVENTARIO DISPONIBLE"));

        inventoryModel = new DefaultTableModel(
            new String[]{"ID", "Tipo", "Valor", "Stock", "Ubicación"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        inventoryTable = new JTable(inventoryModel);
        inventoryTable.setRowHeight(28);
        inventoryTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inventoryTable.getTableHeader().setBackground(new Color(0, 120, 215));
        inventoryTable.getTableHeader().setForeground(Color.WHITE);
        inventoryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Doble click o click derecho para añadir
        inventoryTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 || SwingUtilities.isRightMouseButton(e)) {
                    int row = inventoryTable.getSelectedRow();
                    if (row != -1) {
                        addComponentFromInventory(row);
                    }
                }
            }
        });

        leftPanel.add(new JScrollPane(inventoryTable), BorderLayout.CENTER);
        JLabel leftHint = new JLabel("Doble click o click derecho en un componente para añadir al carrito", SwingConstants.CENTER);
        leftHint.setForeground(Color.GRAY);
        leftPanel.add(leftHint, BorderLayout.SOUTH);

        // === PANEL DERECHO: CARRITO ===
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(0, 150, 0), 3),
            "CARRITO DE DISPENSACIÓN"));

        cartModel = new DefaultTableModel(
            new String[]{"ID", "Tipo", "Valor", "Solicitado", "Stock"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3; // Solo cantidad editable
            }
        };
        cartTable = new JTable(cartModel);
        cartTable.setRowHeight(28);
        cartTable.getTableHeader().setBackground(new Color(0, 150, 0));
        cartTable.getTableHeader().setForeground(Color.WHITE);

        rightPanel.add(new JScrollPane(cartTable), BorderLayout.CENTER);

        // Botones carrito
        JPanel cartButtons = new JPanel(new FlowLayout());
        JButton removeBtn = new JButton("Eliminar");
        removeBtn.addActionListener(e -> {
            int row = cartTable.getSelectedRow();
            if (row != -1) {
                cart.remove(row);
                refreshCart();
            }
        });
        JButton clearBtn = new JButton("Vaciar Carrito");
        clearBtn.addActionListener(e -> {
            cart.clear();
            refreshCart();
        });
        cartButtons.add(removeBtn);
        cartButtons.add(clearBtn);
        rightPanel.add(cartButtons, BorderLayout.NORTH);

        // Botón confirmar grande
        JButton confirmBtn = new JButton("CONFIRMAR Y DISPENSAR");
        confirmBtn.setFont(new Font("Arial", Font.BOLD, 24));
        confirmBtn.setBackground(new Color(0, 150, 0));
        confirmBtn.setForeground(Color.WHITE);
        confirmBtn.setPreferredSize(new Dimension(0, 80));
        confirmBtn.addActionListener(e -> confirmDispense());
        rightPanel.add(confirmBtn, BorderLayout.SOUTH);

        // Añadir paneles a la ventana
        add(leftPanel);
        add(rightPanel);

        // Cargar inventario
        loadInventory();
    }

    private void addComponentFromInventory(int row) {
        String id = (String) inventoryModel.getValueAt(row, 0);
        String tipo = (String) inventoryModel.getValueAt(row, 1);
        String valor = (String) inventoryModel.getValueAt(row, 2);
        int stock = (Integer) inventoryModel.getValueAt(row, 3);

        String input = JOptionPane.showInputDialog(this,
            "<html><b>" + tipo + " " + valor + "</b><br>Stock disponible: " + stock + 
            "<br><br>¿Cuántas unidades quieres dispensar?</html>", "1");

        if (input == null || input.trim().isEmpty()) return;

        try {
            int qty = Integer.parseInt(input.trim());
            if (qty <= 0) throw new Exception();
            if (qty > stock) {
                JOptionPane.showMessageDialog(this, "No hay suficiente stock", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Component comp = DatabaseManager.getInstance().findComponent(tipo, valor);
            if (comp != null) {
                // Si ya está en el carrito, sumar
                for (CartItem item : cart) {
                    if (item.component.getId().equals(id)) {
                        item.requestedQuantity += qty;
                        refreshCart();
                        return;
                    }
                }
                // Si no, añadir nuevo
                cart.add(new CartItem(comp, qty));
                refreshCart();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Cantidad no válida", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshCart() {
        cartModel.setRowCount(0);
        for (CartItem item : cart) {
            cartModel.addRow(new Object[]{
                item.component.getId(),
                item.component.getTipo(),
                item.component.getValor(),
                item.requestedQuantity,
                item.component.getCantidad()
            });
        }
    }

    private void loadInventory() {
        inventoryModel.setRowCount(0);
        List<Component> list = DatabaseManager.getInstance().getAllComponents();
        for (Component c : list) {
            inventoryModel.addRow(new Object[]{
                c.getId(),
                c.getTipo(),
                c.getValor(),
                c.getCantidad(),
                c.getUbicacion()
            });
        }
    }

    private void confirmDispense() {
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El carrito está vacío");
            return;
        }

        // Validar stock actualizado
        for (CartItem item : cart) {
            Component fresh = DatabaseManager.getInstance().findComponent(
                item.component.getTipo(), item.component.getValor());
            if (fresh.getCantidad() < item.requestedQuantity) {
                JOptionPane.showMessageDialog(this,
                "Stock insuficiente para: " + item.component.getTipo() + " " + item.component.getValor());
                return;
            }
        }

        // Dispensar
        for (CartItem item : cart) {
            DatabaseManager.getInstance().dispenseComponent(item.component.getId(), item.requestedQuantity);
        }

        JOptionPane.showMessageDialog(this,
            "¡Dispensación completada con éxito!\nSe han dispensado " + cart.size() + " referencias.",
            "Éxito", JOptionPane.INFORMATION_MESSAGE);

        cart.clear();
        refreshCart();
        loadInventory(); // Actualiza stock en tiempo real
    }

    // Para que solo haya una ventana abierta
    public static void open() {
        SwingUtilities.invokeLater(() -> {
            new DispenseFrame().setVisible(true);
        });
    }
}