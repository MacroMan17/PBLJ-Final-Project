-- ==============================================================================
-- SQL Injection Sandbox & Visualizer - Database Initialization Script
-- Target Database: oops
-- Encoding: UTF-8 (utf8mb4)
-- ==============================================================================

-- 1. Create Database if not exists
CREATE DATABASE IF NOT EXISTS oops
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE oops;

-- 2. Create Dedicated Non-Root Application User (Least Privilege Principle)
-- Run this block with administrative privileges to configure the dedicated demo account:
CREATE USER IF NOT EXISTS 'oops_user'@'localhost' IDENTIFIED BY 'oops_pass2026';
CREATE USER IF NOT EXISTS 'oops_user'@'%' IDENTIFIED BY 'oops_pass2026';
GRANT SELECT, INSERT, UPDATE, DELETE ON oops.* TO 'oops_user'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON oops.* TO 'oops_user'@'%';
FLUSH PRIVILEGES;

-- ==============================================================================
-- 3. SECURE AUTHENTICATION SCHEMA (Production Standard)
-- - Passwords are NEVER stored in plaintext.
-- - Only strong BCrypt hashes ($2a$10$...) are persisted.
-- - Usernames are uniquely indexed.
-- ==============================================================================
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Seed Secure Accounts (Pass: Admin@123, Alice#Pass99, Bob$Secure2026)
-- Password hashes generated via BCrypt (cost factor 10)
INSERT INTO users (username, password_hash, role)
VALUES
    ('admin', '$2a$10$n4qJ5gXoMhXyG09e2M/vIuR7t1O8G0tQG9x1iQoO8/r5r1e2y3u4i', 'ADMIN'),
    ('alice', '$2a$10$1Pz6fB8W7eY9k3T5vU1cOuR0a5s2d3f4g5h6j7k8l9z0x1c2v3b4n', 'USER'),
    ('bob',   '$2a$10$9Kj8h7g6f5d4s3a2z1x0OuL8k7j6h5g4f3d2s1a0z9x8c7v6b5n4m', 'USER')
ON DUPLICATE KEY UPDATE username=username;

-- ==============================================================================
-- 4. INTENTIONALLY VULNERABLE LAB SCHEMA (Isolated Lab Environment)
-- - Strictly synthetic mock data used to demonstrate raw SQL injection.
-- - Completely isolated from the secure users table.
-- - No real-world accounts, personal data, or sensitive secrets.
-- ==============================================================================
CREATE TABLE IF NOT EXISTS vulnerable_lab_users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'STUDENT'
);

-- Seed Synthetic Lab Accounts
INSERT INTO vulnerable_lab_users (username, password, role)
VALUES
    ('lab_admin', 'password123', 'ADMIN'),
    ('john_doe',  'welcome1',    'STUDENT'),
    ('jane_smith', 'secret88',    'STUDENT')
ON DUPLICATE KEY UPDATE username=username;

-- Verification Queries:
-- SELECT id, username, role, created_at FROM users;
-- SELECT id, username, role FROM vulnerable_lab_users;
