package gui;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import utils.FileHandler;

public class MainDashboard extends JFrame {

    private JPanel contentPanel;
    private JButton activeButton = null;

    public MainDashboard() {

        setTitle("Enterprise Resource Management System");
        setSize(1100, 650);
        setMinimumSize(new Dimension(900, 580));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        UIManager.put("Label.font", new Font("Avenir Next", Font.PLAIN, 14));
        UIManager.put("Button.font", new Font("Avenir Next", Font.BOLD, 13));

        JPanel sidebar = new JPanel();
        sidebar.setLayout(new GridLayout(7, 1, 12, 12));
        sidebar.setBackground(Color.WHITE);
        sidebar.setPreferredSize(new Dimension(212, 0));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(229, 234, 230)),
            BorderFactory.createEmptyBorder(20, 12, 20, 12)));

        JLabel logo = new JLabel("ORBIT", JLabel.CENTER);
        logo.setForeground(new Color(29, 90, 67));
        logo.setFont(new Font("Avenir Next", Font.BOLD, 20));
        sidebar.add(logo);

        JButton btnDashboard = createButton("Overview");
        JButton btnEmployee = createButton("Employees");
        JButton btnInventory = createButton("Inventory");
        JButton btnSales = createButton("Sales");
        JButton btnReports = createButton("Reports");
        JButton btnExit = createButton("Exit");

        sidebar.add(btnDashboard);
        sidebar.add(btnEmployee);
        sidebar.add(btnInventory);
        sidebar.add(btnSales);
        sidebar.add(btnReports);
        sidebar.add(btnExit);

        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(new Color(245, 247, 244));
        showDashboard();
        setActiveButton(btnDashboard);

        add(sidebar, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(29, 90, 67));
        topBar.setPreferredSize(new Dimension(100, 62));

        JLabel title = new JLabel("Enterprise Resource Management System");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Avenir Next", Font.BOLD, 15));
        title.setBorder(BorderFactory.createEmptyBorder(10, 22, 10, 10));

        topBar.add(title, BorderLayout.WEST);
        add(topBar, BorderLayout.NORTH);

        btnDashboard.addActionListener(e -> {
            setActiveButton(btnDashboard);
            showDashboard();
        });
        btnEmployee.addActionListener(e -> {
            setActiveButton(btnEmployee);
            loadPanel(new EmployeeGUI());
        });
        btnInventory.addActionListener(e -> {
            setActiveButton(btnInventory);
            loadPanel(new InventoryGUI());
        });
        btnSales.addActionListener(e -> {
            setActiveButton(btnSales);
            loadPanel(new SalesGUI());
        });
        btnReports.addActionListener(e -> {
            setActiveButton(btnReports);
            loadPanel(new ReportsGUI());
        });
        btnExit.addActionListener(e -> System.exit(0));
    }

    private void showDashboard() {
        contentPanel.removeAll();

        // Load real data
        List<String[]> inventory = FileHandler.loadInventory();
        List<String[]> sales = FileHandler.loadSales();
        int employeeCount = FileHandler.loadEmployees().size();

        double totalRevenue = 0;
        for (String[] s : sales) {
            try {
                totalRevenue += Double.parseDouble(s[2]);
            } catch (Exception ignored) {}
        }

        JPanel dashboard = new JPanel(new GridLayout(2, 2, 30, 30));
        dashboard.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        dashboard.setBackground(new Color(245, 247, 244));

        dashboard.add(createCard("Employees", String.valueOf(employeeCount)));
        dashboard.add(createCard("Products", String.valueOf(inventory.size())));
        dashboard.add(createCard("Sales", String.valueOf(sales.size())));
        dashboard.add(createCard("Revenue", "₹" + totalRevenue));

        contentPanel.add(dashboard, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void loadPanel(JPanel panel) {
        contentPanel.removeAll();
        contentPanel.add(panel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void setActiveButton(JButton btn) {
        if (activeButton != null) {
            activeButton.setBackground(Color.WHITE);
            activeButton.setForeground(new Color(67, 84, 76));
        }
        btn.setBackground(new Color(29, 90, 67));
        btn.setForeground(Color.WHITE);
        activeButton = btn;
    }

    private JButton createButton(String text) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false);
        btn.setBackground(Color.WHITE);
        btn.setForeground(new Color(67, 84, 76));
        btn.setFont(new Font("Avenir Next", Font.BOLD, 13));
        btn.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 10));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (btn != activeButton) {
                    btn.setBackground(new Color(240, 246, 241));
                    btn.setForeground(new Color(29, 90, 67));
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (btn != activeButton) {
                    btn.setBackground(Color.WHITE);
                    btn.setForeground(new Color(67, 84, 76));
                }
            }
        });

        return btn;
    }

    private JPanel createCard(String title, String value) {

    JPanel card = new JPanel() {
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(new Color(25, 55, 39, 14));
            g2.fillRoundRect(3, 4, getWidth() - 7, getHeight() - 8, 14, 14);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 6, getHeight() - 7, 14, 14);
            g2.setColor(new Color(229, 234, 230));
            g2.drawRoundRect(0, 0, getWidth() - 6, getHeight() - 7, 14, 14);
        }
    };

    card.setLayout(new BorderLayout());
    card.setOpaque(false);
    card.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

    JLabel t = new JLabel(title);
    t.setFont(new Font("Avenir Next", Font.BOLD, 13));
    t.setForeground(new Color(105, 119, 110));

    JLabel v = new JLabel(value);
    v.setFont(new Font("Avenir Next", Font.BOLD, 28));
    v.setForeground(new Color(29, 90, 67));

    card.add(t, BorderLayout.NORTH);
    card.add(v, BorderLayout.CENTER);

    return card;
}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MainDashboard().setVisible(true);
        });
    }
}