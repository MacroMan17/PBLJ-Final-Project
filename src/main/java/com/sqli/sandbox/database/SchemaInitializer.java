package com.sqli.sandbox.database;

import com.sqli.sandbox.security.PasswordHasher;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Initializes and verifies the database schema for both:
 * 1. The secure production-grade table (`users`) with BCrypt password hashes.
 * 2. The synthetic isolated lab table (`vulnerable_lab_users`) for SQL injection demonstrations.
 */
public final class SchemaInitializer {

    private final ConnectionProvider connectionProvider;

    public SchemaInitializer(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    /**
     * Ensures all required tables and demonstration seed records are present in the target database.
     */
    public boolean initializeSchema() throws SQLException {
        try (Connection conn = connectionProvider.getConnection()) {
            createTablesIfNotExist(conn);
            seedSecureUsersIfEmpty(conn);
            seedVulnerableLabUsersIfEmpty(conn);
            return true;
        }
    }

    private void createTablesIfNotExist(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // 1. Secure authentication table (Production schema)
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS users (" +
                "    id INT AUTO_INCREMENT PRIMARY KEY, " +
                "    username VARCHAR(50) NOT NULL UNIQUE, " +
                "    password_hash VARCHAR(255) NOT NULL, " +
                "    role VARCHAR(20) NOT NULL DEFAULT 'USER', " +
                "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );

            // 2. Synthetic vulnerable lab table (Isolated demonstration schema)
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS vulnerable_lab_users (" +
                "    id INT AUTO_INCREMENT PRIMARY KEY, " +
                "    username VARCHAR(50) NOT NULL, " +
                "    password VARCHAR(100) NOT NULL, " +
                "    role VARCHAR(20) NOT NULL DEFAULT 'STUDENT'" +
                ")"
            );
        }
    }

    private void seedSecureUsersIfEmpty(Connection conn) throws SQLException {
        boolean isEmpty = true;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
            if (rs.next()) {
                isEmpty = (rs.getInt(1) == 0);
            }
        }

        if (isEmpty) {
            String insertSql = "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                // Seed 1: admin
                ps.setString(1, "admin");
                ps.setString(2, PasswordHasher.hashPassword("Admin@123"));
                ps.setString(3, "ADMIN");
                ps.executeUpdate();

                // Seed 2: alice
                ps.setString(1, "alice");
                ps.setString(2, PasswordHasher.hashPassword("Alice#Pass99"));
                ps.setString(3, "USER");
                ps.executeUpdate();

                // Seed 3: bob
                ps.setString(1, "bob");
                ps.setString(2, PasswordHasher.hashPassword("Bob$Secure2026"));
                ps.setString(3, "USER");
                ps.executeUpdate();
            }
        }
    }

    private void seedVulnerableLabUsersIfEmpty(Connection conn) throws SQLException {
        boolean isEmpty = true;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM vulnerable_lab_users")) {
            if (rs.next()) {
                isEmpty = (rs.getInt(1) == 0);
            }
        }

        if (isEmpty) {
            String insertSql = "INSERT INTO vulnerable_lab_users (username, password, role) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                // Seed 1: lab_admin
                ps.setString(1, "lab_admin");
                ps.setString(2, "password123");
                ps.setString(3, "ADMIN");
                ps.executeUpdate();

                // Seed 2: john_doe
                ps.setString(1, "john_doe");
                ps.setString(2, "welcome1");
                ps.setString(3, "STUDENT");
                ps.executeUpdate();

                // Seed 3: jane_smith
                ps.setString(1, "jane_smith");
                ps.setString(2, "secret88");
                ps.setString(3, "STUDENT");
                ps.executeUpdate();
            }
        }
    }
}
