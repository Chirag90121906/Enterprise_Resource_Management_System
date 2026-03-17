package gui;

import models.Employee;
import utils.FileHandler;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.ListSelectionModel;
import java.awt.*;
import java.util.ArrayList;

public class EmployeeGUI extends JPanel {

    private JTextField nameField, salaryField;
    private DefaultTableModel tableModel;
    private ArrayList<Employee> employees;
    private int idCounter = 1;
    private JTextField searchField;
    private JTable table;

    public EmployeeGUI() {

        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(245, 245, 245));

        employees = FileHandler.loadEmployees();

        // ===== FORM PANEL =====
        JPanel formPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createTitledBorder("Add Employee"));
        formPanel.setBackground(Color.WHITE);

        formPanel.add(new JLabel("Name:"));
        nameField = new JTextField();
        formPanel.add(nameField);

        formPanel.add(new JLabel("Salary:"));
        salaryField = new JTextField();
        formPanel.add(salaryField);

        JButton addBtn = new JButton("Add Employee");
        JButton deleteBtn = new JButton("Delete Selected");

        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.setBackground(new Color(245, 245, 245));
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addBtn);
        buttonPanel.add(deleteBtn);

        topContainer.add(formPanel, BorderLayout.CENTER);
        topContainer.add(buttonPanel, BorderLayout.SOUTH);

        add(topContainer, BorderLayout.NORTH);

        // ===== TABLE =====
        String[] cols = {"ID", "Name", "Salary"};
        tableModel = new DefaultTableModel(cols, 0);
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table.setRowHeight(25);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        table.getTableHeader().setBackground(new Color(70, 130, 180));
        table.getTableHeader().setForeground(Color.WHITE);

        add(new JScrollPane(table), BorderLayout.CENTER);

        // ===== LOAD DATA =====
        for (Employee emp : employees) {
            tableModel.addRow(new Object[]{
                    emp.getId(),
                    emp.getName(),
                    emp.getSalary()
            });

            if (emp.getId() >= idCounter) {
                idCounter = emp.getId() + 1;
            }
        }

        // ===== SEARCH PANEL =====
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchField = new JTextField();
        JButton searchBtn = new JButton("Search");
        JButton clearBtn = new JButton("Clear");

        JPanel searchActions = new JPanel();
        searchActions.add(searchBtn);
        searchActions.add(clearBtn);

        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchActions, BorderLayout.EAST);

        add(searchPanel, BorderLayout.SOUTH);

        // ===== BUTTON ACTION =====
        addBtn.addActionListener(e -> addEmployee());
        deleteBtn.addActionListener(e -> deleteEmployee());
        searchBtn.addActionListener(e -> filterEmployees());
        clearBtn.addActionListener(e -> {
            searchField.setText("");
            reloadTable(employees);
        });
    }

    private void addEmployee() {

        String name = nameField.getText();
        String salaryText = salaryField.getText();

        try {
            double salary = Double.parseDouble(salaryText);

            Employee emp = new Employee(idCounter++, name, salary);
            employees.add(emp);

            tableModel.addRow(new Object[]{
                    emp.getId(),
                    emp.getName(),
                    emp.getSalary()
            });

            FileHandler.saveEmployees(employees);

            nameField.setText("");
            salaryField.setText("");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid Input!");
        }
    }

    private void filterEmployees() {
        String q = searchField.getText().toLowerCase();
        ArrayList<Employee> filtered = new ArrayList<>();

        for (Employee e : employees) {
            if (e.getName().toLowerCase().contains(q) ||
                String.valueOf(e.getId()).contains(q) ||
                String.valueOf(e.getSalary()).contains(q)) {
                filtered.add(e);
            }
        }

        reloadTable(filtered);
    }

    private void reloadTable(ArrayList<Employee> list) {
        tableModel.setRowCount(0);
        for (Employee emp : list) {
            tableModel.addRow(new Object[]{
                    emp.getId(),
                    emp.getName(),
                    emp.getSalary()
            });
        }
    }

    private void deleteEmployee() {
        int selectedRow = table.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select an employee to delete!");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete this employee?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            employees.remove(selectedRow);
            FileHandler.saveEmployees(employees);
            reloadTable(employees);
        }
    }
}