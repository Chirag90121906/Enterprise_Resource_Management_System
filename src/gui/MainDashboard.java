package gui;

import javax.swing.*;
import java.awt.*;
import utils.FileHandler;
import java.util.List;

public class MainDashboard extends JFrame {

    private JPanel contentPanel;
    private JButton activeButton = null;

    public MainDashboard() {

        setTitle("Enterprise Resource Management System");
        setSize(1100, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // ===== GLOBAL STYLE =====
        UIManager.put("Label.font", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("Button.font", new Font("Segoe UI", Font.BOLD, 14));

        // ===== SIDEBAR =====
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new GridLayout(7, 1, 12, 12));
        sidebar.setBackground(new Color(28, 28, 28));
        sidebar.setPreferredSize(new Dimension(200, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 12, 20, 12));

        JLabel logo = new JLabel("ERMS", JLabel.CENTER);
        logo.setForeground(Color.WHITE);
        logo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        sidebar.add(logo);

        JButton btnDashboard = createButton("🏠 Dashboard");
        JButton btnEmployee = createButton("👥 Employees");
        JButton btnInventory = createButton("📦 Inventory");
        JButton btnSales = createButton("💰 Sales");
        JButton btnReports = createButton("📊 Reports");
        JButton btnExit = createButton("🚪 Exit");

        sidebar.add(btnDashboard);
        sidebar.add(btnEmployee);
        sidebar.add(btnInventory);
        sidebar.add(btnSales);
        sidebar.add(btnReports);
        sidebar.add(btnExit);

        // ===== CONTENT PANEL =====
        contentPanel = new JPanel(new BorderLayout());
        showDashboard();
        setActiveButton(btnDashboard);

        add(sidebar, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(30, 110, 200));
        topBar.setPreferredSize(new Dimension(100, 50));

        JLabel title = new JLabel("Enterprise Resource Management System");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 10));

        topBar.add(title, BorderLayout.WEST);
        add(topBar, BorderLayout.NORTH);

        // ===== BUTTON ACTIONS =====
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
        dashboard.setBackground(new Color(245, 247, 250));

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
            activeButton.setBackground(new Color(50, 50, 50));
        }
        btn.setBackground(new Color(70, 130, 180));
        activeButton = btn;
    }

    private JButton createButton(String text) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false);
        btn.setBackground(new Color(50, 50, 50));
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 10));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (btn != activeButton) {
                    btn.setBackground(new Color(70, 130, 180));
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (btn != activeButton) {
                    btn.setBackground(new Color(50, 50, 50));
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

            // shadow
            g2.setColor(new Color(0,0,0,30));
            g2.fillRoundRect(5,5,getWidth()-10,getHeight()-10,20,20);

            // card
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0,0,getWidth()-5,getHeight()-5,20,20);
        }
    };

    card.setLayout(new BorderLayout());
    card.setOpaque(false);
    card.addMouseListener(new java.awt.event.MouseAdapter() {
        public void mouseEntered(java.awt.event.MouseEvent evt) {
            card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }
    });
    card.setBorder(BorderFactory.createEmptyBorder(25,25,25,25));

    JLabel t = new JLabel(title);
    t.setFont(new Font("Segoe UI", Font.BOLD, 14));
    t.setForeground(new Color(120,120,120));

    JLabel v = new JLabel(value);
    v.setFont(new Font("Segoe UI", Font.BOLD, 28));
    v.setForeground(new Color(45,120,200));

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