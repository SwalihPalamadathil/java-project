package com.shop.dao;

import com.shop.model.Employee;
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
 * JDBC implementation of EmployeeDao.
 */
public class EmployeeDaoImpl implements EmployeeDao {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeDaoImpl.class);

    @Override
    public Optional<Employee> findById(int id) {
        String sql = "SELECT * FROM employees WHERE id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToEmployee(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding employee by id {}: {}", id, e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Employee> findAll() {
        List<Employee> list = new ArrayList<>();
        String sql = "SELECT * FROM employees ORDER BY name ASC";
        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToEmployee(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching all employees: {}", e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Employee> findAllActive() {
        List<Employee> list = new ArrayList<>();
        String sql = "SELECT * FROM employees WHERE status = 'ACTIVE' ORDER BY name ASC";
        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToEmployee(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching active employees: {}", e.getMessage(), e);
        }
        return list;
    }

    @Override
    public boolean create(Employee employee) {
        String sql = "INSERT INTO employees (name, phone, address, designation, role, joining_date, base_salary, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, employee.getName());
            ps.setString(2, employee.getPhone());
            ps.setString(3, employee.getAddress());
            ps.setString(4, employee.getDesignation());
            ps.setString(5, employee.getRole());
            ps.setString(6, employee.getJoiningDate());
            ps.setDouble(7, employee.getBaseSalary());
            ps.setString(8, employee.getStatus());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        employee.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error creating employee {}: {}", employee.getName(), e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean update(Employee employee) {
        String sql = "UPDATE employees SET name = ?, phone = ?, address = ?, designation = ?, "
                + "role = ?, joining_date = ?, base_salary = ?, status = ? WHERE id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, employee.getName());
            ps.setString(2, employee.getPhone());
            ps.setString(3, employee.getAddress());
            ps.setString(4, employee.getDesignation());
            ps.setString(5, employee.getRole());
            ps.setString(6, employee.getJoiningDate());
            ps.setDouble(7, employee.getBaseSalary());
            ps.setString(8, employee.getStatus());
            ps.setInt(9, employee.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating employee {}: {}", employee.getId(), e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean delete(int id) {
        // Soft deactivate or delete
        String sql = "UPDATE employees SET status = 'INACTIVE' WHERE id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deactivating employee {}: {}", id, e.getMessage(), e);
        }
        return false;
    }

    @Override
    public int getTotalActiveCount() {
        String sql = "SELECT COUNT(*) FROM employees WHERE status = 'ACTIVE'";
        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error counting active employees: {}", e.getMessage(), e);
        }
        return 0;
    }

    private Employee mapResultSetToEmployee(ResultSet rs) throws SQLException {
        return new Employee(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("phone"),
                rs.getString("address"),
                rs.getString("designation"),
                rs.getString("role"),
                rs.getString("joining_date"),
                rs.getDouble("base_salary"),
                rs.getString("status")
        );
    }
}
