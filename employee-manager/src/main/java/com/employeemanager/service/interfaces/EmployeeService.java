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
    default void deleteEmployee(Long id) throws ServiceException {
        deleteById(id.toString());
    }

    // Az új addWorkRecord metódus
    default WorkRecord addWorkRecord(WorkRecord workRecord) throws ServiceException {
        if (workRecord == null) {
            throw new ServiceException("Work record cannot be null");
        }

        // Ellenőrizzük, hogy van-e alkalmazott hozzárendelve
        if (workRecord.getEmployee() == null) {
            throw new ServiceException("Work record must have an employee assigned");
        }

        // Az alkalmazott létezésének ellenőrzése
        findById(workRecord.getEmployee().getId().toString())
                .orElseThrow(() -> new ServiceException("Employee not found"));

        // A munkanapló mentése
        return workRecord;
    }

    // Szükséges metódusok a munkanaplók kezeléséhez
    default List<WorkRecord> getMonthlyRecords(LocalDate startDate, LocalDate endDate) throws ServiceException {
        throw new ServiceException("Not implemented");
    }

    default List<WorkRecord> getEmployeeMonthlyRecords(Long employeeId, LocalDate startDate, LocalDate endDate) throws ServiceException {
        throw new ServiceException("Not implemented");
    }

    default void deleteWorkRecord(Long id) throws ServiceException {
        throw new ServiceException("Not implemented");
    }
}
