package com.sqli.sandbox.service;

import com.sqli.sandbox.database.ConnectionProvider;
import com.sqli.sandbox.model.LoginResult;
import com.sqli.sandbox.model.SecurityMode;
import com.sqli.sandbox.model.User;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Demonstrates intentionally vulnerable SQL execution using direct string concatenation.
 *
 * CRITICAL SAFETY BOUNDARY:
 * This service operates SOLELY against the isolated synthetic lab table (`vulnerable_lab_users`).
 * It is designed strictly for local educational simulation and assessment demonstration.
 * Passwords and sensitive data are redacted from application output.
 */
public class VulnerableAuthService {

    public static final String VULNERABLE_QUERY_BASE =
        "SELECT id, username, role FROM vulnerable_lab_users WHERE username = '%s' AND password = '%s'";

    private final ConnectionProvider connectionProvider;

    public VulnerableAuthService(ConnectionProvider connectionProvider) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "ConnectionProvider must not be null");
    }

    public LoginResult authenticate(String username, String password) {
        long startTime = System.currentTimeMillis();

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return new LoginResult(
                false,
                SecurityMode.VULNERABLE,
                "SELECT id, username, role FROM vulnerable_lab_users WHERE username = ? AND password = ?",
                "SELECT id, username, role FROM vulnerable_lab_users WHERE username = '' AND password = ''",
                List.of("Parameter 1 (username): [EMPTY]", "Parameter 2 (password): [REDACTED]"),
                "Validation Failed: Username and password must not be empty.",
                List.of(),
                false,
                System.currentTimeMillis() - startTime
            );
        }

        // Direct string concatenation - Classic SQL Injection Vulnerability!
        String rawConstructedSql = String.format(VULNERABLE_QUERY_BASE, username, password);

        // For display purposes, we redact the raw password parameter while preserving any injected syntax
        String displaySql = String.format(VULNERABLE_QUERY_BASE, username, "[REDACTED_PASSWORD]");

        List<User> returnedUsers = new ArrayList<>();

        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement()) {

            try (ResultSet rs = stmt.executeQuery(rawConstructedSql)) {
                while (rs.next()) {
                    returnedUsers.add(new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("role"),
                        ""
                    ));
                }
            }

            long duration = System.currentTimeMillis() - startTime;

            if (!returnedUsers.isEmpty()) {
                boolean isBypass = returnedUsers.size() > 1 || !username.equals(returnedUsers.get(0).getUsername());
                String statusMsg = isBypass
                    ? "⚠ EXPLOIT SUCCESSFUL — SQL Injection bypassed authentication! " + returnedUsers.size() + " user row(s) leaked."
                    : "Login Successful (Valid credentials matched in vulnerable table).";

                return new LoginResult(
                    true,
                    SecurityMode.VULNERABLE,
                    "SELECT id, username, role FROM vulnerable_lab_users WHERE username = [INPUT] AND password = [INPUT]",
                    displaySql,
                    List.of("Injected Username: \"" + username + "\"", "Password: [REDACTED]"),
                    statusMsg,
                    returnedUsers,
                    false,
                    duration
                );
            } else {
                return new LoginResult(
                    false,
                    SecurityMode.VULNERABLE,
                    "SELECT id, username, role FROM vulnerable_lab_users WHERE username = [INPUT] AND password = [INPUT]",
                    displaySql,
                    List.of("Input Username: \"" + username + "\"", "Password: [REDACTED]"),
                    "Login Failed: No matching user record found.",
                    List.of(),
                    false,
                    duration
                );
            }

        } catch (SQLException e) {
            long duration = System.currentTimeMillis() - startTime;
            // Detect syntax error caused by unescaped quotes or malformed SQL
            String errMessage = e.getMessage();
            boolean isSyntaxErr = (errMessage != null && (
                errMessage.toLowerCase().contains("syntax") ||
                errMessage.toLowerCase().contains("you have an error in your sql syntax") ||
                errMessage.toLowerCase().contains("right syntax to use near")
            ));

            String statusDesc = isSyntaxErr
                ? "⚠ SQL SYNTAX ERROR: Malformed query structure! Injected quotes broke the SQL statement syntax."
                : "DATABASE ERROR: " + errMessage;

            return new LoginResult(
                false,
                SecurityMode.VULNERABLE,
                "SELECT id, username, role FROM vulnerable_lab_users WHERE username = [INPUT] AND password = [INPUT]",
                displaySql,
                List.of("Injected Username: \"" + username + "\"", "Password: [REDACTED]"),
                statusDesc,
                List.of(),
                isSyntaxErr,
                duration
            );
        }
    }
}
