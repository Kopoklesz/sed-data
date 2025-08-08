// Pattern for dynamic repository usage in services
// This ensures services always use the current repository after database switch

package com.employeemanager.service.impl;

import com.employeemanager.model.Employee;
import com.employeemanager.repository.RepositoryFactory;
import com.employeemanager.repository.interfaces.EmployeeRepository;
import com.employeemanager.repository.interfaces.WorkRecordRepository;
import com.employeemanager.service.exception.ServiceException;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Example of dynamic repository pattern for EmployeeServiceImpl
 * Apply same pattern to WorkRecordServiceImpl
 */
@Service
@RequiredArgsConstructor
public class DynamicRepositoryPattern {

    private final RepositoryFactory repositoryFactory;

    /**
     * IMPORTANT: Always get repository dynamically, never cache!
     * This ensures the correct repository is used after database switch
     */

    // ❌ WRONG - Don't do this:
    // private final EmployeeRepository employeeRepository;

    // ✅ CORRECT - Do this instead:
    private EmployeeRepository getEmployeeRepository() {
        return repositoryFactory.getEmployeeRepository();
    }

    private WorkRecordRepository getWorkRecordRepository() {
        return repositoryFactory.getWorkRecordRepository();
    }

    // Example method showing dynamic repository usage:
    public Employee saveEmployee(Employee employee) throws ServiceException {
        try {
            // Always get fresh repository reference
            return getEmployeeRepository().save(employee);
        } catch (Exception e) {
            throw new ServiceException("Failed to save employee", e);
        }
    }

    // Thread-safe repository access with synchronization if needed:
    public synchronized List<Employee> findAllEmployees() throws ServiceException {
        try {
            // Get current repository - safe even during database switch
            EmployeeRepository repo = getEmployeeRepository();
            if (repo == null) {
                throw new ServiceException("Repository not available - database switch in progress?");
            }
            return repo.findAll();
        } catch (Exception e) {
            throw new ServiceException("Failed to find employees", e);
        }
    }
}