package com.employeemanager.config;

public enum DatabaseType {
    FIREBASE("Firebase", "Firebase Realtime Database"),
    MYSQL("MySQL", "MySQL Database"),
    POSTGRESQL("PostgreSQL", "PostgreSQL Database");

    private final String displayName;
    private final String description;

    DatabaseType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return displayName;
    }
}