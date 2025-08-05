package com.employeemanager.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

@Slf4j
@Configuration
public class DatabaseConfig {

    @Value("${database.type:FIREBASE}")
    private String databaseType;

    @Autowired
    private DatabaseConnectionManager connectionManager;

    @PostConstruct
    public void init() {
        log.info("Initializing database configuration with type: {}", databaseType);

        // Load saved configuration if exists
        if (connectionManager.getActiveConnection() == null) {
            // Create default configuration based on properties
            DatabaseConnectionConfig defaultConfig = createDefaultConfig();
            try {
                connectionManager.applyConnection(defaultConfig);
            } catch (Exception e) {
                log.error("Failed to apply default database configuration", e);
            }
        }
    }

    private DatabaseConnectionConfig createDefaultConfig() {
        DatabaseConnectionConfig config = new DatabaseConnectionConfig();
        config.setType(DatabaseType.valueOf(databaseType));
        config.setProfileName("Default");
        config.setActive(true);

        // Set default values based on application.properties
        switch (config.getType()) {
            case FIREBASE:
                config.setFirebaseServiceAccountPath("classpath:service-account.json");
                config.setFirebaseProjectId("employee-manager-e70b6");
                config.setFirebaseDatabaseUrl("https://employee-manager-e70b6.firebaseio.com");
                break;
            case MYSQL:
                config.setJdbcHost("localhost");
                config.setJdbcPort(3306);
                config.setJdbcDatabase("employeemanager");
                config.setJdbcUsername("root");
                config.setJdbcPassword("");
                break;
            case POSTGRESQL:
                config.setJdbcHost("localhost");
                config.setJdbcPort(5432);
                config.setJdbcDatabase("employeemanager");
                config.setJdbcUsername("postgres");
                config.setJdbcPassword("");
                break;
        }

        return config;
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "database.type", havingValue = "MYSQL")
    public DataSource mysqlDataSource() {
        return connectionManager.getCurrentDataSource();
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "database.type", havingValue = "POSTGRESQL")
    public DataSource postgresqlDataSource() {
        return connectionManager.getCurrentDataSource();
    }
}