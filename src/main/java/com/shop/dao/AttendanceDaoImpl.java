package com.shop.dao;

import com.shop.model.Attendance;
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
 * JDBC implementation of AttendanceDao using PreparedStatements.
 */
public class AttendanceDaoImpl implements AttendanceDao {

    private static final Logger logger = LoggerFactory.getLogger(AttendanceDaoImpl.class);

    @Override
    public boolean saveOrUpdate(Attendance attendance) {
        String sql = "INSERT INTO attendance (employee_id, date, status, remarks) VALUES (?, ?, ?, ?) "
                + "ON CONFLICT(employee_id, date) DO UPDATE SET status = excluded.status, remarks = excluded.remarks";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, attendance.getEmployeeId());
            ps.setString(2, attendance.getDate());
            ps.setString(3, attendance.getStatus());
            ps.setString(4, attendance.getRemarks());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        attendance.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error saving attendance for emp {}: {}", attendance.getEmployeeId(), e.getMessage(), e);
        }
        return false;
    }

    @Override
    public Optional<Attendance> findByEmployeeAndDate(int employeeId, String date) {
        String sql = "SELECT a.*, e.name AS employee_name FROM attendance a "
                + "JOIN employees e ON a.employee_id = e.id "
                + "WHERE a.employee_id = ? AND a.date = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            ps.setString(2, date);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAttendance(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding attendance: {}", e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Attendance> findByDate(String date) {
        List<Attendance> list = new ArrayList<>();
        String sql = "SELECT a.*, e.name AS employee_name FROM attendance a "
                + "JOIN employees e ON a.employee_id = e.id "
                + "WHERE a.date = ? ORDER BY e.name ASC";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToAttendance(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding attendance by date: {}", e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Attendance> findByEmployeeAndMonth(int employeeId, String monthYear) {
        List<Attendance> list = new ArrayList<>();
        String sql = "SELECT a.*, e.name AS employee_name FROM attendance a "
                + "JOIN employees e ON a.employee_id = e.id "
                + "WHERE a.employee_id = ? AND a.date LIKE ? ORDER BY a.date ASC";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            ps.setString(2, monthYear + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToAttendance(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding attendance by employee month: {}", e.getMessage(), e);
        }
        return list;
    }

    @Override
    public int countAbsencesInMonth(int employeeId, String monthYear) {
        String sql = "SELECT COUNT(*) FROM attendance WHERE employee_id = ? AND date LIKE ? AND status = 'ABSENT'";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            ps.setString(2, monthYear + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error counting absences: {}", e.getMessage(), e);
        }
        return 0;
    }

    @Override
    public int countHalfDaysInMonth(int employeeId, String monthYear) {
        String sql = "SELECT COUNT(*) FROM attendance WHERE employee_id = ? AND date LIKE ? AND status = 'HALF_DAY'";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            ps.setString(2, monthYear + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error counting half days: {}", e.getMessage(), e);
        }
        return 0;
    }

    private Attendance mapResultSetToAttendance(ResultSet rs) throws SQLException {
        Attendance a = new Attendance();
        a.setId(rs.getInt("id"));
        a.setEmployeeId(rs.getInt("employee_id"));
        a.setEmployeeName(rs.getString("employee_name"));
        a.setDate(rs.getString("date"));
        a.setStatus(rs.getString("status"));
        a.setRemarks(rs.getString("remarks"));
        return a;
    }
}
