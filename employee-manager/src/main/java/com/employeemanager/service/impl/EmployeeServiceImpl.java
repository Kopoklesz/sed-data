package com.employeemanager.service.impl;

import com.employeemanager.model.Employee;
import com.employeemanager.model.WorkRecord;
import com.employeemanager.repository.interfaces.EmployeeRepository;
import com.employeemanager.repository.interfaces.WorkRecordRepository;
import com.employeemanager.service.exception.ServiceException;
import com.employeemanager.service.interfaces.EmployeeService;
import com.employeemanager.service.interfaces.WorkRecordService;
import com.employeemanager.util.ValidationHelper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {
    private static final Logger logger = LoggerFactory.getLogger(EmployeeServiceImpl.class);
    private final EmployeeRepository employeeRepository;

    @Override
    public Employee save(Employee employee) throws ServiceException {
        try {
            if (!validateEmployee(employee)) {
                throw new ServiceException("Invalid employee data");
            }

            // Ellenőrizzük a unique mezőket
            if (employee.getId() == null) {
                if (employeeRepository.findByTaxNumber(employee.getTaxNumber()).isPresent()) {
                    throw new ServiceException("Tax number already exists");
                }
                if (employeeRepository.findBySocialSecurityNumber(employee.getSocialSecurityNumber()).isPresent()) {
                    throw new ServiceException("Social security number already exists");
                }
            }

            return employeeRepository.save(employee);
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error saving employee", e);
            throw new ServiceException("Failed to save employee", e);
        }
    }

    @Override
    public Optional<Employee> findById(String id) throws ServiceException {
        try {
            return employeeRepository.findById(id);
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error finding employee by id: " + id, e);
            throw new ServiceException("Failed to find employee", e);
        }
    }

    @Override
    public List<Employee> findAll() throws ServiceException {
        try {
            return employeeRepository.findAll();
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error finding all employees", e);
            throw new ServiceException("Failed to find all employees", e);
        }
    }

    @Override
    public void deleteById(String id) throws ServiceException {
        try {
            employeeRepository.deleteById(id);
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error deleting employee with id: " + id, e);
            throw new ServiceException("Failed to delete employee", e);
        }
    }

    @Override
    public List<Employee> saveAll(List<Employee> employees) throws ServiceException {
        try {
            if (employees.stream().anyMatch(e -> !validateEmployee(e))) {
                throw new ServiceException("Invalid employee data in batch");
            }
            return employeeRepository.saveAll(employees);
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error saving multiple employees", e);
            throw new ServiceException("Failed to save employees", e);
        }
    }

    @Override
    public Optional<Employee> findByTaxNumber(String taxNumber) throws ServiceException {
        try {
            return employeeRepository.findByTaxNumber(taxNumber);
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error finding employee by tax number: " + taxNumber, e);
            throw new ServiceException("Failed to find employee by tax number", e);
        }
    }

    @Override
    public Optional<Employee> findBySocialSecurityNumber(String ssn) throws ServiceException {
        try {
            return employeeRepository.findBySocialSecurityNumber(ssn);
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error finding employee by SSN: " + ssn, e);
            throw new ServiceException("Failed to find employee by SSN", e);
        }
    }

    @Override
    public boolean validateEmployee(Employee employee) {
        return employee != null &&
                ValidationHelper.isValidName(employee.getName()) &&
                ValidationHelper.isValidTaxNumber(employee.getTaxNumber()) &&
                ValidationHelper.isValidSocialSecurityNumber(employee.getSocialSecurityNumber()) &&
                ValidationHelper.isValidBirthDate(employee.getBirthDate());
    }
}