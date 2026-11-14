package com.sqli.sandbox.security;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Provides industry-standard password hashing and constant-time verification using BCrypt.
 * Protects stored credentials against rainbow table and dictionary attacks via adaptive salting.
 */
public final class PasswordHasher {

    // Work factor of 10 provides strong resistance against brute force while keeping desktop latency < 100ms
    private static final int WORK_FACTOR = 10;

    private PasswordHasher() {
        // Utility class, prevent instantiation
    }

    /**
     * Hashes a plaintext password using BCrypt with an automatically generated cryptographically secure salt.
     *
     * @param plainTextPassword the plaintext password to hash
     * @return the resulting BCrypt hash string ($2a$10$...)
     * @throws IllegalArgumentException if the password is null or empty
     */
    public static String hashPassword(String plainTextPassword) {
        if (plainTextPassword == null || plainTextPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty for hashing.");
        }
        String salt = BCrypt.gensalt(WORK_FACTOR);
        return BCrypt.hashpw(plainTextPassword, salt);
    }

    /**
     * Verifies a candidate plaintext password against a stored BCrypt hash.
     * Uses constant-time comparison to protect against timing attacks.
     *
     * @param candidatePassword the candidate plaintext password supplied by the user
     * @param storedHash        the BCrypt hash loaded from the secure database
     * @return true if the password matches the hash; false otherwise
     */
    public static boolean verifyPassword(String candidatePassword, String storedHash) {
        if (candidatePassword == null || storedHash == null) {
            return false;
        }
        if (candidatePassword.isEmpty() || storedHash.trim().isEmpty()) {
            return false;
        }
        try {
            return BCrypt.checkpw(candidatePassword, storedHash);
        } catch (IllegalArgumentException e) {
            // Malformed hash string; fail closed securely
            return false;
        }
    }

    /**
     * CLI utility to generate BCrypt hashes for testing or database manual seeding.
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java com.sqli.sandbox.security.PasswordHasher <password>");
            System.out.println("Default Demo Hashes:");
            System.out.println("  Admin@123     -> " + hashPassword("Admin@123"));
            System.out.println("  Alice#Pass99  -> " + hashPassword("Alice#Pass99"));
            System.out.println("  Bob$Secure2026-> " + hashPassword("Bob$Secure2026"));
            return;
        }
        for (String pwd : args) {
            System.out.println("Password: [" + pwd + "] -> Hash: " + hashPassword(pwd));
        }
    }
}
