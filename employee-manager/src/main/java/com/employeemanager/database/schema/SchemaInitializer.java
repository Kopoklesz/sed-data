package com.employeemanager.database.schema;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;

import com.employeemanager.database.config.DatabaseType;

import java.sql.*;

/**
 * Adatbázis séma inicializáló
 */
public interface SchemaInitializer {
    void initializeSchema(DataSource dataSource) throws SQLException;
    boolean isSchemaExists(DataSource dataSource) throws SQLException;
}

/**
 * MySQL séma inicializáló
 */
@Slf4j
class MySQLSchemaInitializer implements SchemaInitializer {
    
    @Override
    public void initializeSchema(DataSource dataSource) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            
            // employees tábla
            String createEmployeesTable = """
                CREATE TABLE IF NOT EXISTS employees (
                    id VARCHAR(50) PRIMARY KEY,
                    name VARCHAR(200) NOT NULL,
                    birth_place VARCHAR(200),
                    birth_date DATE,
                    mother_name VARCHAR(200),
                    tax_number VARCHAR(10) UNIQUE,
                    social_security_number VARCHAR(9) UNIQUE,
                    address TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_tax_number (tax_number),
                    INDEX idx_ssn (social_security_number)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """;
            
            // work_records tábla
            String createWorkRecordsTable = """
                CREATE TABLE IF NOT EXISTS work_records (
                    id VARCHAR(50) PRIMARY KEY,
                    employee_id VARCHAR(50) NOT NULL,
                    notification_date DATE NOT NULL,
                    notification_time TIME,
                    ebev_serial_number VARCHAR(100),
                    work_date DATE NOT NULL,
                    payment DECIMAL(12, 2) NOT NULL,
                    hours_worked INT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
                    INDEX idx_employee_id (employee_id),
                    INDEX idx_work_date (work_date),
                    INDEX idx_notification_date (notification_date)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """;
            
            // database_info tábla (verziókövetéshez)
            String createInfoTable = """
                CREATE TABLE IF NOT EXISTS database_info (
                    property_key VARCHAR(50) PRIMARY KEY,
                    property_value VARCHAR(200),
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """;
            
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createEmployeesTable);
                log.info("Employees table created/verified");
                
                stmt.execute(createWorkRecordsTable);
                log.info("Work records table created/verified");
                
                stmt.execute(createInfoTable);
                log.info("Database info table created/verified");
                
                // Verzió információ beszúrása
                stmt.execute("""
                    INSERT INTO database_info (property_key, property_value)
                    VALUES ('schema_version', '1.0')
                    ON DUPLICATE KEY UPDATE property_value = '1.0'
                    """);
            }
        }
    }
    
    @Override
    public boolean isSchemaExists(DataSource dataSource) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            // Ellenőrizzük, hogy léteznek-e a fő táblák
            try (ResultSet rs = metaData.getTables(null, null, "employees", null)) {
                if (!rs.next()) {
                    return false;
                }
            }
            
            try (ResultSet rs = metaData.getTables(null, null, "work_records", null)) {
                return rs.next();
            }
        }
    }
}

/**
 * PostgreSQL séma inicializáló
 */
@Slf4j
class PostgreSQLSchemaInitializer implements SchemaInitializer {
    
    @Override
    public void initializeSchema(DataSource dataSource) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            
            // employees tábla
            String createEmployeesTable = """
                CREATE TABLE IF NOT EXISTS employees (
                    id VARCHAR(50) PRIMARY KEY,
                    name VARCHAR(200) NOT NULL,
                    birth_place VARCHAR(200),
                    birth_date DATE,
                    mother_name VARCHAR(200),
                    tax_number VARCHAR(10) UNIQUE,
                    social_security_number VARCHAR(9) UNIQUE,
                    address TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
            
            // work_records tábla
            String createWorkRecordsTable = """
                CREATE TABLE IF NOT EXISTS work_records (
                    id VARCHAR(50) PRIMARY KEY,
                    employee_id VARCHAR(50) NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
                    notification_date DATE NOT NULL,
                    notification_time TIME,
                    ebev_serial_number VARCHAR(100),
                    work_date DATE NOT NULL,
                    payment DECIMAL(12, 2) NOT NULL,
                    hours_worked INT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
            
            // database_info tábla
            String createInfoTable = """
                CREATE TABLE IF NOT EXISTS database_info (
                    property_key VARCHAR(50) PRIMARY KEY,
                    property_value VARCHAR(200),
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
            
            // Indexek
            String[] createIndexes = {
                "CREATE INDEX IF NOT EXISTS idx_employees_tax_number ON employees(tax_number)",
                "CREATE INDEX IF NOT EXISTS idx_employees_ssn ON employees(social_security_number)",
                "CREATE INDEX IF NOT EXISTS idx_work_records_employee_id ON work_records(employee_id)",
                "CREATE INDEX IF NOT EXISTS idx_work_records_work_date ON work_records(work_date)",
                "CREATE INDEX IF NOT EXISTS idx_work_records_notification_date ON work_records(notification_date)"
            };
            
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createEmployeesTable);
                log.info("Employees table created/verified");
                
                stmt.execute(createWorkRecordsTable);
                log.info("Work records table created/verified");
                
                stmt.execute(createInfoTable);
                log.info("Database info table created/verified");
                
                // Indexek létrehozása
                for (String createIndex : createIndexes) {
                    stmt.execute(createIndex);
                }
                log.info("Indexes created/verified");
                
                // Verzió információ beszúrása/frissítése
                stmt.execute("""
                    INSERT INTO database_info (property_key, property_value)
                    VALUES ('schema_version', '1.0')
                    ON CONFLICT (property_key) DO UPDATE
                    SET property_value = '1.0', updated_at = CURRENT_TIMESTAMP
                    """);
            }
        }
    }
    
    @Override
    public boolean isSchemaExists(DataSource dataSource) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            // PostgreSQL-ben a táblák kisbetűsek
            try (ResultSet rs = metaData.getTables(null, null, "employees", null)) {
                if (!rs.next()) {
                    return false;
                }
            }
            
            try (ResultSet rs = metaData.getTables(null, null, "work_records", null)) {
                return rs.next();
            }
        }
    }
}

/**
 * Séma inicializáló factory
 */
@Slf4j
class SchemaInitializerFactory {
    
    public static SchemaInitializer getInitializer(DatabaseType type) {
        switch (type) {
            case MYSQL:
                return new MySQLSchemaInitializer();
            case POSTGRESQL:
                return new PostgreSQLSchemaInitializer();
            case H2:
                // H2 ugyanazt a szintaxist használja mint MySQL (MODE=MySQL)
                return new MySQLSchemaInitializer();
            default:
                throw new IllegalArgumentException("No schema initializer for: " + type);
        }
    }
    
    /**
     * Séma inicializálása és adatok betöltése
     */
    public static void initializeDatabase(DataSource dataSource, DatabaseType type) {
        try {
            SchemaInitializer initializer = getInitializer(type);
            
            boolean schemaExists = initializer.isSchemaExists(dataSource);
            log.info("Schema exists: {}", schemaExists);
            
            if (!schemaExists) {
                log.info("Initializing database schema for: {}", type);
                initializer.initializeSchema(dataSource);
                log.info("Database schema initialized successfully");
            } else {
                log.info("Database schema already exists, skipping initialization");
            }
            
        } catch (SQLException e) {
            log.error("Failed to initialize database schema", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }
}