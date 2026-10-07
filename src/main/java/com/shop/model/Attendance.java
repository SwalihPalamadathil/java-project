package com.shop.model;

/**
 * POJO representing daily attendance of an employee.
 */
public class Attendance {
    private int id;
    private int employeeId;
    private String employeeName; // Helper field for UI display
    private String date; // YYYY-MM-DD
    private String status; // PRESENT, ABSENT, HALF_DAY, LEAVE
    private String remarks;

    public Attendance() {}

    public Attendance(int id, int employeeId, String employeeName, String date, String status, String remarks) {
        this.id = id;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.date = date;
        this.status = status;
        this.remarks = remarks;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
