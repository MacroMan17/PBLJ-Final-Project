package com.sqli.sandbox.service;

import com.sqli.sandbox.database.ConnectionProvider;
import com.sqli.sandbox.model.LoginResult;
import com.sqli.sandbox.model.SecurityMode;
import com.sqli.sandbox.model.User;
import com.sqli.sandbox.security.PasswordHasher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Implements secure authentication using parameterized PreparedStatement and BCrypt password verification.
 * Adheres strictly to OWASP guidelines:
 * 1. Parameters are bound separately from SQL execution plan.
 * 2. Passwords are never queried in raw form or concatenated.
 * 3. Constant-time BCrypt verification is performed in application logic.
 * 4. Passwords and hashes are completely redacted from visualizer and logs.
 */
public class SecureAuthService {

    public static final String SECURE_QUERY_TEMPLATE =
        "SELECT id, username, password_hash, role, created_at FROM users WHERE username = ?";

    private final ConnectionProvider connectionProvider;

    public SecureAuthService(ConnectionProvider connectionProvider) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "ConnectionProvider must not be null");
    }

    public LoginResult authenticate(String username, String password) {
        long startTime = System.currentTimeMillis();

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return new LoginResult(
                false,
                SecurityMode.SECURE,
                SECURE_QUERY_TEMPLATE,
                SECURE_QUERY_TEMPLATE,
                List.of("Parameter 1 (username): [EMPTY]", "Parameter 2 (password): [REDACTED]"),
                "Validation Failed: Username and password must not be empty.",
                List.of(),
                false,
                System.currentTimeMillis() - startTime
            );
        }

        List<String> boundParams = List.of(
            "Param 1 [VARCHAR / username] -> \"" + username + "\"",
            "Param 2 [VARCHAR / password] -> [REDACTED - Verified via BCrypt in Application]"
        );

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(SECURE_QUERY_TEMPLATE)) {

            // Parameter binding treats the username strictly as literal data, neutralising all SQL injection
            ps.setString(1, username.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    String dbUsername = rs.getString("username");
                    String storedHash = rs.getString("password_hash");
                    String role = rs.getString("role");
                    String createdAt = rs.getString("created_at");

                    // Verify password using BCrypt
                    boolean passwordMatches = PasswordHasher.verifyPassword(password, storedHash);
                    long duration = System.currentTimeMillis() - startTime;

                    if (passwordMatches) {
                        User user = new User(id, dbUsername, role, createdAt);
                        return new LoginResult(
                            true,
                            SecurityMode.SECURE,
                            SECURE_QUERY_TEMPLATE,
                            "PreparedStatement executed safely with 1 bound parameter.",
                            boundParams,
                            "AUTHENTICATION SUCCESS: Welcome, " + dbUsername + " (" + role + ")!",
                            List.of(user),
                            false,
                            duration
                        );
                    } else {
                        return new LoginResult(
                            false,
                            SecurityMode.SECURE,
                            SECURE_QUERY_TEMPLATE,
                            "PreparedStatement executed safely with 1 bound parameter.",
                            boundParams,
                            "AUTHENTICATION FAILED: Invalid credentials (password verification failed).",
                            List.of(),
                            false,
                            duration
                        );
                    }
                } else {
                    long duration = System.currentTimeMillis() - startTime;
                    return new LoginResult(
                        false,
                        SecurityMode.SECURE,
                        SECURE_QUERY_TEMPLATE,
                        "PreparedStatement executed safely with 1 bound parameter.",
                        boundParams,
                        "AUTHENTICATION FAILED: User '" + username.trim() + "' does not exist. (Input treated as literal data, attack neutralized).",
                        List.of(),
                        false,
                        duration
                    );
                }
            }
        } catch (SQLException e) {
            long duration = System.currentTimeMillis() - startTime;
            return new LoginResult(
                false,
                SecurityMode.SECURE,
                SECURE_QUERY_TEMPLATE,
                "PreparedStatement execution aborted due to database error.",
                boundParams,
                "DATABASE ERROR: " + e.getMessage(),
                List.of(),
                false,
                duration
            );
        }
    }
}
