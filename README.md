# ⚡ SQL Injection Sandbox & Visualizer
**Third-Year CSE Object-Oriented Programming (OOP) & Application Security Capstone**

---

## 📌 1. Project Overview

The **SQL Injection Sandbox & Visualizer** is an educational, production-modeled Java Swing desktop application designed to demonstrate the critical difference between **vulnerable, concatenated SQL queries** and **secure, parameterized queries (`PreparedStatement`) with BCrypt password hashing**.

### Core Educational Objectives:
1. **Demonstrate Exploitation**: Visually illustrate how untrusted string interpolation (`java.sql.Statement`) breaks SQL syntax and allows attackers to bypass authentication or leak table contents.
2. **Demonstrate Remediation**: Prove how parameterized queries (`java.sql.PreparedStatement`) treat all inputs strictly as literal data, completely neutralizing SQL injection attacks.
3. **Industry-Grade Credential Security**: Demonstrate salt-based password hashing using `BCrypt` ($2a$10$), showing why plaintext passwords should never be stored or queried in SQL statements.
4. **Resilient Desktop Architecture**: Implement asynchronous background query execution (`SwingWorker`), preventing UI freezing during slow database operations.
5. **Deterministic Security Verification**: Provide an interactive test runner and automated JUnit 5 test suite covering 10 deterministic security scenarios.

---

## 🏗️ 2. Architectural Design & Design Patterns

The codebase is refactored from a monolithic script into a clean, multi-layered architecture:

```
src/main/java/com/sqli/sandbox/
├── config/
│   └── DatabaseConfig.java       # Externalized config loader (.env, db.properties, system env)
├── database/
│   ├── ConnectionProvider.java   # Connection provider interface (Dependency Inversion)
│   ├── DatabaseManager.java      # Connection lifecycle, health check, exception translation
│   └── SchemaInitializer.java    # Automated DDL migration and demonstration seed data
├── model/
│   ├── User.java                 # Credential-safe User model (NO password/hash fields)
│   ├── LoginResult.java          # Immutable query & authentication result container
│   ├── SecurityMode.java         # Execution enum: VULNERABLE vs SECURE
│   └── TestCaseResult.java       # Deterministic test assertion outcome model
├── security/
│   └── PasswordHasher.java       # BCrypt password hashing and constant-time verification
├── service/
│   ├── AuthenticationService.java# Facade coordinating secure & vulnerable execution paths
│   ├── SecureAuthService.java    # Parameterized PreparedStatement + BCrypt verification
│   ├── VulnerableAuthService.java# Concatenated Statement (isolated to synthetic lab schema)
│   └── TestCaseRunnerService.java# Deterministic 10-scenario security test engine
└── ui/
    ├── UITheme.java              # Color tokens, typography, component styling
    ├── ConnectionStatusPanel.java# Live DB connectivity banner & offline In-Memory switch
    ├── LoginFormPanel.java       # Interactive attack workbench & quick payload chips
    ├── QueryVisualizerPanel.java # Split-view SQL template, parameter, & syntax analyzer
    ├── TestRunnerPanel.java      # Test results table, progress bar, CSV report exporter
    ├── CheatSheetPanel.java      # Interactive attack vector cards with 1-click clipboard copy
    └── SQLiSandboxApp.java       # Main JFrame application entry point
```

### Applied Software Engineering Patterns:
- **Separation of Concerns (SoC)**: Presentation logic (`ui`) is strictly separated from business logic (`service`), database access (`database`), and security algorithms (`security`).
- **Facade Pattern**: `AuthenticationService` provides a unified interface hiding the underlying complexities of database drivers, statement compilation, and password verification.
- **Dependency Inversion Principle (DIP)**: Services depend on the `ConnectionProvider` abstraction rather than tightly coupling to a concrete MySQL driver or singleton.
- **Fail-Closed Security**: Any malformed input, null value, or corrupt hash immediately fails closed (`return false`), preventing bypasses via exception swallowing.
- **Worker Thread Decoupling**: Database calls run on `SwingWorker` threads, keeping the Swing Event Dispatch Thread (EDT) 100% responsive.

---

## 💻 3. Prerequisites

- **Java Development Kit (JDK)**: Java 17 LTS or higher (Fully verified on Java 17, 21, and 25).
- **Build Tool**: Maven Wrapper (`./mvnw`) is bundled in the project root — **no standalone Maven installation is required**.
- **Database (Optional)**: MySQL Server 8.0+ running on port 3306.
  > **Note for Practical Exams / Offline Evaluation**: If MySQL is not installed on the evaluation machine, simply toggle **"In-Memory Lab (Offline Mode)"** in the top bar! The built-in H2 in-memory engine runs the complete demonstration seamlessly without any external dependencies!

