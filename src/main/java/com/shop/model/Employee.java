package com.shop.model;

/**
 * POJO representing an Employee in the shop.
 */
public class Employee {
    private int id;
    private String name;
    private String phone;
    private String address;
    private String designation;
    private String role; // ADMIN or STAFF
    private String joiningDate; // YYYY-MM-DD
    private double baseSalary;
    private String status; // ACTIVE or INACTIVE

    public Employee() {
        this.status = "ACTIVE";
        this.role = "STAFF";
    }

    public Employee(int id, String name, String phone, String address, String designation,
                    String role, String joiningDate, double baseSalary, String status) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.address = address;
        this.designation = designation;
        this.role = role;
        this.joiningDate = joiningDate;
        this.baseSalary = baseSalary;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getJoiningDate() { return joiningDate; }
    public void setJoiningDate(String joiningDate) { this.joiningDate = joiningDate; }

    public double getBaseSalary() { return baseSalary; }
    public void setBaseSalary(double baseSalary) { this.baseSalary = baseSalary; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(this.status);
    }

    @Override
    public String toString() {
        return name + " (" + designation + ")";
    }
}
