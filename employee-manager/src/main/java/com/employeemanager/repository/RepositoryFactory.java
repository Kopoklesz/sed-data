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
import javax.annotation.PreDestroy;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe Repository Factory with dynamic repository management
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RepositoryFactory {

    private final ApplicationContext applicationContext;
    private final DatabaseConnectionManager connectionManager;

    // Thread-safe access control
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // Repository instances
    private volatile EmployeeRepository employeeRepository;
    private volatile WorkRecordRepository workRecordRepository;
    private volatile EntityManagerFactory entityManagerFactory;

    // Cache for repository instances by database type
    private final Map<DatabaseType, EmployeeRepository> employeeRepoCache = new HashMap<>();
    private final Map<DatabaseType, WorkRecordRepository> workRecordRepoCache = new HashMap<>();

    // State tracking
    private volatile boolean isUpdating = false;
    private volatile DatabaseType currentType = null;

    @PostConstruct
    public void init() {
        log.info("Initializing RepositoryFactory");
        updateRepositories();
    }

    @PreDestroy
    public void cleanup() {
        log.info("Cleaning up RepositoryFactory");
        clearRepositories();
    }

    /**
     * Update repositories based on current database configuration
     * Thread-safe implementation
     */
    public void updateRepositories() {
        lock.writeLock().lock();
        try {
            isUpdating = true;
            DatabaseType activeType = connectionManager.getActiveType();
            log.info("Updating repositories for database type: {}", activeType);

            // Check if we're switching database types
            if (currentType != null && currentType != activeType) {
                log.info("Database type changed from {} to {}", currentType, activeType);
                clearCache();
            }

            currentType = activeType;

            // Close existing EntityManagerFactory if switching from JPA
            if (entityManagerFactory != null && activeType == DatabaseType.FIREBASE) {
                closeEntityManagerFactory();
            }

            // Check cache first
            if (employeeRepoCache.containsKey(activeType)) {
                log.info("Using cached repositories for {}", activeType);
                employeeRepository = employeeRepoCache.get(activeType);
                workRecordRepository = workRecordRepoCache.get(activeType);
            } else {
                // Create new repositories
                switch (activeType) {
                    case FIREBASE:
                        setupFirebaseRepositories();
                        break;
                    case MYSQL:
                    case POSTGRESQL:
                        setupJpaRepositories();
                        break;
                }

                // Cache the repositories
                if (employeeRepository != null && workRecordRepository != null) {
                    employeeRepoCache.put(activeType, employeeRepository);
                    workRecordRepoCache.put(activeType, workRecordRepository);
                }
            }

            log.info("Repositories successfully updated for {}", activeType);
        } catch (Exception e) {
            log.error("Failed to update repositories for {}: {}", currentType, e.getMessage(), e);
            throw new RuntimeException("Failed to update repositories", e);
        } finally {
            isUpdating = false;
            lock.writeLock().unlock();
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
            String beanPrefix = currentType == DatabaseType.MYSQL ? "mysql" : "postgresql";

            if (applicationContext.containsBean(beanPrefix + "EmployeeRepository")) {
                employeeRepository = applicationContext.getBean(
                        beanPrefix + "EmployeeRepository", EmployeeRepository.class);
                workRecordRepository = applicationContext.getBean(
                        beanPrefix + "WorkRecordRepository", WorkRecordRepository.class);
                log.info("Using Spring-managed {} repositories", currentType);
            } else if (applicationContext.containsBean("jpaEmployeeRepository")) {
                employeeRepository = applicationContext.getBean(
                        "jpaEmployeeRepository", JpaEmployeeRepository.class);
                workRecordRepository = applicationContext.getBean(
                        "jpaWorkRecordRepository", JpaWorkRecordRepository.class);
                log.info("Using default Spring-managed JPA repositories");
            } else {
                log.warn("No Spring-managed JPA repositories found");
                throw new RuntimeException("JPA repositories require Spring configuration");
            }
        } catch (Exception e) {
            log.error("Could not get Spring-managed JPA repositories: {}", e.getMessage());
            throw new RuntimeException("Failed to setup JPA repositories", e);
        }
    }

    private void closeEntityManagerFactory() {
        if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
            try {
                entityManagerFactory.close();
                log.info("EntityManagerFactory closed");
            } catch (Exception e) {
                log.error("Error closing EntityManagerFactory", e);
            } finally {
                entityManagerFactory = null;
            }
        }
    }

    /**
     * Get employee repository with read lock
     */
    public EmployeeRepository getEmployeeRepository() {
        lock.readLock().lock();
        try {
            if (employeeRepository == null && !isUpdating) {
                // Upgrade to write lock to update
                lock.readLock().unlock();
                lock.writeLock().lock();
                try {
                    if (employeeRepository == null) {
                        updateRepositories();
                    }
                } finally {
                    lock.readLock().lock();
                    lock.writeLock().unlock();
                }
            }
            return employeeRepository;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get work record repository with read lock
     */
    public WorkRecordRepository getWorkRecordRepository() {
        lock.readLock().lock();
        try {
            if (workRecordRepository == null && !isUpdating) {
                // Upgrade to write lock to update
                lock.readLock().unlock();
                lock.writeLock().lock();
                try {
                    if (workRecordRepository == null) {
                        updateRepositories();
                    }
                } finally {
                    lock.readLock().lock();
                    lock.writeLock().unlock();
                }
            }
            return workRecordRepository;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Clear all repositories and caches
     */
    public void clearRepositories() {
        lock.writeLock().lock();
        try {
            log.info("Clearing all repositories and caches");

            employeeRepository = null;
            workRecordRepository = null;

            closeEntityManagerFactory();

            clearCache();

            currentType = null;
            log.info("Repositories cleared");
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void clearCache() {
        employeeRepoCache.clear();
        workRecordRepoCache.clear();
        log.info("Repository cache cleared");
    }

    /**
     * Check if repositories are currently being updated
     */
    public boolean isUpdating() {
        return isUpdating;
    }

    /**
     * Get current database type
     */
    public DatabaseType getCurrentType() {
        return currentType;
    }

    /**
     * Refresh repositories - force update even if same type
     */
    public void refreshRepositories() {
        lock.writeLock().lock();
        try {
            log.info("Forcing repository refresh");
            clearCache();
            updateRepositories();
        } finally {
            lock.writeLock().unlock();
        }
    }
}