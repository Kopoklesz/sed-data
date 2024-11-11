package com.employeemanager.repository;

import com.employeemanager.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByTaxNumber(String taxNumber);
    Optional<Employee> findBySocialSecurityNumber(String socialSecurityNumber);
}