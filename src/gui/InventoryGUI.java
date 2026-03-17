package gui;

import utils.FileHandler;
import java.util.List;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class InventoryGUI extends JPanel {

    private JTable table;
    private DefaultTableModel model;
    private JTextField nameField, quantityField;
    private List<String[]> items;

    public InventoryGUI() {
        setLayout(new BorderLayout(10, 10));

        // ===== FORM =====
        JPanel form = new JPanel(new GridLayout(2, 2, 10, 10));
        form.setBorder(BorderFactory.createTitledBorder("Add Product"));

        form.add(new JLabel("Product Name:"));
        nameField = new JTextField();
        form.add(nameField);

        form.add(new JLabel("Quantity:"));
        quantityField = new JTextField();
        form.add(quantityField);

        JButton addBtn = new JButton("Add Product");

        JPanel top = new JPanel(new BorderLayout());
        top.add(form, BorderLayout.CENTER);
        top.add(addBtn, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);

        // ===== TABLE =====
        model = new DefaultTableModel(new String[]{"ID", "Product", "Quantity"}, 0);
        table = new JTable(model);

        items = new ArrayList<>(FileHandler.loadInventory());

        // Load existing data into table
        for (String[] row : items) {
            model.addRow(row);
        }

        add(new JScrollPane(table), BorderLayout.CENTER);

        // ===== ACTION =====
        addBtn.addActionListener(e -> {
            String name = nameField.getText();
            String qty = quantityField.getText();

            if (name.isEmpty() || qty.isEmpty()) return;

            String[] row = new String[]{
                    String.valueOf(items.size() + 1),
                    name,
                    qty
            };

            items.add(row);
            FileHandler.saveInventory(items);

            model.addRow(row);

            nameField.setText("");
            quantityField.setText("");
        });
    }
}