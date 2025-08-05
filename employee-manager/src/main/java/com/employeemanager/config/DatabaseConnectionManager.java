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

    public DatabaseConnectionManager() {
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
            log.error("Connection test failed", e);
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

            return true;
        } catch (Exception e) {
            log.error("Firebase connection test failed", e);
            return false;
        }
    }

    private boolean testJdbcConnection(DatabaseConnectionConfig config) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.getJdbcUrl());
        hikariConfig.setUsername(config.getJdbcUsername());
        hikariConfig.setPassword(config.getJdbcPassword());
        hikariConfig.setConnectionTimeout(5000); // 5 seconds timeout
        hikariConfig.setMaximumPoolSize(1);

        try (HikariDataSource testDataSource = new HikariDataSource(hikariConfig)) {
            try (Connection connection = testDataSource.getConnection()) {
                return connection.isValid(2);
            }
        } catch (SQLException e) {
            log.error("JDBC connection test failed", e);
            return false;
        }
    }

    public void applyConnection(DatabaseConnectionConfig config) throws ServiceException {
        try {
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

            // Save as active connection
            this.activeConnection = config.copy();
            this.activeConnection.setActive(true);

            // Save configuration
            saveConnection(config);
            saveRuntimeConfig(config);

            log.info("Successfully applied {} connection", config.getType());
        } catch (Exception e) {
            log.error("Failed to apply connection", e);
            throw new ServiceException("Failed to apply database connection: " + e.getMessage(), e);
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
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.getJdbcUrl());
        hikariConfig.setUsername(config.getJdbcUsername());
        hikariConfig.setPassword(config.getJdbcPassword());
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);

        currentDataSource = new HikariDataSource(hikariConfig);
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
        try {
            Path configPath = Paths.get(CONFIG_FILE);
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(savedConnections);
            Files.writeString(configPath, json);
        } catch (IOException e) {
            log.error("Failed to save connections", e);
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
                    lines.add("spring.datasource.url=" + config.getJdbcUrl());
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