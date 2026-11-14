package com.sqli.sandbox.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PasswordHasher Security Unit Tests")
class PasswordHasherTest {

    @Test
    @DisplayName("Should generate valid BCrypt hash with expected format ($2a$10$)")
    void testHashPasswordFormat() {
        String hash = PasswordHasher.hashPassword("Admin@123");
        assertNotNull(hash, "Generated hash must not be null");
        assertTrue(hash.startsWith("$2a$10$"), "BCrypt hash must start with $2a$10$ prefix");
        assertEquals(60, hash.length(), "Standard BCrypt hash must be exactly 60 characters long");
    }

    @Test
    @DisplayName("Should produce different salts for identical passwords")
    void testSaltingUniqueness() {
        String hash1 = PasswordHasher.hashPassword("SuperSecret123");
        String hash2 = PasswordHasher.hashPassword("SuperSecret123");
        assertNotEquals(hash1, hash2, "Identical passwords must produce distinct salted hashes");
        assertTrue(PasswordHasher.verifyPassword("SuperSecret123", hash1));
        assertTrue(PasswordHasher.verifyPassword("SuperSecret123", hash2));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Admin@123", "Alice#Pass99", "Bob$Secure2026", "Complex!@#$%^&*()"})
    @DisplayName("Should verify valid candidate passwords against their hashes")
    void testSuccessfulVerification(String password) {
        String hash = PasswordHasher.hashPassword(password);
        assertTrue(PasswordHasher.verifyPassword(password, hash), "Password should match its own hash");
    }

    @Test
    @DisplayName("Should reject incorrect candidate passwords")
    void testFailedVerification() {
        String hash = PasswordHasher.hashPassword("CorrectHorseBatteryStaple");
        assertFalse(PasswordHasher.verifyPassword("WrongPassword", hash), "Incorrect password must be rejected");
        assertFalse(PasswordHasher.verifyPassword("correcthorsebatterystaple", hash), "Password verification must be case-sensitive");
    }

    @Test
    @DisplayName("Should fail safely when given null, empty, or malformed inputs")
    void testEdgeCases() {
        String validHash = PasswordHasher.hashPassword("ValidPass123");

        assertFalse(PasswordHasher.verifyPassword(null, validHash), "Null password must return false");
        assertFalse(PasswordHasher.verifyPassword("", validHash), "Empty password must return false");
        assertFalse(PasswordHasher.verifyPassword("   ", validHash), "Whitespace password must return false");
        assertFalse(PasswordHasher.verifyPassword("ValidPass123", null), "Null hash must return false");
        assertFalse(PasswordHasher.verifyPassword("ValidPass123", ""), "Empty hash must return false");
        assertFalse(PasswordHasher.verifyPassword("ValidPass123", "invalid-hash-string"), "Malformed hash must return false without throwing");

        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hashPassword(null));
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hashPassword(""));
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hashPassword("   "));
    }
}
