package com.shop.dao;

import com.shop.model.Employee;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for Employee management.
 */
public interface EmployeeDao {
    Optional<Employee> findById(int id);
    List<Employee> findAll();
    List<Employee> findAllActive();
    boolean create(Employee employee);
    boolean update(Employee employee);
    boolean delete(int id);
    int getTotalActiveCount();
}
