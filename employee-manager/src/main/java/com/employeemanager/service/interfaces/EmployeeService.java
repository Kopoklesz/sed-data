package com.employeemanager.service.interfaces;

import com.employeemanager.model.Employee;
import com.employeemanager.model.WorkRecord;
import com.employeemanager.service.exception.ServiceException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EmployeeService extends BaseService<Employee, String> {
    Optional<Employee> findByTaxNumber(String taxNumber) throws ServiceException;
    Optional<Employee> findBySocialSecurityNumber(String ssn) throws ServiceException;
    boolean validateEmployee(Employee employee);

    default List<Employee> getAllEmployees() throws ServiceException {
        return findAll();
    }

    default Employee saveEmployee(Employee employee) throws ServiceException {
        return save(employee);
    }

    default void deleteEmployee(String id) throws ServiceException {
        deleteById(id);
    }

    // Munkanapló kezelés
    WorkRecord addWorkRecord(WorkRecord workRecord) throws ServiceException;
    List<WorkRecord> getMonthlyRecords(LocalDate startDate, LocalDate endDate) throws ServiceException;
    List<WorkRecord> getEmployeeMonthlyRecords(String employeeId, LocalDate startDate, LocalDate endDate) throws ServiceException;
    void deleteWorkRecord(String id) throws ServiceException;
}