package services;

import models.Employee;
import utils.FileHandler;

import java.util.ArrayList;
import java.util.List;

public class EmployeeService {

    public static List<Employee> getAllEmployees() {
        return FileHandler.loadEmployees();
    }

    public static void addEmployee(String name, double salary) {
        List<Employee> employees = FileHandler.loadEmployees();
        int id = employees.size() + 1;

        Employee emp = new Employee(id, name, salary);
        employees.add(emp);

        FileHandler.saveEmployees((ArrayList<Employee>) employees);
    }

    public static void deleteEmployee(int id) {
        List<Employee> employees = FileHandler.loadEmployees();

        employees.removeIf(emp -> emp.getId() == id);

        FileHandler.saveEmployees((ArrayList<Employee>) employees);
    }

    public static List<Employee> searchEmployees(String query) {
        List<Employee> employees = FileHandler.loadEmployees();
        List<Employee> filtered = new ArrayList<>();

        query = query.toLowerCase();

        for (Employee e : employees) {
            if (e.getName().toLowerCase().contains(query) ||
                String.valueOf(e.getId()).contains(query) ||
                String.valueOf(e.getSalary()).contains(query)) {
                filtered.add(e);
            }
        }

        return filtered;
    }
}