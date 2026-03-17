package utils;

import models.Employee;

import java.io.*;
import java.util.ArrayList;

public class FileHandler {

    private static final String FILE_NAME = "employees.txt";

    // SAVE EMPLOYEES
    public static void saveEmployees(ArrayList<Employee> employees) {

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_NAME))) {

            for (Employee emp : employees) {
                writer.write(emp.getId() + "," + emp.getName() + "," + emp.getSalary());
                writer.newLine();
            }

        } catch (IOException e) {
            System.out.println("Error saving file");
        }
    }

    // LOAD EMPLOYEES
    public static ArrayList<Employee> loadEmployees() {

        ArrayList<Employee> employees = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {

            String line;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");

                int id = Integer.parseInt(parts[0]);
                String name = parts[1];
                double salary = Double.parseDouble(parts[2]);

                employees.add(new Employee(id, name, salary));
            }

        } catch (IOException e) {
            System.out.println("No previous data found");
        }

        return employees;
    }
    // ===== INVENTORY =====
    public static void saveInventory(java.util.List<String[]> items) {
        try (java.io.PrintWriter writer = new java.io.PrintWriter("inventory.txt")) {
            for (String[] item : items) {
                writer.println(item[0] + "," + item[1] + "," + item[2]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static java.util.List<String[]> loadInventory() {
        java.util.List<String[]> items = new java.util.ArrayList<>();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader("inventory.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                items.add(line.split(","));
            }
        } catch (Exception e) {
            System.out.println("No inventory data");
        }
        return items;
    }

    // ===== SALES =====
    public static void saveSales(java.util.List<String[]> sales) {
        try (java.io.PrintWriter writer = new java.io.PrintWriter("sales.txt")) {
            for (String[] s : sales) {
                writer.println(s[0] + "," + s[1] + "," + s[2]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static java.util.List<String[]> loadSales() {
        java.util.List<String[]> sales = new java.util.ArrayList<>();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader("sales.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sales.add(line.split(","));
            }
        } catch (Exception e) {
            System.out.println("No sales data");
        }
        return sales;
    }
}