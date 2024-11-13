package com.employeemanager.repository.interfaces;

import com.employeemanager.model.Employee;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public interface EmployeeRepository extends BaseRepository<Employee, String> {
    Optional<Employee> findByTaxNumber(String taxNumber) throws ExecutionException, InterruptedException;
    Optional<Employee> findBySocialSecurityNumber(String ssn) throws ExecutionException, InterruptedException;
}
