package models;

public class Manager extends Employee {

    int teamSize;

    public Manager(int id, String name, double salary, int teamSize) {
        super(id, name, salary);
        this.teamSize = teamSize;
    }

}