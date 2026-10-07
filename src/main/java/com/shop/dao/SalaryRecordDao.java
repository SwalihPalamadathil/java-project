package com.shop.dao;

import com.shop.model.SalaryRecord;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for monthly Salary records.
 */
public interface SalaryRecordDao {
    boolean saveOrUpdate(SalaryRecord record);
    Optional<SalaryRecord> findById(int id);
    Optional<SalaryRecord> findByEmployeeAndMonth(int employeeId, String monthYear);
    List<SalaryRecord> findByMonth(String monthYear);
    List<SalaryRecord> findAll();
    boolean markAsPaid(int recordId, String paymentDate, String paymentMethod);
    double getTotalPayrollExpenseByMonth(String monthYear);
}
