package com.employeemanager.event;

import com.employeemanager.config.DatabaseConnectionConfig;
import com.employeemanager.config.DatabaseType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Event class for database connection changes
 */
@Getter
@RequiredArgsConstructor
public class DatabaseChangeEvent {

    public enum ChangeType {
        BEFORE_SWITCH,
        AFTER_SWITCH,
        SWITCH_FAILED,
        CONNECTION_TESTED
    }

    private final ChangeType changeType;
    private final DatabaseConnectionConfig oldConnection;
    private final DatabaseConnectionConfig newConnection;
    private final DatabaseType databaseType;
    private final String message;
    private final Exception error;

    // Constructor for successful switch
    public DatabaseChangeEvent(ChangeType changeType,
                               DatabaseConnectionConfig oldConnection,
                               DatabaseConnectionConfig newConnection,
                               String message) {
        this.changeType = changeType;
        this.oldConnection = oldConnection;
        this.newConnection = newConnection;
        this.databaseType = newConnection != null ? newConnection.getType() : null;
        this.message = message;
        this.error = null;
    }

    // Constructor for failed switch
    public DatabaseChangeEvent(ChangeType changeType,
                               DatabaseConnectionConfig oldConnection,
                               DatabaseConnectionConfig newConnection,
                               Exception error) {
        this.changeType = changeType;
        this.oldConnection = oldConnection;
        this.newConnection = newConnection;
        this.databaseType = newConnection != null ? newConnection.getType() : null;
        this.message = error != null ? error.getMessage() : "Unknown error";
        this.error = error;
    }

    public boolean isSuccessful() {
        return changeType == ChangeType.AFTER_SWITCH && error == null;
    }

    public boolean isFailed() {
        return changeType == ChangeType.SWITCH_FAILED || error != null;
    }
}