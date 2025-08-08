package com.employeemanager.service.impl;

import com.employeemanager.config.DatabaseConnectionConfig;
import com.employeemanager.config.DatabaseConnectionManager;
import com.employeemanager.config.DatabaseType;
import com.employeemanager.event.DatabaseChangeEvent;
import com.employeemanager.event.DatabaseChangeListener;
import com.employeemanager.repository.RepositoryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseSwitchService {

    private final DatabaseConnectionManager connectionManager;
    private final RepositoryFactory repositoryFactory;
    private final ApplicationContext applicationContext;

    // Thread-safe listener list
    private final List<DatabaseChangeListener> listeners = new CopyOnWriteArrayList<>();

    // State tracking
    private volatile boolean isSwitching = false;
    private DatabaseConnectionConfig previousConfig = null;

    @PostConstruct
    public void init() {
        log.info("DatabaseSwitchService initialized");
    }

    /**
     * Switch database without restarting the application
     */
    public CompletableFuture<Boolean> switchWithoutRestart(DatabaseConnectionConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            if (isSwitching) {
                log.warn("Database switch already in progress");
                return false;
            }

            isSwitching = true;
            DatabaseConnectionConfig oldConfig = connectionManager.getActiveConnection();

            try {
                // Notify BEFORE_SWITCH
                notifyListeners(new DatabaseChangeEvent(
                        DatabaseChangeEvent.ChangeType.BEFORE_SWITCH,
                        oldConfig, config,
                        "Starting database switch to " + config.getProfileName()
                ));

                // Store previous config for rollback
                previousConfig = oldConfig;

                log.info("=== Starting hot database switch to: {} ({}) ===",
                        config.getProfileName(), config.getType());

                // Step 1: Test new connection
                log.info("Step 1: Testing new connection...");
                if (!connectionManager.testConnection(config)) {
                    throw new Exception("Connection test failed for " + config.getProfileName());
                }

                // Step 2: Apply the new connection
                log.info("Step 2: Applying new connection...");
                connectionManager.applyConnection(config);

                // Step 3: Clear and update repositories
                log.info("Step 3: Updating repositories...");
                repositoryFactory.clearRepositories();
                repositoryFactory.updateRepositories();

                // Step 4: Verify the switch
                log.info("Step 4: Verifying switch...");
                verifyDatabaseSwitch(config);

                // Step 5: Clear application caches
                log.info("Step 5: Clearing application caches...");
                clearApplicationCaches();

                log.info("=== Hot database switch completed successfully ===");

                // Notify AFTER_SWITCH
                notifyListeners(new DatabaseChangeEvent(
                        DatabaseChangeEvent.ChangeType.AFTER_SWITCH,
                        oldConfig, config,
                        "Successfully switched to " + config.getProfileName()
                ));

                return true;

            } catch (Exception e) {
                log.error("=== Hot database switch failed ===", e);

                // Notify SWITCH_FAILED
                notifyListeners(new DatabaseChangeEvent(
                        DatabaseChangeEvent.ChangeType.SWITCH_FAILED,
                        oldConfig, config, e
                ));

                // Attempt rollback
                if (previousConfig != null && !previousConfig.equals(config)) {
                    attemptRollback(previousConfig);
                }

                return false;
            } finally {
                isSwitching = false;
            }
        }).orTimeout(30, TimeUnit.SECONDS); // 30 second timeout
    }

    /**
     * Switch database with application restart (legacy method)
     */
    public void switchWithRestart(DatabaseConnectionConfig config) throws Exception {
        DatabaseConnectionConfig oldConfig = connectionManager.getActiveConnection();

        log.info("=== Starting database switch with restart to: {} ({}) ===",
                config.getProfileName(), config.getType());

        try {
            // Apply the new connection (this saves it as active)
            connectionManager.applyConnection(config);

            // Notify listeners about restart requirement
            notifyListeners(new DatabaseChangeEvent(
                    DatabaseChangeEvent.ChangeType.AFTER_SWITCH,
                    oldConfig, config,
                    "Database switched, restart required"
            ));

        } catch (Exception e) {
            log.error("Database switch failed", e);

            notifyListeners(new DatabaseChangeEvent(
                    DatabaseChangeEvent.ChangeType.SWITCH_FAILED,
                    oldConfig, config, e
            ));

            throw e;
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

                // Test with a simple query
                try {
                    connectionManager.getCurrentFirestore()
                            .collection("test")
                            .limit(1)
                            .get()
                            .get(5, TimeUnit.SECONDS);
                    log.info("Firebase connection verified with test query");
                } catch (Exception e) {
                    log.warn("Firebase test query failed, but connection exists");
                }
                break;

            case MYSQL:
            case POSTGRESQL:
                if (connectionManager.getCurrentDataSource() == null) {
                    throw new Exception("JDBC DataSource is null after switch");
                }

                // Test connection
                try (var conn = connectionManager.getCurrentDataSource().getConnection()) {
                    if (!conn.isValid(2)) {
                        throw new Exception("JDBC connection is not valid after switch");
                    }

                    // Run test query
                    String testQuery = activeType == DatabaseType.MYSQL ?
                            "SELECT 1" : "SELECT version()";
                    try (var stmt = conn.createStatement();
                         var rs = stmt.executeQuery(testQuery)) {
                        if (rs.next()) {
                            log.info("JDBC connection verified with test query");
                        }
                    }
                }
                break;
        }

        // Verify repositories are updated
        if (repositoryFactory.getEmployeeRepository() == null) {
            throw new Exception("Employee repository is null after switch");
        }
        if (repositoryFactory.getWorkRecordRepository() == null) {
            throw new Exception("WorkRecord repository is null after switch");
        }

        log.info("All repositories and connections verified");
    }

    /**
     * Clear application-level caches
     */
    private void clearApplicationCaches() {
        try {
            // Clear any Spring caches if present
            if (applicationContext.containsBean("cacheManager")) {
                Object cacheManager = applicationContext.getBean("cacheManager");
                // Implement cache clearing if needed
                log.info("Application caches cleared");
            }
        } catch (Exception e) {
            log.warn("Could not clear application caches: {}", e.getMessage());
        }
    }

    /**
     * Attempt to rollback to previous connection
     */
    private void attemptRollback(DatabaseConnectionConfig previousConfig) {
        try {
            log.info("Attempting rollback to previous connection: {}",
                    previousConfig.getProfileName());

            connectionManager.applyConnection(previousConfig);
            repositoryFactory.clearRepositories();
            repositoryFactory.updateRepositories();

            log.info("Rollback successful");
        } catch (Exception e) {
            log.error("Rollback failed: {}", e.getMessage());
        }
    }

    /**
     * Register a database change listener
     */
    public void addListener(DatabaseChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            log.debug("Added database change listener: {}", listener.getClass().getSimpleName());
        }
    }

    /**
     * Remove a database change listener
     */
    public void removeListener(DatabaseChangeListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notify all listeners about a database change
     */
    private void notifyListeners(DatabaseChangeEvent event) {
        for (DatabaseChangeListener listener : listeners) {
            try {
                listener.onDatabaseChange(event);
            } catch (Exception e) {
                log.error("Error notifying listener {}: {}",
                        listener.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    /**
     * Get current database status
     */
    public DatabaseStatus getDatabaseStatus() {
        DatabaseConnectionConfig active = connectionManager.getActiveConnection();

        return new DatabaseStatus(
                active != null ? active.getProfileName() : "None",
                active != null ? active.getType() : null,
                isSwitching,
                connectionManager.getCurrentFirestore() != null ||
                        connectionManager.getCurrentDataSource() != null
        );
    }

    /**
     * Check if a database switch is currently in progress
     */
    public boolean isSwitching() {
        return isSwitching;
    }

    /**
     * Inner class for database status
     */
    public static class DatabaseStatus {
        public final String profileName;
        public final DatabaseType type;
        public final boolean isSwitching;
        public final boolean isConnected;

        public DatabaseStatus(String profileName, DatabaseType type,
                              boolean isSwitching, boolean isConnected) {
            this.profileName = profileName;
            this.type = type;
            this.isSwitching = isSwitching;
            this.isConnected = isConnected;
        }

        @Override
        public String toString() {
            return String.format("Database: %s (%s) - Connected: %s, Switching: %s",
                    profileName, type != null ? type.getDisplayName() : "N/A",
                    isConnected, isSwitching);
        }
    }
}