package com.sqli.sandbox.database;

import com.sqli.sandbox.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Interface defining a provider of JDBC connections.
 * Decouples services from static connection singletons and facilitates test mocking and in-memory injection.
 */
public interface ConnectionProvider {

    /**
     * Obtains a fresh database connection.
     * Callers must ensure the connection is closed after use (e.g., via try-with-resources).
     */
    Connection getConnection() throws SQLException;

    /**
     * Gets the active configuration used by this provider.
     */
    DatabaseConfig getConfig();
}
