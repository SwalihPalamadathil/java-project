package com.shop.dao;

import com.shop.model.SalaryRecord;
import com.shop.util.DBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of SalaryRecordDao.
 */
public class SalaryRecordDaoImpl implements SalaryRecordDao {

    private static final Logger logger = LoggerFactory.getLogger(SalaryRecordDaoImpl.class);

    private static final String BASE_SELECT =
            "SELECT sr.*, e.name AS employee_name FROM salary_records sr "
            + "JOIN employees e ON sr.employee_id = e.id ";

    @Override
    public boolean saveOrUpdate(SalaryRecord record) {
        String sql = "INSERT INTO salary_records (employee_id, month_year, base_salary, absence_deduction, "
                + "overtime_pay, bonus, advance_deduction, net_salary, payment_date, payment_status, payment_method, remarks) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT(employee_id, month_year) DO UPDATE SET "
                + "base_salary = excluded.base_salary, absence_deduction = excluded.absence_deduction, "
                + "overtime_pay = excluded.overtime_pay, bonus = excluded.bonus, advance_deduction = excluded.advance_deduction, "
                + "net_salary = excluded.net_salary, payment_date = excluded.payment_date, payment_status = excluded.payment_status, "
                + "payment_method = excluded.payment_method, remarks = excluded.remarks";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, record.getEmployeeId());
            ps.setString(2, record.getMonthYear());
            ps.setDouble(3, record.getBaseSalary());
            ps.setDouble(4, record.getAbsenceDeduction());
            ps.setDouble(5, record.getOvertimePay());
            ps.setDouble(6, record.getBonus());
            ps.setDouble(7, record.getAdvanceDeduction());
            ps.setDouble(8, record.getNetSalary());
            ps.setString(9, record.getPaymentDate());
            ps.setString(10, record.getPaymentStatus());
            ps.setString(11, record.getPaymentMethod());
            ps.setString(12, record.getRemarks());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        record.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error saving salary record: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public Optional<SalaryRecord> findById(int id) {
        String sql = BASE_SELECT + "WHERE sr.id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToSalaryRecord(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding salary record by id {}: {}", id, e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<SalaryRecord> findByEmployeeAndMonth(int employeeId, String monthYear) {
        String sql = BASE_SELECT + "WHERE sr.employee_id = ? AND sr.month_year = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            ps.setString(2, monthYear);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToSalaryRecord(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding salary record: {}", e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<SalaryRecord> findByMonth(String monthYear) {
        List<SalaryRecord> list = new ArrayList<>();
        String sql = BASE_SELECT + "WHERE sr.month_year = ? ORDER BY e.name ASC";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, monthYear);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToSalaryRecord(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding salary records by month: {}", e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<SalaryRecord> findAll() {
        List<SalaryRecord> list = new ArrayList<>();
        String sql = BASE_SELECT + "ORDER BY sr.month_year DESC, e.name ASC";
        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToSalaryRecord(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding all salary records: {}", e.getMessage(), e);
        }
        return list;
    }

    @Override
    public boolean markAsPaid(int recordId, String paymentDate, String paymentMethod) {
        String sql = "UPDATE salary_records SET payment_status = 'PAID', payment_date = ?, payment_method = ? WHERE id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, paymentDate);
            ps.setString(2, paymentMethod);
            ps.setInt(3, recordId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error marking salary as paid: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public double getTotalPayrollExpenseByMonth(String monthYear) {
        String sql = "SELECT SUM(net_salary) FROM salary_records WHERE month_year = ? AND payment_status = 'PAID'";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, monthYear);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error calculating monthly payroll expense: {}", e.getMessage(), e);
        }
        return 0.0;
    }

    private SalaryRecord mapResultSetToSalaryRecord(ResultSet rs) throws SQLException {
        SalaryRecord sr = new SalaryRecord();
        sr.setId(rs.getInt("id"));
        sr.setEmployeeId(rs.getInt("employee_id"));
        sr.setEmployeeName(rs.getString("employee_name"));
        sr.setMonthYear(rs.getString("month_year"));
        sr.setBaseSalary(rs.getDouble("base_salary"));
        sr.setAbsenceDeduction(rs.getDouble("absence_deduction"));
        sr.setOvertimePay(rs.getDouble("overtime_pay"));
        sr.setBonus(rs.getDouble("bonus"));
        sr.setAdvanceDeduction(rs.getDouble("advance_deduction"));
        sr.setNetSalary(rs.getDouble("net_salary"));
        sr.setPaymentDate(rs.getString("payment_date"));
        sr.setPaymentStatus(rs.getString("payment_status"));
        sr.setPaymentMethod(rs.getString("payment_method"));
        sr.setRemarks(rs.getString("remarks"));
        return sr;
    }
}
