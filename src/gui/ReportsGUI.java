package gui;

import javax.swing.*;
import java.awt.*;
import utils.FileHandler;
import java.util.List;

public class ReportsGUI extends JPanel {

    public ReportsGUI() {
        setLayout(new BorderLayout());

        JLabel title = new JLabel("Business Reports", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));

        JTextArea reportArea = new JTextArea();

        // Load real data
        List<String[]> inventory = FileHandler.loadInventory();
        List<String[]> sales = FileHandler.loadSales();

        // Calculate totals
        double totalRevenue = 0;
        for (String[] s : sales) {
            try {
                totalRevenue += Double.parseDouble(s[2]);
            } catch (Exception e) {
                // ignore invalid data
            }
        }

        reportArea.setText(
                "Reports Overview:\n\n" +
                "- Total Products: " + inventory.size() + "\n" +
                "- Total Sales Entries: " + sales.size() + "\n" +
                "- Total Revenue: ₹" + totalRevenue + "\n"
        );

        reportArea.setEditable(false);

        add(title, BorderLayout.NORTH);
        add(new JScrollPane(reportArea), BorderLayout.CENTER);
    }
}