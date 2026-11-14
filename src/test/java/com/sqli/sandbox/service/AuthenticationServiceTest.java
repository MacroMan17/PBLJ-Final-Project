package com.sqli.sandbox.service;

import com.sqli.sandbox.config.DatabaseConfig;
import com.sqli.sandbox.database.ConnectionProvider;
import com.sqli.sandbox.database.DatabaseManager;
import com.sqli.sandbox.database.SchemaInitializer;
import com.sqli.sandbox.model.LoginResult;
import com.sqli.sandbox.model.SecurityMode;
import com.sqli.sandbox.model.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuthenticationService Core Logic Unit Tests")
class AuthenticationServiceTest {

    private static DatabaseManager dbManager;
    private static AuthenticationService authService;

    @BeforeAll
    static void init() throws SQLException {
        DatabaseConfig config = DatabaseConfig.createInMemoryH2Config("auth_service_test");
        dbManager = new DatabaseManager(config);
        SchemaInitializer initializer = new SchemaInitializer(dbManager);
        initializer.initializeSchema();
        authService = new AuthenticationService(dbManager);
    }

    @Test
    @DisplayName("Secure Mode: Valid credentials should authenticate successfully")
    void testSecureLoginSuccess() {
        LoginResult result = authService.login("admin", "Admin@123", SecurityMode.SECURE);
        assertTrue(result.isSuccess());
        assertTrue(result.hasUsers());
        assertEquals("admin", result.getUsers().get(0).getUsername());
        assertEquals("ADMIN", result.getUsers().get(0).getRole());
        // Verify User model does not leak credentials
        assertNotNull(result.getUsers().get(0).toString());
        assertFalse(result.getUsers().get(0).toString().contains("Admin@123"));
    }

    @Test
    @DisplayName("Secure Mode: Wrong password should be rejected")
    void testSecureLoginWrongPassword() {
        LoginResult result = authService.login("admin", "WrongPass123", SecurityMode.SECURE);
        assertFalse(result.isSuccess());
        assertFalse(result.hasUsers());
        assertTrue(result.getMessage().contains("Invalid credentials"));
    }

    @Test
    @DisplayName("Secure Mode: Non-existent user should fail safely")
    void testSecureLoginUnknownUser() {
        LoginResult result = authService.login("does_not_exist", "SomePass", SecurityMode.SECURE);
        assertFalse(result.isSuccess());
        assertFalse(result.hasUsers());
    }

    @Test
    @DisplayName("Secure Mode: Empty credentials should fail input validation")
    void testSecureLoginEmptyInput() {
        LoginResult result = authService.login("", "", SecurityMode.SECURE);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Validation Failed"));
    }

    @Test
    @DisplayName("Secure Mode: SQL injection payload should be treated as literal string")
    void testSecureModeNeutralizesSqlInjection() {
        String payload = "' OR '1'='1";
        LoginResult result = authService.login(payload, "anything", SecurityMode.SECURE);
        assertFalse(result.isSuccess(), "Secure mode must NEVER authenticate an SQL injection payload");
        assertFalse(result.isSyntaxError(), "Secure mode must handle special characters without syntax errors");
    }

    @Test
    @DisplayName("Vulnerable Mode: Classic SQL injection should successfully bypass authentication")
    void testVulnerableModeBypassed() {
        String payload = "' OR '1'='1' -- ";
        LoginResult result = authService.login(payload, "anything", SecurityMode.VULNERABLE);
        assertTrue(result.isSuccess(), "Vulnerable mode should be exploited by ' OR '1'='1' -- ");
        assertTrue(result.hasUsers(), "Rows should be returned due to query logic bypass");
        assertTrue(result.getUsers().size() >= 1);
    }

    @Test
    @DisplayName("Vulnerable Mode: Unclosed quote should produce a detected syntax error")
    void testVulnerableModeSyntaxError() {
        String brokenPayload = "admin'";
        LoginResult result = authService.login(brokenPayload, "test", SecurityMode.VULNERABLE);
        assertFalse(result.isSuccess());
        assertTrue(result.isSyntaxError(), "Unbalanced quote should be detected as syntax error");
    }

    @Test
    @DisplayName("Graceful handling of database unavailability")
    void testDatabaseFailureHandling() {
        // Mock provider that fails on getConnection
        ConnectionProvider failingProvider = new ConnectionProvider() {
            @Override
            public Connection getConnection() throws SQLException {
                throw new SQLException("Connection refused: database server down");
            }

            @Override
            public DatabaseConfig getConfig() {
                return DatabaseConfig.load();
            }
        };

        AuthenticationService failingService = new AuthenticationService(failingProvider);

        LoginResult secureRes = failingService.login("admin", "Admin@123", SecurityMode.SECURE);
        assertFalse(secureRes.isSuccess());
        assertTrue(secureRes.getMessage().contains("DATABASE ERROR"));

        LoginResult vulnRes = failingService.login("admin", "Admin@123", SecurityMode.VULNERABLE);
        assertFalse(vulnRes.isSuccess());
        assertTrue(vulnRes.getMessage().contains("DATABASE ERROR"));
    }
}
