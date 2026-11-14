package com.sqli.sandbox.model;

/**
 * Defines the execution mode for SQL evaluation:
 * - VULNERABLE: Direct string concatenation using java.sql.Statement (Simulated Lab Environment).
 * - SECURE: Parameterized SQL using java.sql.PreparedStatement with BCrypt password verification.
 */
public enum SecurityMode {
    VULNERABLE("VULNERABLE (Raw Statement Concatenation)", false),
    SECURE("SECURE (PreparedStatement + BCrypt Verification)", true);

    private final String displayName;
    private final boolean secure;

    SecurityMode(String displayName, boolean secure) {
        this.displayName = displayName;
        this.secure = secure;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isSecure() {
        return secure;
    }
}
