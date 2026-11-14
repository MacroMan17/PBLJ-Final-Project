package com.sqli.sandbox.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DatabaseConfig Configuration Unit Tests")
class DatabaseConfigTest {

    @Test
    @DisplayName("Should load default configuration correctly")
    void testDefaultConfig() {
        DatabaseConfig config = DatabaseConfig.load();
        assertNotNull(config.getHost());
        assertTrue(config.getPort() > 0);
        assertNotNull(config.getDatabase());
        assertNotNull(config.getUsername());
        assertNotNull(config.getJdbcUrl());
    }

    @Test
    @DisplayName("Should create valid In-Memory H2 config for testing")
    void testInMemoryH2Config() {
        DatabaseConfig h2Config = DatabaseConfig.createInMemoryH2Config("test_db");
        assertEquals("h2", h2Config.getDbType());
        assertTrue(h2Config.getJdbcUrl().startsWith("jdbc:h2:mem:test_db"));
        assertEquals("sa", h2Config.getUsername());
    }

    @Test
    @DisplayName("Masked password must never reveal secret plaintext")
    void testPasswordMasking() {
        DatabaseConfig config = DatabaseConfig.load();
        String masked = config.getMaskedPassword();
        assertNotNull(masked);
        assertFalse(masked.contains("admin1234"), "Raw password must not be present in masked string");
    }
}
