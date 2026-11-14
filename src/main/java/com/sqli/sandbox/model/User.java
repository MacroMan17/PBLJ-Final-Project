package com.sqli.sandbox.model;

import java.util.Objects;

/**
 * Represents an authenticated or queried user entity.
 * In accordance with application security best practices, this model
 * NEVER contains sensitive credentials, passwords, or password hashes.
 */
public final class User {

    private final int id;
    private final String username;
    private final String role;
    private final String createdAt;

    public User(int id, String username, String role, String createdAt) {
        this.id = id;
        this.username = Objects.requireNonNull(username, "Username must not be null");
        this.role = (role == null || role.isBlank()) ? "USER" : role;
        this.createdAt = (createdAt == null) ? "" : createdAt;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return String.format("User[id=%d, username='%s', role='%s']", id, username, role);
    }
}
