package com.employeemanager.config;

import com.employeemanager.service.exception.ServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class DatabaseConnectionManager {

    private static final String CONFIG_FILE = "database-connections.json";
    private static final String RUNTIME_CONFIG_FILE = "application-runtime.properties";

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private ObjectMapper objectMapper;

    private DatabaseConnectionConfig activeConnection;
    private List<DatabaseConnectionConfig> savedConnections = new ArrayList<>();
    private DataSource currentDataSource;
    private Firestore currentFirestore;

    @PostConstruct
    public void init() {
        loadSavedConnections();
    }

    public boolean testConnection(DatabaseConnectionConfig config) {
        try {
            switch (config.getType()) {
                case FIREBASE:
                    return testFirebaseConnection(config);
                case MYSQL:
                case POSTGRESQL:
                    return testJdbcConnection(config);
                default:
                    return false;
            }
        } catch (Exception e) {
            log.error("Connection test failed: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean testFirebaseConnection(DatabaseConnectionConfig config) {
        try {
            Resource serviceAccount = resourceLoader.getResource(config.getFirebaseServiceAccountPath());

            if (!serviceAccount.exists()) {
                // Try as file path
                File file = new File(config.getFirebaseServiceAccountPath());
                if (!file.exists()) {
                    throw new IOException("Service account file not found");
                }
                serviceAccount = resourceLoader.getResource("file:" + file.getAbsolutePath());
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount.getInputStream()))
                    .setProjectId(config.getFirebaseProjectId())
                    .setDatabaseUrl(config.getFirebaseDatabaseUrl())
                    .build();

            // Test by creating a temporary app
            String testAppName = "test-" + System.currentTimeMillis();
            FirebaseApp testApp = FirebaseApp.initializeApp(options, testAppName);

            // Try to get Firestore instance
            Firestore testFirestore = FirestoreClient.getFirestore(testApp);

            // Cleanup
            testApp.delete();

            log.info("Firebase connection test successful");
            return true;
        } catch (Exception e) {
            log.error("Firebase connection test failed: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean testJdbcConnection(DatabaseConnectionConfig config) {
        String jdbcUrl = getJdbcUrl(config);
        log.info("Testing JDBC connection to: {}", jdbcUrl);
        log.info("Database type: {}", config.getType());
        log.info("Host: {}, Port: {}, Database: {}", config.getJdbcHost(), config.getJdbcPort(), config.getJdbcDatabase());
        log.info("Username: {}", config.getJdbcUsername());

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(config.getJdbcUsername());
        hikariConfig.setPassword(config.getJdbcPassword());
        hikariConfig.setConnectionTimeout(5000); // 5 seconds timeout
        hikariConfig.setMaximumPoolSize(1);
        hikariConfig.setDriverClassName(getDriverClassName(config.getType()));

        // PostgreSQL specific settings
        if (config.getType() == DatabaseType.POSTGRESQL) {
            hikariConfig.addDataSourceProperty("sslmode", "disable");
        }

        try (HikariDataSource testDataSource = new HikariDataSource(hikariConfig)) {
            try (Connection connection = testDataSource.getConnection()) {
                boolean isValid = connection.isValid(2);
                if (isValid) {
                    log.info("JDBC connection test successful for {} at {}:{}/{}",
                            config.getType(), config.getJdbcHost(), config.getJdbcPort(), config.getJdbcDatabase());

                    // Test if we can actually query the database
                    String testQuery = config.getType() == DatabaseType.MYSQL ?
                            "SELECT 1" : "SELECT version()";
                    try (var stmt = connection.createStatement();
                         var rs = stmt.executeQuery(testQuery)) {
                        if (rs.next()) {
                            log.info("Database query test successful");
                        }
                    }
                }
                return isValid;
            }
        } catch (SQLException e) {
            log.error("JDBC connection test failed - SQL Error: {}", e.getMessage());
            log.error("SQL State: {}, Error Code: {}", e.getSQLState(), e.getErrorCode());
            if (e.getMessage().contains("password") || e.getMessage().contains("authentication")) {
                log.error("Authentication failed. Please check username and password.");
            }
            return false;
        } catch (Exception e) {
            log.error("JDBC connection test failed - General Error: {}", e.getMessage(), e);
            return false;
        }
    }

    public void applyConnection(DatabaseConnectionConfig config) throws ServiceException {
        try {
            log.info("Applying connection: {} ({})", config.getProfileName(), config.getType());

            // Close existing connections
            closeCurrentConnections();

            // Apply new connection
            switch (config.getType()) {
                case FIREBASE:
                    applyFirebaseConnection(config);
                    break;
                case MYSQL:
                case POSTGRESQL:
                    applyJdbcConnection(config);
                    break;
            }

            // Update active status for all connections
            for (DatabaseConnectionConfig conn : savedConnections) {
                conn.setActive(false);
            }

            // Find and update the connection in saved list
            boolean found = false;
            for (int i = 0; i < savedConnections.size(); i++) {
                if (savedConnections.get(i).getProfileName().equals(config.getProfileName())) {
                    DatabaseConnectionConfig updated = config.copy();
                    updated.setActive(true);
                    savedConnections.set(i, updated);
                    found = true;
                    break;
                }
            }

            if (!found) {
                DatabaseConnectionConfig newConfig = config.copy();
                newConfig.setActive(true);
                savedConnections.add(newConfig);
            }

            // Save as active connection
            this.activeConnection = config.copy();
            this.activeConnection.setActive(true);

            // Save connections to file
            saveConnectionsToFile();

            // Save runtime config
            saveRuntimeConfig(config);

            // Update repository factory if available
            updateRepositoryFactory();

            log.info("Successfully applied {} connection: {}", config.getType(), config.getProfileName());
        } catch (Exception e) {
            log.error("Failed to apply connection: {}", e.getMessage(), e);
            throw new ServiceException("Failed to apply database connection: " + e.getMessage(), e);
        }
    }

    private void updateRepositoryFactory() {
        try {
            // Try to update RepositoryFactory if it exists in the context
            if (applicationContext != null && applicationContext.containsBean("repositoryFactory")) {
                Object factory = applicationContext.getBean("repositoryFactory");
                if (factory != null) {
                    factory.getClass().getMethod("updateRepositories").invoke(factory);
                    log.info("Repository factory updated for new connection");
                }
            }
        } catch (Exception e) {
            log.warn("Could not update repository factory: {}", e.getMessage());
        }
    }

    private void applyFirebaseConnection(DatabaseConnectionConfig config) throws IOException {
        Resource serviceAccount = resourceLoader.getResource(config.getFirebaseServiceAccountPath());

        if (!serviceAccount.exists()) {
            File file = new File(config.getFirebaseServiceAccountPath());
            if (!file.exists()) {
                throw new IOException("Service account file not found");
            }
            serviceAccount = resourceLoader.getResource("file:" + file.getAbsolutePath());
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount.getInputStream()))
                .setProjectId(config.getFirebaseProjectId())
                .setDatabaseUrl(config.getFirebaseDatabaseUrl())
                .build();

        // Clear existing Firebase apps
        for (FirebaseApp app : FirebaseApp.getApps()) {
            app.delete();
        }

        // Initialize new Firebase app
        FirebaseApp.initializeApp(options);
        currentFirestore = FirestoreClient.getFirestore();
    }

    private void applyJdbcConnection(DatabaseConnectionConfig config) {
        String jdbcUrl = getJdbcUrl(config);
        log.info("Applying JDBC connection to: {}", jdbcUrl);
        log.info("Database type: {}, Username: {}", config.getType(), config.getJdbcUsername());

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(config.getJdbcUsername());
        hikariConfig.setPassword(config.getJdbcPassword());
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);
        hikariConfig.setDriverClassName(getDriverClassName(config.getType()));

        // PostgreSQL specific settings
        if (config.getType() == DatabaseType.POSTGRESQL) {
            hikariConfig.addDataSourceProperty("sslmode", "disable");
        }

        currentDataSource = new HikariDataSource(hikariConfig);

        // Test the connection
        try (Connection conn = currentDataSource.getConnection()) {
            log.info("JDBC connection pool created and tested successfully");
        } catch (SQLException e) {
            log.error("Failed to test JDBC connection pool: {}", e.getMessage());
            throw new RuntimeException("Failed to create JDBC connection pool", e);
        }
    }

    private String getJdbcUrl(DatabaseConnectionConfig config) {
        if (config.getType() == DatabaseType.MYSQL) {
            return String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=UTF-8",
                    config.getJdbcHost() != null ? config.getJdbcHost() : "localhost",
                    config.getJdbcPort() != null ? config.getJdbcPort() : 3306,
                    config.getJdbcDatabase() != null ? config.getJdbcDatabase() : "testdb");
        } else if (config.getType() == DatabaseType.POSTGRESQL) {
            return String.format("jdbc:postgresql://%s:%d/%s?sslmode=disable",
                    config.getJdbcHost() != null ? config.getJdbcHost() : "localhost",
                    config.getJdbcPort() != null ? config.getJdbcPort() : 5432,
                    config.getJdbcDatabase() != null ? config.getJdbcDatabase() : "testdb");
        }
        return null;
    }

    private void closeCurrentConnections() {
        if (currentDataSource instanceof HikariDataSource) {
            ((HikariDataSource) currentDataSource).close();
            currentDataSource = null;
        }

        for (FirebaseApp app : FirebaseApp.getApps()) {
            app.delete();
        }
        currentFirestore = null;
    }

    public void saveConnection(DatabaseConnectionConfig config) {
        // Remove existing connection with same profile name
        savedConnections.removeIf(c -> c.getProfileName().equals(config.getProfileName()));

        // Add new/updated connection
        savedConnections.add(config.copy());

        // Save to file
        saveConnectionsToFile();
    }

    public void updateConnection(DatabaseConnectionConfig config) {
        // Find and update existing connection
        for (int i = 0; i < savedConnections.size(); i++) {
            if (savedConnections.get(i).getProfileName().equals(config.getProfileName())) {
                DatabaseConnectionConfig updated = config.copy();
                updated.setActive(savedConnections.get(i).isActive());
                savedConnections.set(i, updated);
                saveConnectionsToFile();
                return;
            }
        }
        // If not found, add as new
        saveConnection(config);
    }

    public void deleteConnection(DatabaseConnectionConfig config) {
        if (config.isActive()) {
            log.warn("Cannot delete active connection: {}", config.getProfileName());
            return;
        }

        savedConnections.removeIf(c -> c.getProfileName().equals(config.getProfileName()));
        saveConnectionsToFile();
        log.info("Deleted connection: {}", config.getProfileName());
    }

    private void saveConnectionsToFile() {
        try {
            Path configPath = Paths.get(CONFIG_FILE);
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(savedConnections);
            Files.writeString(configPath, json);
            log.info("Saved {} connections to file", savedConnections.size());
        } catch (IOException e) {
            log.error("Failed to save connections to file", e);
        }
    }

    private void loadSavedConnections() {
        try {
            Path configPath = Paths.get(CONFIG_FILE);
            if (Files.exists(configPath)) {
                String json = Files.readString(configPath);
                savedConnections = objectMapper.readValue(json,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, DatabaseConnectionConfig.class));

                // Find active connection
                activeConnection = savedConnections.stream()
                        .filter(DatabaseConnectionConfig::isActive)
                        .findFirst()
                        .orElse(null);

                log.info("Loaded {} saved connections", savedConnections.size());
            }
        } catch (IOException e) {
            log.error("Failed to load saved connections", e);
        }
    }

    private void saveRuntimeConfig(DatabaseConnectionConfig config) {
        try {
            List<String> lines = new ArrayList<>();
            lines.add("# Runtime database configuration");
            lines.add("database.type=" + config.getType().name());

            switch (config.getType()) {
                case FIREBASE:
                    lines.add("firebase.service-account.path=" + config.getFirebaseServiceAccountPath());
                    lines.add("firebase.project.id=" + config.getFirebaseProjectId());
                    lines.add("firebase.database.url=" + config.getFirebaseDatabaseUrl());
                    break;
                case MYSQL:
                case POSTGRESQL:
                    lines.add("spring.datasource.url=" + getJdbcUrl(config));
                    lines.add("spring.datasource.username=" + config.getJdbcUsername());
                    lines.add("spring.datasource.password=" + config.getJdbcPassword());
                    lines.add("spring.datasource.driver-class-name=" + getDriverClassName(config.getType()));
                    lines.add("spring.jpa.hibernate.ddl-auto=update");
                    lines.add("spring.jpa.show-sql=false");
                    break;
            }

            Path runtimeConfigPath = Paths.get(RUNTIME_CONFIG_FILE);
            Files.write(runtimeConfigPath, lines);
        } catch (IOException e) {
            log.error("Failed to save runtime config", e);
        }
    }

    private String getDriverClassName(DatabaseType type) {
        switch (type) {
            case MYSQL:
                return "com.mysql.cj.jdbc.Driver";
            case POSTGRESQL:
                return "org.postgresql.Driver";
            default:
                return "";
        }
    }

    // Getters
    public DatabaseConnectionConfig getActiveConnection() {
        return activeConnection;
    }

    public List<DatabaseConnectionConfig> getSavedConnections() {
        // Reload from file to ensure we have the latest active status
        loadSavedConnections();
        return new ArrayList<>(savedConnections);
    }

    public DataSource getCurrentDataSource() {
        return currentDataSource;
    }

    public Firestore getCurrentFirestore() {
        return currentFirestore;
    }

    public DatabaseType getActiveType() {
        return activeConnection != null ? activeConnection.getType() : DatabaseType.FIREBASE;
    }
}