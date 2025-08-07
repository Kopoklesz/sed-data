package com.employeemanager.service.impl;

import com.employeemanager.config.DatabaseConnectionConfig;
import com.employeemanager.config.DatabaseConnectionManager;
import com.employeemanager.config.DatabaseType;
import com.employeemanager.repository.RepositoryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseSwitchService {

    private final DatabaseConnectionManager connectionManager;
    private final RepositoryFactory repositoryFactory;
    private final ApplicationContext applicationContext;

    private final List<Consumer<DatabaseType>> switchListeners = new ArrayList<>();

    @PostConstruct
    public void init() {
        log.info("DatabaseSwitchService initialized");
    }

    /**
     * Switch to a different database connection
     */
    public void switchDatabase(DatabaseConnectionConfig config) throws Exception {
        log.info("=== Starting database switch to: {} ({}) ===",
                config.getProfileName(), config.getType());

        DatabaseType oldType = connectionManager.getActiveType();
        DatabaseType newType = config.getType();

        try {
            // Step 1: Apply the new connection
            log.info("Step 1: Applying new connection...");
            connectionManager.applyConnection(config);

            // Step 2: Clear and update repositories
            log.info("Step 2: Updating repositories...");
            repositoryFactory.clearRepositories();
            repositoryFactory.updateRepositories();

            // Step 3: Verify the switch
            log.info("Step 3: Verifying switch...");
            verifyDatabaseSwitch(config);

            // Step 4: Notify listeners
            log.info("Step 4: Notifying listeners...");
            notifyListeners(newType);

            log.info("=== Database switch completed successfully ===");

        } catch (Exception e) {
            log.error("=== Database switch failed ===", e);

            // Try to rollback if possible
            if (oldType != newType) {
                log.info("Attempting to rollback to previous connection...");
                // Note: This would need the previous config stored
            }

            throw new Exception("Failed to switch database: " + e.getMessage(), e);
        }
    }

    /**
     * Verify that the database switch was successful
     */
    private void verifyDatabaseSwitch(DatabaseConnectionConfig config) throws Exception {
        DatabaseType activeType = connectionManager.getActiveType();

        if (activeType != config.getType()) {
            throw new Exception("Database type mismatch after switch. Expected: " +
                    config.getType() + ", Actual: " + activeType);
        }

        // Verify connection is active
        switch (activeType) {
            case FIREBASE:
                if (connectionManager.getCurrentFirestore() == null) {
                    throw new Exception("Firebase connection is null after switch");
                }
                log.info("Firebase connection verified");
                break;

            case MYSQL:
            case POSTGRESQL:
                if (connectionManager.getCurrentDataSource() == null) {
                    throw new Exception("JDBC DataSource is null after switch");
                }
                // Try to get a connection to verify
                try (var conn = connectionManager.getCurrentDataSource().getConnection()) {
                    if (!conn.isValid(2)) {
                        throw new Exception("JDBC connection is not valid after switch");
                    }
                }
                log.info("JDBC connection verified for {}", activeType);
                break;
        }

        // Verify repositories are updated
        if (repositoryFactory.getEmployeeRepository() == null) {
            throw new Exception("Employee repository is null after switch");
        }
        if (repositoryFactory.getWorkRecordRepository() == null) {
            throw new Exception("WorkRecord repository is null after switch");
        }

        log.info("All repositories verified");
    }

    /**
     * Register a listener for database switch events
     */
    public void addSwitchListener(Consumer<DatabaseType> listener) {
        switchListeners.add(listener);
    }

    /**
     * Notify all listeners about the database switch
     */
    private void notifyListeners(DatabaseType newType) {
        for (Consumer<DatabaseType> listener : switchListeners) {
            try {
                listener.accept(newType);
            } catch (Exception e) {
                log.error("Error notifying listener: {}", e.getMessage());
            }
        }
    }

    /**
     * Get current database status
     */
    public String getDatabaseStatus() {
        DatabaseConnectionConfig active = connectionManager.getActiveConnection();
        if (active == null) {
            return "No active database connection";
        }

        StringBuilder status = new StringBuilder();
        status.append("Active: ").append(active.getProfileName());
        status.append(" (").append(active.getType().getDisplayName()).append(")");

        switch (active.getType()) {
            case FIREBASE:
                status.append("\nFirestore: ")
                        .append(connectionManager.getCurrentFirestore() != null ? "Connected" : "Not connected");
                break;
            case MYSQL:
            case POSTGRESQL:
                status.append("\nDataSource: ")
                        .append(connectionManager.getCurrentDataSource() != null ? "Connected" : "Not connected");
                break;
        }

        return status.toString();
    }
}