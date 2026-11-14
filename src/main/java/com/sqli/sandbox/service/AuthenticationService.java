package com.sqli.sandbox.service;

import com.sqli.sandbox.database.ConnectionProvider;
import com.sqli.sandbox.model.LoginResult;
import com.sqli.sandbox.model.SecurityMode;

import java.util.Objects;

/**
 * High-level authentication service coordinating between the secure parameterized path
 * and the isolated vulnerable demonstration path.
 */
public class AuthenticationService {

    private final SecureAuthService secureAuthService;
    private final VulnerableAuthService vulnerableAuthService;

    public AuthenticationService(ConnectionProvider connectionProvider) {
        Objects.requireNonNull(connectionProvider, "ConnectionProvider must not be null");
        this.secureAuthService = new SecureAuthService(connectionProvider);
        this.vulnerableAuthService = new VulnerableAuthService(connectionProvider);
    }

    public LoginResult login(String username, String password, SecurityMode mode) {
        if (mode == SecurityMode.SECURE) {
            return secureAuthService.authenticate(username, password);
        } else {
            return vulnerableAuthService.authenticate(username, password);
        }
    }

    public SecureAuthService getSecureAuthService() {
        return secureAuthService;
    }

    public VulnerableAuthService getVulnerableAuthService() {
        return vulnerableAuthService;
    }
}
