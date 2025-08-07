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
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RepositoryFactory {

    private final ApplicationContext applicationContext;
    private final DatabaseConnectionManager connectionManager;

    private EmployeeRepository employeeRepository;
    private WorkRecordRepository workRecordRepository;
    private EntityManagerFactory entityManagerFactory;

    @PostConstruct
    public void init() {
        updateRepositories();
    }

    public void updateRepositories() {
        DatabaseType activeType = connectionManager.getActiveType();
        log.info("Updating repositories for database type: {}", activeType);

        // Close existing EntityManagerFactory if switching from JPA
        if (entityManagerFactory != null && activeType == DatabaseType.FIREBASE) {
            entityManagerFactory.close();
            entityManagerFactory = null;
        }

        try {
            switch (activeType) {
                case FIREBASE:
                    setupFirebaseRepositories();
                    break;
                case MYSQL:
                case POSTGRESQL:
                    setupJpaRepositories();
                    break;
            }
            log.info("Repositories successfully updated for {}", activeType);
        } catch (Exception e) {
            log.error("Failed to update repositories for {}: {}", activeType, e.getMessage(), e);
            throw new RuntimeException("Failed to update repositories", e);
        }
    }

    private void setupFirebaseRepositories() {
        if (connectionManager.getCurrentFirestore() != null) {
            log.info("Setting up Firebase repositories with active Firestore connection");
            employeeRepository = new FirebaseEmployeeRepository(connectionManager.getCurrentFirestore());
            workRecordRepository = new FirebaseWorkRecordRepository(
                    connectionManager.getCurrentFirestore(), employeeRepository);
        } else {
            // Fallback to Spring managed beans
            log.info("Setting up Firebase repositories from Spring context");
            try {
                employeeRepository = applicationContext.getBean(FirebaseEmployeeRepository.class);
                workRecordRepository = applicationContext.getBean(FirebaseWorkRecordRepository.class);
            } catch (Exception e) {
                log.error("Failed to get Firebase repositories from Spring context", e);
                throw new RuntimeException("Firebase repositories not available", e);
            }
        }
    }

    private void setupJpaRepositories() {
        DataSource dataSource = connectionManager.getCurrentDataSource();
        if (dataSource == null) {
            throw new RuntimeException("No DataSource available for JPA repositories");
        }

        log.info("Setting up JPA repositories with active DataSource");

        try {
            // Try to get Spring-managed JPA repositories first
            if (applicationContext.containsBean("jpaEmployeeRepository")) {
                employeeRepository = applicationContext.getBean("jpaEmployeeRepository", JpaEmployeeRepository.class);
                workRecordRepository = applicationContext.getBean("jpaWorkRecordRepository", JpaWorkRecordRepository.class);
                log.info("Using Spring-managed JPA repositories");
            } else {
                // Create manual JPA repositories if Spring beans not available
                log.info("Creating manual JPA repositories");
                createManualJpaRepositories(dataSource);
            }
        } catch (Exception e) {
            log.warn("Could not get Spring-managed JPA repositories, creating manual ones: {}", e.getMessage());
            createManualJpaRepositories(dataSource);
        }
    }

    private void createManualJpaRepositories(DataSource dataSource) {
        try {
            // Create EntityManagerFactory programmatically
            Map<String, Object> properties = new HashMap<>();
            properties.put("javax.persistence.nonJtaDataSource", dataSource);
            properties.put("hibernate.hbm2ddl.auto", "update");
            properties.put("hibernate.show_sql", "false");
            properties.put("hibernate.format_sql", "true");

            DatabaseType dbType = connectionManager.getActiveType();
            if (dbType == DatabaseType.MYSQL) {
                properties.put("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");
            } else if (dbType == DatabaseType.POSTGRESQL) {
                properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
            }

            // Close existing factory if exists
            if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
                entityManagerFactory.close();
            }

            // Note: This would require persistence.xml configuration
            // For now, we'll rely on Spring-managed beans
            log.warn("Manual JPA repository creation not fully implemented. Spring configuration required.");
            throw new RuntimeException("JPA repositories require Spring configuration. Please restart the application.");

        } catch (Exception e) {
            log.error("Failed to create manual JPA repositories", e);
            throw new RuntimeException("Failed to create JPA repositories", e);
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

    public void clearRepositories() {
        employeeRepository = null;
        workRecordRepository = null;
        if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
            entityManagerFactory.close();
            entityManagerFactory = null;
        }
        log.info("Repositories cleared");
    }
}