---

## 🗄️ 4. Database Setup (MySQL)

### Step 1: Initialize Database and Tables
Open your terminal or MySQL Workbench, log in to MySQL as root or an administrative user, and execute the initialization script:

```bash
mysql -u root -p < database/schema.sql
```

Alternatively, copy and paste the contents of `database/schema.sql` into MySQL Workbench.

### Step 2: What `schema.sql` Configures:
1. **Creates Database**: `oops` (UTF-8 `utf8mb4`).
2. **Creates Dedicated Application User**:
   - Create a dedicated local account named `oops_user` yourself and grant it access only to `oops.*`.
   - Choose your own password; do not reuse or publish a password in source control.
   - The application does not create database accounts or grant privileges.
3. **Creates Production Table (`users`)**:
   - Columns: `id`, `username`, `password_hash`, `role`, `created_at`.
   - Contains **NO plaintext passwords**.
   - The Java `SchemaInitializer` seeds secure demo users with BCrypt hashes when the `users` table is empty:
     - `admin` (Password: `Admin@123`, Role: `ADMIN`)
     - `alice` (Password: `Alice#Pass99`, Role: `USER`)
     - `bob` (Password: `Bob$Secure2026`, Role: `USER`)
4. **Creates Isolated Synthetic Lab Table (`vulnerable_lab_users`)**:
   - Columns: `id`, `username`, `password`, `role`.
   - Seeds mock users: `lab_admin`, `john_doe`, `jane_smith`.
   - Kept strictly separated from the secure users table to isolate vulnerable string-concatenation testing.

---

## ⚙️ 5. Configuration

