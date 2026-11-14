-- =====================================================
-- SQL Injection Sandbox & Visualizer
-- Database: oops
-- =====================================================

CREATE DATABASE IF NOT EXISTS oops
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE oops;

-- =====================================================
-- 1. SECURE USERS TABLE
-- Passwords are stored as BCrypt hashes.
-- =====================================================

CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- 2. VULNERABLE LAB USERS TABLE
-- Synthetic accounts for SQL injection demonstrations.
-- =====================================================

CREATE TABLE IF NOT EXISTS vulnerable_lab_users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'STUDENT'
);

-- =====================================================
-- 3. SEED SYNTHETIC LAB ACCOUNTS
-- =====================================================

INSERT INTO vulnerable_lab_users
    (username, password, role)
VALUES
    ('lab_admin', 'password123', 'ADMIN'),
    ('john_doe', 'welcome1', 'STUDENT'),
    ('jane_smith', 'secret88', 'STUDENT')
ON DUPLICATE KEY UPDATE
    username = VALUES(username);

-- =====================================================
-- 4. VERIFICATION
-- =====================================================

SELECT id, username, role, created_at FROM users;

SELECT id, username, role FROM vulnerable_lab_users;