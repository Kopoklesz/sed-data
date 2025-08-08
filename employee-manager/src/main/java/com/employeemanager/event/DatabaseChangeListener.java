package com.employeemanager.event;

/**
 * Listener interface for database change events
 */
@FunctionalInterface
public interface DatabaseChangeListener {

    /**
     * Called when a database change event occurs
     * @param event the database change event
     */
    void onDatabaseChange(DatabaseChangeEvent event);

    /**
     * Default method to handle only successful changes
     * @param event the database change event
     */
    default void onSuccessfulChange(DatabaseChangeEvent event) {
        if (event.isSuccessful()) {
            onDatabaseChange(event);
        }
    }

    /**
     * Default method to handle only failed changes
     * @param event the database change event
     */
    default void onFailedChange(DatabaseChangeEvent event) {
        if (event.isFailed()) {
            onDatabaseChange(event);
        }
    }
}