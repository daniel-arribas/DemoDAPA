package com.poc.armario;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
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
        setTitle("ARMARIO INTELIGENTE - Dispensación de Componentes Electrónicos");
        setExtendedState(JFrame.MAXIMIZED_BOTH);  // Pantalla completa opcional
        setSize(1400, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // === PANEL PRINCIPAL: Split horizontal ===
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(800);
        splitPane.setResizeWeight(0.6);

        // === IZQUIERDA: INVENTARIO ===
        JPanel leftPanel = createInventoryPanel();
        splitPane.setLeftComponent(leftPanel);

        // === DERECHA: CARRITO ===
        JPanel rightPanel = createCartPanel();
        splitPane.setRightComponent(rightPanel);

        add(splitPane, BorderLayout.CENTER);

        // === BARRA INFERIOR: Info y botón grande ===
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JLabel statusLabel = new JLabel(" Arrastra un archivo .txt/.cir aquí o haz doble click en un componente", JLabel.CENTER);
        statusLabel.setForeground(Color.BLUE);
        statusLabel.setFont(new Font("Arial", Font.ITALIC, 14));
        bottomPanel.add(statusLabel, BorderLayout.NORTH);

        JButton confirmBtn = new JButton("CONFIRMAR Y DISPENSAR TODOS LOS COMPONENTES");
        confirmBtn.setFont(new Font("Arial", Font.BOLD, 22));
        confirmBtn.setBackground(new Color(0, 140, 0));
        confirmBtn.setForeground(Color.WHITE);
        confirmBtn.setFocusPainted(false);
        confirmBtn.setPreferredSize(new Dimension(0, 80));
        confirmBtn.addActionListener(e -> confirmDispense());
        bottomPanel.add(confirmBtn, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);

        // Cargar datos
        loadInventory();

        // Activar drag & drop en toda la ventana
        enableDragAndDropOnFrame();

        setVisible(true);
    }

    private JPanel createInventoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(0, 120, 215), 3),
            " INVENTARIO DISPONIBLE (Doble click para añadir)"));

        inventoryModel = new DefaultTableModel(
            new String[]{"ID", "Tipo", "Valor", "Stock", "Ubicación"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        inventoryTable = new JTable(inventoryModel);
        inventoryTable.setRowHeight(30);
        inventoryTable.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        inventoryTable.getTableHeader().setBackground(new Color(0, 120, 215));
        inventoryTable.getTableHeader().setForeground(Color.WHITE);

        // Doble click → añadir al carrito
        inventoryTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = inventoryTable.getSelectedRow();
                    if (row != -1) addComponentFromInventory(row);
                }
            }
        });

        panel.add(new JScrollPane(inventoryTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createCartPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(0, 150, 0), 3),
            " CARRITO DE DISPENSACIÓN"));

        cartModel = new DefaultTableModel(
            new String[]{"ID", "Tipo", "Valor", "Cantidad", "Stock"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 3; }
        };

        cartTable = new JTable(cartModel);
        cartTable.setRowHeight(30);
        cartTable.getTableHeader().setBackground(new Color(0, 150, 0));
        cartTable.getTableHeader().setForeground(Color.WHITE);

        panel.add(new JScrollPane(cartTable), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout());
        JButton remove = new JButton("Eliminar");
        remove.addActionListener(e -> {
            int row = cartTable.getSelectedRow();
            if (row != -1) {
                cart.remove(row);
                refreshCart();
            }
        });
        JButton clear = new JButton("Vaciar");
        clear.addActionListener(e -> { cart.clear(); refreshCart(); });
        buttons.add(remove);
        buttons.add(clear);
        panel.add(buttons, BorderLayout.SOUTH);

        return panel;
    }

    private void addComponentFromInventory(int row) {
        String id = (String) inventoryModel.getValueAt(row, 0);
        String tipo = (String) inventoryModel.getValueAt(row, 1);
        String valor = (String) inventoryModel.getValueAt(row, 2);
        int stock = (Integer) inventoryModel.getValueAt(row, 3);

        String input = JOptionPane.showInputDialog(this,
            "<html><b>" + tipo + " " + valor + "</b><br>Stock: " + stock + "<br><br>Cantidad:</html>", "1");
        if (input == null || input.trim().isEmpty()) return;

        try {
            int qty = Integer.parseInt(input.trim());
            if (qty <= 0 || qty > stock) {
                JOptionPane.showMessageDialog(this, "Cantidad inválida o sin stock", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Component c = DatabaseManager.getInstance().findComponent(tipo, valor);
            if (c != null) {
                addToCart(c, qty);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Solo números", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void addToCart(Component c, int qty) {
        for (CartItem item : cart) {
            if (item.component.getId().equals(c.getId())) {
                item.requestedQuantity += qty;
                refreshCart();
                return;
            }
        }
        cart.add(new CartItem(c, qty));
        refreshCart();
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
                c.getId(), c.getTipo(), c.getValor(), c.getCantidad(), c.getUbicacion()
            });
        }
    }

    private void enableDragAndDropOnFrame() {
        setDropTarget(new DropTarget(this, DnDConstants.ACTION_COPY, new java.awt.dnd.DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> files = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

                    for (File file : files) {
                        String name = file.getName().toLowerCase();
                        if (name.endsWith(".txt") || name.endsWith(".cir") || name.endsWith(".net")) {
                            List<NetlistParser.ComponentRequest> reqs = NetlistParser.parse(file.getAbsolutePath());

                            int added = 0;
                            StringBuilder missing = new StringBuilder();

                            for (NetlistParser.ComponentRequest r : reqs) {
                                Component c = DatabaseManager.getInstance().findComponent(r.tipo, r.valor);
                                if (c != null && c.getCantidad() > 0) {
                                    addToCart(c, 1);
                                    added++;
                                } else {
                                    missing.append(r.tipo).append(" ").append(r.valor).append("\n");
                                }
                            }

                            String msg = "Netlist cargada: " + file.getName() + "\n" + added + " componentes añadidos";
                            if (missing.length() > 0) {
                                msg += "\n\nNo encontrados:\n" + missing;
                            }
                            JOptionPane.showMessageDialog(DispenseFrame.this, msg, "Resultado", JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(DispenseFrame.this, "Error al leer archivo", "Error", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }
        }));
    }

    private void confirmDispense() {
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Carrito vacío");
            return;
        }

        for (CartItem item : cart) {
            Component fresh = DatabaseManager.getInstance().findComponent(
                item.component.getTipo(), item.component.getValor());
            if (fresh == null || fresh.getCantidad() < item.requestedQuantity) {
                JOptionPane.showMessageDialog(this, "Sin stock: " + item.component.getTipo() + " " + item.component.getValor());
                return;
            }
        }

        for (CartItem item : cart) {
            DatabaseManager.getInstance().dispenseComponent(item.component.getId(), item.requestedQuantity);
        }

        JOptionPane.showMessageDialog(this,
            "¡Dispensación completada!\nSe han entregado " + cart.size() + " referencias.",
            "Éxito", JOptionPane.INFORMATION_MESSAGE);

        cart.clear();
        refreshCart();
        loadInventory();
    }

    // Lanzador
    public static void open() {
        SwingUtilities.invokeLater(() -> new DispenseFrame());
    }
}