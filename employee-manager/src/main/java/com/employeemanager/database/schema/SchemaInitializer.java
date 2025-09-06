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