package gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import utils.FileHandler;
import java.util.List;
import java.util.ArrayList;

public class SalesGUI extends JPanel {

    private JTable table;
    private DefaultTableModel model;
    private JTextField productField, amountField;
    private List<String[]> sales;

    public SalesGUI() {
        setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridLayout(2, 2, 10, 10));
        form.setBorder(BorderFactory.createTitledBorder("Record Sale"));

        form.add(new JLabel("Product:"));
        productField = new JTextField();
        form.add(productField);

        form.add(new JLabel("Amount:"));
        amountField = new JTextField();
        form.add(amountField);

        JButton addBtn = new JButton("Add Sale");

        JPanel top = new JPanel(new BorderLayout());
        top.add(form, BorderLayout.CENTER);
        top.add(addBtn, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);

        model = new DefaultTableModel(new String[]{"ID", "Product", "Amount"}, 0);
        table = new JTable(model);

        sales = new ArrayList<>(FileHandler.loadSales());

        // Load existing sales into table
        for (String[] row : sales) {
            model.addRow(row);
        }

        add(new JScrollPane(table), BorderLayout.CENTER);

        addBtn.addActionListener(e -> {
            String product = productField.getText();
            String amount = amountField.getText();

            if (product.isEmpty() || amount.isEmpty()) return;

            String[] row = new String[]{
                    String.valueOf(sales.size() + 1),
                    product,
                    amount
            };

            sales.add(row);
            FileHandler.saveSales(sales);

            model.addRow(row);

            productField.setText("");
            amountField.setText("");
        });
    }
}