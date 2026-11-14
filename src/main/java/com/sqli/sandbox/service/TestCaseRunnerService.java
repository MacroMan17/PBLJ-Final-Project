package com.sqli.sandbox.service;

import com.sqli.sandbox.model.LoginResult;
import com.sqli.sandbox.model.SecurityMode;
import com.sqli.sandbox.model.TestCaseResult;
import com.sqli.sandbox.security.PasswordHasher;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Executes a deterministic test suite covering the 10 required security scenarios.
 * Verifies SQL injection prevention, authentication mechanics, syntax error handling,
 * and BCrypt password hashing.
 */
public class TestCaseRunnerService {

    private final AuthenticationService authService;

    public TestCaseRunnerService(AuthenticationService authService) {
        this.authService = authService;
    }

    /**
     * Executes all 10 security test scenarios and returns the results.
     */
    public List<TestCaseResult> runAllTests() {
        List<TestCaseResult> results = new ArrayList<>();

        // Test 1: Correct Credentials (Secure Mode)
        {
            LoginResult r = authService.login("alice", "Alice#Pass99", SecurityMode.SECURE);
            boolean passed = r.isSuccess() && r.hasUsers() && "alice".equals(r.getUsers().get(0).getUsername());
            results.add(new TestCaseResult(
                1,
                "Valid Credentials Authentication",
                SecurityMode.SECURE,
                "alice",
                "********",
                "Authentication Success",
                r.isSuccess() ? "Authentication Success" : "Authentication Failed (" + r.getMessage() + ")",
                passed,
                "Valid user credentials correctly verified via BCrypt hash comparison."
            ));
        }

        // Test 2: Incorrect Password (Secure Mode)
        {
            LoginResult r = authService.login("alice", "WrongPassword123", SecurityMode.SECURE);
            boolean passed = !r.isSuccess() && !r.hasUsers();
            results.add(new TestCaseResult(
                2,
                "Incorrect Password Rejection",
                SecurityMode.SECURE,
                "alice",
                "********",
                "Authentication Failed",
                !r.isSuccess() ? "Authentication Failed" : "Authentication Succeeded (Unexpected)",
                passed,
                "Invalid password correctly rejected by BCrypt verification without leaking hash."
            ));
        }

        // Test 3: Unknown Username (Secure Mode)
        {
            LoginResult r = authService.login("nonexistent_user_999", "RandomPass", SecurityMode.SECURE);
            boolean passed = !r.isSuccess() && !r.hasUsers();
            results.add(new TestCaseResult(
                3,
                "Unknown Username Rejection",
                SecurityMode.SECURE,
                "nonexistent_user_999",
                "********",
                "Authentication Failed",
                !r.isSuccess() ? "Authentication Failed" : "Authentication Succeeded (Unexpected)",
                passed,
                "Query returns zero rows; application safely fails closed."
            ));
        }

        // Test 4: Empty Input Validation (Secure Mode)
        {
            LoginResult r = authService.login("", "", SecurityMode.SECURE);
            boolean passed = !r.isSuccess() && r.getMessage().contains("Validation Failed");
            results.add(new TestCaseResult(
                4,
                "Empty Input Rejection",
                SecurityMode.SECURE,
                "<EMPTY>",
                "<EMPTY>",
                "Validation Failed",
                !r.isSuccess() ? "Validation Failed" : "Processed Query (Unexpected)",
                passed,
                "Empty inputs rejected prior to executing unnecessary database queries."
            ));
        }

        // Test 5: Quote-Containing Input (Legitimate Name) (Secure Mode)
        {
            String quoteName = "O'Connor";
            LoginResult r = authService.login(quoteName, "AnyPass", SecurityMode.SECURE);
            // PreparedStatement treats quote as literal character; no syntax error occurs
            boolean passed = !r.isSyntaxError() && !r.getMessage().contains("SQL ERROR");
            results.add(new TestCaseResult(
                5,
                "Quote-Containing Input (O'Connor)",
                SecurityMode.SECURE,
                quoteName,
                "********",
                "Safe Literal Query (No Syntax Error)",
                !r.isSyntaxError() ? "Safe Literal Query (No Syntax Error)" : "Syntax Error Caused",
                passed,
                "PreparedStatement escapes quotes automatically, treating input as literal text."
            ));
        }

        // Test 6: SQLi Tautology Bypass in Vulnerable Mode (' OR '1'='1' -- )
        {
            String sqliPayload = "' OR '1'='1' -- ";
            LoginResult r = authService.login(sqliPayload, "anything", SecurityMode.VULNERABLE);
            boolean passed = r.isSuccess() && r.hasUsers();
            results.add(new TestCaseResult(
                6,
                "SQLi Tautology Bypass (Vulnerable Mode)",
                SecurityMode.VULNERABLE,
                sqliPayload,
                "********",
                "Authentication Bypassed (Exploit Success)",
                r.isSuccess() ? "Authentication Bypassed (Exploit Success)" : "Bypass Failed (" + r.getMessage() + ")",
                passed,
                "Vulnerable Statement string concatenation allowed '1'='1 tautology with comment to bypass auth."
            ));
        }

        // Test 7: SQLi Tautology Attack in Secure Mode (' OR '1'='1' -- )
        {
            String sqliPayload = "' OR '1'='1' -- ";
            LoginResult r = authService.login(sqliPayload, "anything", SecurityMode.SECURE);
            boolean passed = !r.isSuccess() && !r.isSyntaxError();
            results.add(new TestCaseResult(
                7,
                "SQLi Tautology Blocked (Secure Mode)",
                SecurityMode.SECURE,
                sqliPayload,
                "********",
                "Attack Blocked / Auth Failed",
                !r.isSuccess() ? "Attack Blocked / Auth Failed" : "Attack Succeeded (Vulnerability!)",
                passed,
                "PreparedStatement bound attack payload strictly as literal username string; query failed safely."
            ));
        }

        // Test 8: Comment Injection in Vulnerable Mode (admin' --)
        {
            String commentPayload = "lab_admin' --";
            LoginResult r = authService.login(commentPayload, "wrong_pass", SecurityMode.VULNERABLE);
            boolean passed = r.isSuccess() && r.hasUsers();
            results.add(new TestCaseResult(
                8,
                "SQLi Comment Injection (Vulnerable Mode)",
                SecurityMode.VULNERABLE,
                commentPayload,
                "********",
                "Password Check Commented Out",
                r.isSuccess() ? "Password Check Commented Out" : "Bypass Failed (" + r.getMessage() + ")",
                passed,
                "Injected '--' truncated SQL execution, skipping password verification in vulnerable mode."
            ));
        }

        // Test 9: SQL Syntax Error from Unbalanced Quote (Vulnerable Mode)
        {
            String unclosedQuote = "admin'";
            LoginResult r = authService.login(unclosedQuote, "any", SecurityMode.VULNERABLE);
            boolean passed = r.isSyntaxError() || r.getMessage().contains("SYNTAX ERROR") || r.getMessage().contains("error in your sql syntax");
            results.add(new TestCaseResult(
                9,
                "Unbalanced Quote Syntax Error Detection",
                SecurityMode.VULNERABLE,
                unclosedQuote,
                "********",
                "SQL Syntax Error Detected",
                passed ? "SQL Syntax Error Detected" : "No Syntax Error Reported",
                passed,
                "Unbalanced single quote breaks string literal delimiter in vulnerable mode, proving SQL injection exposure."
            ));
        }

        // Test 10: BCrypt Password Hashing & Salt Verification
        {
            String candidate = "TestingSecret2026!";
            String hash1 = PasswordHasher.hashPassword(candidate);
            String hash2 = PasswordHasher.hashPassword(candidate);

            boolean hashesDiffer = !hash1.equals(hash2); // Different salts
            boolean verify1 = PasswordHasher.verifyPassword(candidate, hash1);
            boolean verifyWrong = !PasswordHasher.verifyPassword("WrongPassword", hash1);

            boolean passed = hashesDiffer && verify1 && verifyWrong;
            results.add(new TestCaseResult(
                10,
                "BCrypt Adaptive Salt & Verification",
                SecurityMode.SECURE,
                "security_engine",
                "********",
                "Unique Salts & Constant-Time Verification",
                passed ? "Unique Salts & Constant-Time Verification" : "Verification Failed",
                passed,
                "BCrypt generates distinct random salts per hash and reliably validates correct credentials."
            ));
        }

        return results;
    }

    /**
     * Exports test results to a CSV file.
     * Enforces strict security by NEVER exporting passwords, hashes, or credentials.
     */
    public boolean exportToCsv(List<TestCaseResult> results, File targetFile) {
        if (targetFile == null || results == null) {
            return false;
        }
        try (FileWriter writer = new FileWriter(targetFile)) {
            writer.write("TestNumber,TestName,SecurityMode,InputUsername,InputPasswordMask,ExpectedOutcome,ActualOutcome,Status,Explanation\n");
            for (TestCaseResult t : results) {
                writer.write(String.format(
                    "%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                    t.getTestNumber(),
                    escapeCsv(t.getTestName()),
                    t.getMode().name(),
                    escapeCsv(t.getInputUsername()),
                    escapeCsv(t.getInputPasswordMasked()),
                    escapeCsv(t.getExpectedOutcome()),
                    escapeCsv(t.getActualOutcome()),
                    t.isPassed() ? "PASS" : "FAIL",
                    escapeCsv(t.getExplanation())
                ));
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        return val.replace("\"", "\"\"");
    }
}
