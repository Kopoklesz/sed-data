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
}