Database settings are externalized and evaluated with the following precedence:
1. **System Environment Variables** (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_TYPE`)
2. **`.env` file** in project root
3. **`db.properties` file** in project root or classpath
4. **Built-in Defaults** (Host: `localhost`, Port: `3306`, Database: `oops`, User: `oops_user`)

### Quick Setup:
Copy the example configuration file:
```bash
cp .env.example .env
```
Edit `.env` to match your local MySQL credentials:
```properties
DB_HOST=localhost
DB_PORT=3306
DB_NAME=oops
DB_USER=oops_user
DB_PASSWORD=your_local_database_password
DB_TYPE=mysql
```
*(Passwords are strictly masked in application logs and diagnostic displays as `********`).*

---

## 🚀 6. Building and Running

### Option A: One-Click Run Script (Recommended)
On macOS / Linux:
```bash
./run.sh
```
On Windows:
```cmd
run.bat
```
*(These scripts automatically compile the application using the bundled Maven Wrapper if not already compiled, and launch the application immediately).*

### Option B: Build and Run via Maven Wrapper
1. **Compile & Package Executable Fat JAR**:
   ```bash
   ./mvnw clean package
   ```
2. **Run the Executable Fat JAR**:
   ```bash
   java -jar target/sqli-sandbox-1.0.0-jar-with-dependencies.jar
   ```

### Option C: Direct Maven Run
```bash
./mvnw exec:java
```

---

## 🧪 7. Automated Testing Suite

To run the complete deterministic automated test suite:
```bash
./mvnw test
```

### Verified Test Classes:
1. **`PasswordHasherTest`** (8 tests):
   - Verifies standard BCrypt format (`$2a$10$...`, length 60).
   - Validates unique salt generation for identical passwords.
   - Validates constant-time positive and negative password verification.
   - Verifies fail-closed handling on null, empty, or malformed inputs.
2. **`DeterministicSecurityTest`** (2 tests):
   - Executes all 10 core security scenarios against an in-memory SQL database.
   - Asserts 100% pass rate.
   - Verifies CSV report export strictly redacts sensitive credentials and hashes.
3. **`AuthenticationServiceTest`** (8 tests):
   - Tests secure authentication success, wrong password rejection, and unknown users.
   - Proves `' OR '1'='1' -- ` is completely neutralized in Secure Mode.
   - Proves `' OR '1'='1' -- ` bypasses authentication in Vulnerable Mode.
   - Validates SQL syntax error detection on unclosed single quotes (`admin'`).
   - Verifies graceful handling when database connectivity is severed.
4. **`DatabaseConfigTest`** (3 tests):
   - Validates configuration defaults, in-memory H2 factory, and password masking.

---

## 🖥️ 8. User Interface Walkthrough

1. **Top Status Bar**:
   - Shows live database host, port, active database, and response latency in milliseconds.
   - **"Test Connection" button**: Evaluates database health in a background thread.
   - **"In-Memory Lab (Offline Mode)" checkbox**: Instantly activates an isolated in-memory H2 database with pre-seeded demonstration data.
2. **Tab 1: Interactive Attack Workbench**:
   - **Mode Toggle Button**: Switches between `🔓 VULNERABLE MODE` (Statement) and `🔒 SECURE MODE` (PreparedStatement).
   - **Warning Banner**: Clearly alerts the user whenever Vulnerable Mode is engaged.
   - **Quick Fill Chips**: One-click fill for testing valid logins (`Alice`), classic bypasses (`' OR '1'='1' -- `), administrative bypass (`lab_admin' -- `), or syntax errors (`admin'`).
   - **Results Area**: Displays authentication outcome, execution time, and returned records (with credentials strictly redacted).
3. **Tab 2: Automated Security Test Suite**:
   - **"▶ Run All 10 Tests"**: Runs the deterministic test battery in a background worker thread with progress bar.
   - **Results Table**: Displays Scenario Name, Mode, Expected Outcome, Actual Outcome, Pass/Fail Status, and Technical Explanation.
   - **Summary Metrics**: Displays Total Tests, Passed, Failed, and Success Percentage.
   - **"Export CSV"**: Exports test results to CSV for project submissions (passwords and hashes are strictly excluded).
4. **Tab 3: Attack Vector Cheat Sheet**:
   - Educational cards explaining the mechanics of Tautologies, Comment Truncation, Numeric Injection, Syntax Probes, and UNION attacks.
   - **"Copy" button**: Copies payload to system clipboard with temporary confirmation.
   - **"Load In Form" button**: Loads the payload directly into the login form and switches to Tab 1.
5. **Lower Half: Query Visualizer**:
   - **Vulnerable Mode**: Displays concatenated SQL, highlights injected syntax in red, and explains how the database parser was tricked.
   - **Secure Mode**: Displays the pre-compiled parameterized template (`?`), shows bound parameter mappings in green, and explains why data values cannot be executed as SQL commands.

---

## 📋 9. Demonstration Script for College Practical Assessment

When demonstrating to your professor or examiner, follow this 5-minute walkthrough:

1. **Step 1: Introduction & Architecture (30 seconds)**
   - Point to the clean separation of concerns (`config`, `database`, `model`, `security`, `service`, `ui`).
   - Highlight that the project uses standard Java Swing, JDBC, BCrypt password hashing, and supports both MySQL and an In-Memory Lab for 100% portability.
2. **Step 2: Demonstrate the Vulnerability (60 seconds)**
   - Select **Vulnerable Mode** (Red banner appears).
   - Click the quick chip `' OR '1'='1' -- ` and click **Execute Authentication**.
   - Show that login was **BYPASSED** and multiple user records leaked!
   - Look at the **Query Visualizer** below: show that the injected `'1'='1'` turned the WHERE clause into a tautology (always TRUE) and `-- ` commented out the password check.
3. **Step 3: Demonstrate SQL Syntax Breaking (45 seconds)**
   - Click the quick chip `admin'` (single quote) and click **Execute Authentication**.
   - Show that the unescaped single quote broke SQL string delimiters, throwing a syntax error.
4. **Step 4: Demonstrate Secure Remediation (90 seconds)**
   - Toggle to **Secure Mode** (Green banner appears).
   - Click `' OR '1'='1' -- ` and click **Execute Authentication**.
   - Show that the attack is **COMPLETELY BLOCKED**!
   - Look at the **Query Visualizer**: point out the `?` placeholder in `SELECT id, username, password_hash, role, created_at FROM users WHERE username = ?`.
   - Explain that the database parser treats the payload as literal characters, searching for a user whose username literally is `' OR '1'='1' -- `, which does not exist!
5. **Step 5: Demonstrate Password Security & Legitimate Login (45 seconds)**
   - Click `Valid (Alice)`: Username `alice`, Password `Alice#Pass99`.
   - Click **Execute Authentication**: Login succeeds!
   - Explain that the SQL query only retrieved the password hash; BCrypt verified the password in application memory using a constant-time algorithm.
6. **Step 6: Run Automated Test Suite (30 seconds)**
   - Switch to the **Automated Security Test Suite** tab and click **Run All 10 Tests**.
   - Show that all 10 deterministic tests pass (100% Success Rate). Click **Export CSV** to show export capabilities.

---

## 💡 10. Viva Questions & Model Answers

### Q1: Why does `PreparedStatement` prevent SQL injection?
> **Answer**: `PreparedStatement` pre-compiles the SQL query template into an abstract syntax tree (AST) on the database engine *before* user parameters are bound. When values are subsequently supplied via `setString()`, the database treats them strictly as literal data values, never as executable SQL code. Even if an attacker injects quotes, SQL keywords, or comments, they cannot alter the query structure.

### Q2: Why is direct string concatenation with `Statement` vulnerable?
> **Answer**: When concatenating strings (`"WHERE username = '" + user + "'"`), the database parser receives a single raw string containing both code and untrusted data. Metacharacters like single quotes (`'`) escape the literal string boundary, allowing the attacker to inject new SQL clauses (like `OR '1'='1'`) or comment markers (`--`), hijacking the query logic.

### Q3: Why is input sanitization (blacklisting) insufficient compared to parameterization?
> **Answer**: Blacklists attempt to identify and remove "bad" characters (like `'`, `OR`, `--`). However, attackers routinely bypass blacklists using alternative encodings, Unicode variants, case manipulation, or novel SQL syntax. Parameterization represents a structural defense that fundamentally separates code from data, offering mathematical protection regardless of input characters.

### Q4: How does BCrypt protect passwords, and why is SHA-256 alone insufficient?
> **Answer**: General-purpose cryptographic hash functions like SHA-256 or MD5 are designed to be fast, allowing attackers with GPUs to calculate billions of hashes per second using dictionary or rainbow table attacks. BCrypt is a slow, adaptive password hashing algorithm that incorporates an automatic cryptographic salt (preventing rainbow tables) and a tunable work factor (cost parameter) that forces brute-force attacks to be computationally intractable.

### Q5: Why shouldn't password verification be done inside the SQL query (`WHERE username=? AND password=?`)?
> **Answer**: If passwords are verified directly in SQL, they would have to be stored in plaintext or computed with fast, unsalted hashes. Furthermore, verifying passwords in the application layer (`BCrypt.checkpw()`) allows us to enforce constant-time string comparison (mitigating timing attacks) and prevents sensitive credentials from appearing in SQL server query logs.

### Q6: What is the purpose of `SwingWorker` in this desktop application?
> **Answer**: Java Swing is single-threaded; all UI rendering and event handling occurs on the Event Dispatch Thread (EDT). If blocking network operations (such as JDBC database queries) are executed on the EDT, the GUI freezes and stops responding. `SwingWorker` moves database I/O to a background worker thread and posts results back to the EDT asynchronously once complete.

### Q7: Why are passwords and hashes excluded from the `User` model?
> **Answer**: Adhering to the Principle of Least Privilege and Information Hiding, domain entities should only contain data required by downstream consumers. The presentation layer needs `id`, `username`, and `role` to render the user interface. Holding credentials in `User` objects risks accidental exposure through logging, debugging, serialization, or UI rendering.

### Q8: What is a Tautology attack in SQL injection?
> **Answer**: A tautology is a mathematical statement that is always true. In SQL injection, appending `' OR '1'='1'` into a WHERE clause transforms the condition into `TRUE`. Because every row in the table satisfies the condition, the query returns all records, allowing unauthorized authentication bypass or mass data leakage.

### Q9: Why is the vulnerable demonstration isolated to `vulnerable_lab_users`?
> **Answer**: In real-world applications, security demonstrations should never compromise real user data or production databases. By isolating the intentional string-concatenation vulnerability to a dedicated synthetic table containing dummy test accounts, we demonstrate exploit mechanics safely without introducing systemic vulnerabilities.

### Q10: How does the connection-per-operation pattern in `DatabaseManager` improve upon a static `Connection` singleton?
> **Answer**: A shared static `Connection` object is not thread-safe and can become invalid if the database restarts or the network drops. In our implementation, connections are retrieved on demand and closed immediately using `try-with-resources`. This ensures deterministic resource cleanup, prevents connection leaks, and ensures multi-threaded safety.

---

## 🔒 11. Security Limitations & Lab Boundaries

1. **Simulated Exploit Scope**: The vulnerable authentication endpoint is strictly confined to `vulnerable_lab_users`. It is intended solely for local desktop education and practical academic grading.
2. **Network Security**: In a production deployment, MySQL traffic should enforce TLS/SSL encryption (`useSSL=true`) and client certificate validation.
3. **Privilege Isolation**: The MySQL account `oops_user` should be granted minimum necessary DML privileges (`SELECT, INSERT, UPDATE, DELETE`) and never administrative privileges (`GRANT`, `SUPER`, `FILE`).
