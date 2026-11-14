package com.sqli.sandbox.database;

import com.sqli.sandbox.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/**
 * Manages database connectivity, lifecycle, and health diagnostics.
 * Implements a connection-per-operation pattern with strict timeout policies,
 * eliminating the thread-safety hazards and stale state of static connection singletons.
 */
public class DatabaseManager implements ConnectionProvider {

    private final DatabaseConfig config;

    public DatabaseManager(DatabaseConfig config) {
        this.config = Objects.requireNonNull(config, "DatabaseConfig cannot be null");
        registerDriver();
    }

    private void registerDriver() {
        try {
            if ("h2".equalsIgnoreCase(config.getDbType())) {
                Class.forName("org.h2.Driver");
            } else {
                Class.forName("com.mysql.cj.jdbc.Driver");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Database driver not found on classpath: " + e.getMessage());
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(config.getJdbcUrl(), config.getUsername(), config.getPassword());
    }

    @Override
    public DatabaseConfig getConfig() {
        return config;
    }

    /**
     * Executes a non-destructive health check against the configured database.
     * Evaluates reachability, credentials, and retrieves database server metadata.
     */
    public ConnectionStatus testConnection() {
        long start = System.currentTimeMillis();
        try (Connection conn = getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            String product = meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion();

            // Perform lightweight query check
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT 1")) {
                if (rs.next()) {
                    long duration = System.currentTimeMillis() - start;
                    return new ConnectionStatus(
                        true,
                        "Connected successfully to " + config.getHost() + ":" + config.getPort() + "/" + config.getDatabase(),
                        duration,
                        product,
                        "Connection established in " + duration + " ms. Configuration source: " + config.getConfigSource()
                    );
                }
            }
        } catch (SQLException e) {
            long duration = System.currentTimeMillis() - start;
            String userFriendly = translateSqlException(e);
            return new ConnectionStatus(false, userFriendly, duration, "Unknown / Offline", e.getMessage());
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            return new ConnectionStatus(false, "Unexpected error: " + e.getMessage(), duration, "Offline", e.toString());
        }

        return new ConnectionStatus(false, "Connection test produced no result.", System.currentTimeMillis() - start, "Unknown", "");
    }

    /**
     * Translates vendor-specific JDBC error codes into clear, actionable messages.
     * Never leaks internal secrets or passwords.
     */
    public String translateSqlException(SQLException e) {
        int errorCode = e.getErrorCode();
        String msg = (e.getMessage() != null) ? e.getMessage().toLowerCase() : "";

        if (msg.contains("communications link failure") || msg.contains("connection refused") || errorCode == 0) {
            return String.format(
                "Cannot reach MySQL at %s:%d. Please verify the MySQL service is started.",
                config.getHost(), config.getPort()
            );
        }
        if (errorCode == 1045 || msg.contains("access denied")) {
            return String.format(
                "Access denied for user '%s'. Please check your credentials in .env or db.properties.",
                config.getUsername()
            );
        }
        if (errorCode == 1049 || msg.contains("unknown database")) {
            return String.format(
                "Database '%s' does not exist. Please run database/schema.sql to initialize it.",
                config.getDatabase()
            );
        }
        return "Database Error (" + errorCode + "): " + e.getMessage();
    }

    /**
     * Diagnostic connection status container.
     */
    public static final class ConnectionStatus {
        private final boolean connected;
        private final String message;
        private final long responseTimeMs;
        private final String serverVersion;
        private final String details;

        public ConnectionStatus(boolean connected, String message, long responseTimeMs, String serverVersion, String details) {
            this.connected = connected;
            this.message = message;
            this.responseTimeMs = responseTimeMs;
            this.serverVersion = serverVersion;
            this.details = details;
        }

        public boolean isConnected() {
            return connected;
        }

        public String getMessage() {
            return message;
        }

        public long getResponseTimeMs() {
            return responseTimeMs;
        }

        public String getServerVersion() {
            return serverVersion;
        }

        public String getDetails() {
            return details;
        }

        @Override
        public String toString() {
            return String.format("ConnectionStatus[connected=%b, msg='%s', time=%dms]", connected, message, responseTimeMs);
        }
    }
}
