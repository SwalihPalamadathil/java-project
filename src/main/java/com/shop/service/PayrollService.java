package com.shop.service;

import com.shop.dao.AttendanceDao;
import com.shop.dao.AttendanceDaoImpl;
import com.shop.dao.EmployeeDao;
import com.shop.dao.EmployeeDaoImpl;
import com.shop.dao.SalaryRecordDao;
import com.shop.dao.SalaryRecordDaoImpl;
import com.shop.dao.SettingDao;
import com.shop.dao.SettingDaoImpl;
import com.shop.model.Attendance;
import com.shop.model.Employee;
import com.shop.model.SalaryRecord;
import com.shop.util.DateUtil;
import com.shop.util.PDFGenerator;
import com.shop.util.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service managing Employee HR, Daily Attendance, Monthly Salary Calculations, and Payslips.
 */
public class PayrollService {

    private static final Logger logger = LoggerFactory.getLogger(PayrollService.class);

    private final EmployeeDao employeeDao;
    private final AttendanceDao attendanceDao;
    private final SalaryRecordDao salaryRecordDao;
    private final SettingDao settingDao;

    public PayrollService() {
        this(new EmployeeDaoImpl(), new AttendanceDaoImpl(), new SalaryRecordDaoImpl(), new SettingDaoImpl());
    }

    public PayrollService(EmployeeDao employeeDao, AttendanceDao attendanceDao,
                          SalaryRecordDao salaryRecordDao, SettingDao settingDao) {
        this.employeeDao = employeeDao;
        this.attendanceDao = attendanceDao;
        this.salaryRecordDao = salaryRecordDao;
        this.settingDao = settingDao;
    }

    // ==========================================
    // EMPLOYEE OPERATIONS
    // ==========================================

    public List<Employee> getAllEmployees() {
        return employeeDao.findAll();
    }

    public List<Employee> getActiveEmployees() {
        return employeeDao.findAllActive();
    }

    public Optional<Employee> getEmployeeById(int id) {
        return employeeDao.findById(id);
    }

    public boolean saveEmployee(Employee emp) {
        if (!Validator.isNotEmpty(emp.getName())) {
            throw new IllegalArgumentException("Employee name is required.");
        }
        if (!Validator.isValidPhone(emp.getPhone())) {
            throw new IllegalArgumentException("Valid phone number is required.");
        }
        if (!Validator.isNotEmpty(emp.getDesignation())) {
            throw new IllegalArgumentException("Designation is required.");
        }
        if (emp.getBaseSalary() < 0) {
            throw new IllegalArgumentException("Base salary cannot be negative.");
        }

        if (emp.getId() == 0) {
            return employeeDao.create(emp);
        } else {
            return employeeDao.update(emp);
        }
    }

    public boolean deactivateEmployee(int id) {
        return employeeDao.delete(id);
    }

    // ==========================================
    // ATTENDANCE OPERATIONS
    // ==========================================

    public boolean markAttendance(int employeeId, String date, String status, String remarks) {
        Attendance a = new Attendance();
        a.setEmployeeId(employeeId);
        a.setDate(date != null ? date : DateUtil.todayString());
        a.setStatus(status != null ? status : "PRESENT");
        a.setRemarks(remarks);
        return attendanceDao.saveOrUpdate(a);
    }

    public List<Attendance> getAttendanceByDate(String date) {
        return attendanceDao.findByDate(date);
    }

    public List<Attendance> getAttendanceByEmployeeMonth(int employeeId, String monthYear) {
        return attendanceDao.findByEmployeeAndMonth(employeeId, monthYear);
    }

    // ==========================================
    // PAYROLL & SALARY CALCULATIONS
    // ==========================================

    /**
     * Calculates monthly salary for an employee according to attendance:
     * Daily Rate = Base Salary / 30.
     * Absence Deduction = Absences * Daily Rate + (Half Days * Daily Rate * 0.5)
     * Net Salary = Base Salary - Absence Deduction + Overtime + Bonus - Advance
     */
    public SalaryRecord calculateMonthlySalary(int employeeId, String monthYear, double overtimePay,
                                               double bonus, double advanceDeduction, String remarks) {
        Employee emp = employeeDao.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found with id: " + employeeId));

        double baseSalary = emp.getBaseSalary();
        double dailyRate = baseSalary / 30.0;

        int absences = attendanceDao.countAbsencesInMonth(employeeId, monthYear);
        int halfDays = attendanceDao.countHalfDaysInMonth(employeeId, monthYear);

        double absenceDeduction = (absences * dailyRate) + (halfDays * dailyRate * 0.5);

        SalaryRecord record = new SalaryRecord();
        record.setEmployeeId(employeeId);
        record.setEmployeeName(emp.getName());
        record.setMonthYear(monthYear);
        record.setBaseSalary(baseSalary);
        record.setAbsenceDeduction(absenceDeduction);
        record.setOvertimePay(overtimePay);
        record.setBonus(bonus);
        record.setAdvanceDeduction(advanceDeduction);
        record.setRemarks(remarks);
        record.calculateNetSalary();

        return record;
    }

    public boolean saveSalaryRecord(SalaryRecord record) {
        return salaryRecordDao.saveOrUpdate(record);
    }

    public boolean markSalaryPaid(int recordId, String paymentMethod) {
        String today = LocalDate.now().toString();
        return salaryRecordDao.markAsPaid(recordId, today, paymentMethod != null ? paymentMethod : "CASH");
    }

    public List<SalaryRecord> getSalaryRecordsByMonth(String monthYear) {
        return salaryRecordDao.findByMonth(monthYear);
    }

    public List<SalaryRecord> getAllSalaryRecords() {
        return salaryRecordDao.findAll();
    }

    public void generatePayslip(int salaryRecordId, File outputFile) throws Exception {
        SalaryRecord sr = salaryRecordDao.findById(salaryRecordId)
                .orElseThrow(() -> new IllegalArgumentException("Salary record not found."));
        Employee emp = employeeDao.findById(sr.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found."));
        Map<String, String> settings = settingDao.getAll();

        PDFGenerator.generatePayslipPdf(sr, emp, settings, outputFile);
    }
}
