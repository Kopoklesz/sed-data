package com.employeemanager.service;

import com.employeemanager.model.Employee;
import com.employeemanager.model.WorkRecord;
import com.employeemanager.repository.EmployeeRepository;
import com.employeemanager.repository.WorkRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final WorkRecordRepository workRecordRepository;

    @Transactional
    public Employee saveEmployee(Employee employee) {
        // Validáció
        if (employeeRepository.findByTaxNumber(employee.getTaxNumber()).isPresent()) {
            throw new RuntimeException("Tax number already exists");
        }
        if (employeeRepository.findBySocialSecurityNumber(employee.getSocialSecurityNumber()).isPresent()) {
            throw new RuntimeException("Social security number already exists");
        }

        return employeeRepository.save(employee);
    }

    @Transactional
    public WorkRecord addWorkRecord(WorkRecord workRecord) {
        return workRecordRepository.save(workRecord);
    }

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    public List<WorkRecord> getMonthlyRecords(LocalDate startDate, LocalDate endDate) {
        return workRecordRepository.findByWorkDateBetween(startDate, endDate);
    }

    public List<WorkRecord> getEmployeeMonthlyRecords(Long employeeId, LocalDate startDate, LocalDate endDate) {
        return workRecordRepository.findByEmployeeIdAndWorkDateBetween(employeeId, startDate, endDate);
    }
}