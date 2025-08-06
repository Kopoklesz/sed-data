package com.employeemanager.repository;

import com.employeemanager.config.DatabaseConnectionManager;
import com.employeemanager.config.DatabaseType;
import com.employeemanager.repository.impl.FirebaseEmployeeRepository;
import com.employeemanager.repository.impl.FirebaseWorkRecordRepository;
import com.employeemanager.repository.impl.JpaEmployeeRepository;
import com.employeemanager.repository.impl.JpaWorkRecordRepository;
import com.employeemanager.repository.interfaces.EmployeeRepository;
import com.employeemanager.repository.interfaces.WorkRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class RepositoryFactory {

    private final ApplicationContext applicationContext;
    private final DatabaseConnectionManager connectionManager;

    private EmployeeRepository employeeRepository;
    private WorkRecordRepository workRecordRepository;

    @PostConstruct
    public void init() {
        updateRepositories();
    }

    public void updateRepositories() {
        DatabaseType activeType = connectionManager.getActiveType();
        log.info("Updating repositories for database type: {}", activeType);

        switch (activeType) {
            case FIREBASE:
                if (connectionManager.getCurrentFirestore() != null) {
                    employeeRepository = new FirebaseEmployeeRepository(connectionManager.getCurrentFirestore());
                    workRecordRepository = new FirebaseWorkRecordRepository(
                            connectionManager.getCurrentFirestore(), employeeRepository);
                } else {
                    // Fallback to Spring managed beans
                    try {
                        employeeRepository = applicationContext.getBean(FirebaseEmployeeRepository.class);
                        workRecordRepository = applicationContext.getBean(FirebaseWorkRecordRepository.class);
                    } catch (Exception e) {
                        log.error("Failed to get Firebase repositories from Spring context", e);
                        throw new RuntimeException("Firebase repositories not available", e);
                    }
                }
                break;

            case MYSQL:
            case POSTGRESQL:
                try {
                    employeeRepository = applicationContext.getBean(JpaEmployeeRepository.class);
                    workRecordRepository = applicationContext.getBean(JpaWorkRecordRepository.class);
                } catch (Exception e) {
                    log.warn("JPA repositories not available, this might be a configuration issue");
                    throw new RuntimeException("JPA repositories not available. Make sure database connection is properly configured.", e);
                }
                break;
        }
    }

    public EmployeeRepository getEmployeeRepository() {
        if (employeeRepository == null) {
            updateRepositories();
        }
        return employeeRepository;
    }

    public WorkRecordRepository getWorkRecordRepository() {
        if (workRecordRepository == null) {
            updateRepositories();
        }
        return workRecordRepository;
    }
}