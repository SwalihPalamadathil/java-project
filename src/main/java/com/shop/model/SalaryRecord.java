package com.shop.model;

/**
 * POJO representing a monthly payroll record for an employee.
 */
public class SalaryRecord {
    private int id;
    private int employeeId;
    private String employeeName; // Helper field for UI display
    private String monthYear; // YYYY-MM
    private double baseSalary;
    private double absenceDeduction;
    private double overtimePay;
    private double bonus;
    private double advanceDeduction;
    private double netSalary;
    private String paymentDate; // YYYY-MM-DD
    private String paymentStatus; // PAID, UNPAID
    private String paymentMethod; // CASH, BANK_TRANSFER, CHEQUE
    private String remarks;

    public SalaryRecord() {
        this.paymentStatus = "UNPAID";
        this.paymentMethod = "CASH";
    }

    public SalaryRecord(int id, int employeeId, String employeeName, String monthYear,
                        double baseSalary, double absenceDeduction, double overtimePay,
                        double bonus, double advanceDeduction, double netSalary,
                        String paymentDate, String paymentStatus, String paymentMethod, String remarks) {
        this.id = id;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.monthYear = monthYear;
        this.baseSalary = baseSalary;
        this.absenceDeduction = absenceDeduction;
        this.overtimePay = overtimePay;
        this.bonus = bonus;
        this.advanceDeduction = advanceDeduction;
        this.netSalary = netSalary;
        this.paymentDate = paymentDate;
        this.paymentStatus = paymentStatus;
        this.paymentMethod = paymentMethod;
        this.remarks = remarks;
    }

    public void calculateNetSalary() {
        this.netSalary = Math.max(0.0, (baseSalary - absenceDeduction + overtimePay + bonus - advanceDeduction));
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getMonthYear() { return monthYear; }
    public void setMonthYear(String monthYear) { this.monthYear = monthYear; }

    public double getBaseSalary() { return baseSalary; }
    public void setBaseSalary(double baseSalary) {
        this.baseSalary = baseSalary;
        calculateNetSalary();
    }

    public double getAbsenceDeduction() { return absenceDeduction; }
    public void setAbsenceDeduction(double absenceDeduction) {
        this.absenceDeduction = absenceDeduction;
        calculateNetSalary();
    }

    public double getOvertimePay() { return overtimePay; }
    public void setOvertimePay(double overtimePay) {
        this.overtimePay = overtimePay;
        calculateNetSalary();
    }

    public double getBonus() { return bonus; }
    public void setBonus(double bonus) {
        this.bonus = bonus;
        calculateNetSalary();
    }

    public double getAdvanceDeduction() { return advanceDeduction; }
    public void setAdvanceDeduction(double advanceDeduction) {
        this.advanceDeduction = advanceDeduction;
        calculateNetSalary();
    }

    public double getNetSalary() { return netSalary; }
    public void setNetSalary(double netSalary) { this.netSalary = netSalary; }

    public String getPaymentDate() { return paymentDate; }
    public void setPaymentDate(String paymentDate) { this.paymentDate = paymentDate; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public boolean isPaid() {
        return "PAID".equalsIgnoreCase(this.paymentStatus);
    }
}
