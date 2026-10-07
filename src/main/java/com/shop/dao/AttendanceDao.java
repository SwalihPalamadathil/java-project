package com.shop.dao;

import com.shop.model.Attendance;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for Employee Daily Attendance.
 */
public interface AttendanceDao {
    boolean saveOrUpdate(Attendance attendance);
    Optional<Attendance> findByEmployeeAndDate(int employeeId, String date);
    List<Attendance> findByDate(String date);
    List<Attendance> findByEmployeeAndMonth(int employeeId, String monthYear);
    int countAbsencesInMonth(int employeeId, String monthYear);
    int countHalfDaysInMonth(int employeeId, String monthYear);
}
