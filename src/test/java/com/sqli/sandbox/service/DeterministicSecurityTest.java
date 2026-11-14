package com.sqli.sandbox.service;

import com.sqli.sandbox.config.DatabaseConfig;
import com.sqli.sandbox.database.DatabaseManager;
import com.sqli.sandbox.database.SchemaInitializer;
import com.sqli.sandbox.model.TestCaseResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Deterministic Security Test Suite (10 Core Scenarios)")
class DeterministicSecurityTest {

    private static TestCaseRunnerService testRunner;

    @BeforeAll
    static void setUp() throws SQLException {
        // Initialize an isolated in-memory H2 database configured in MySQL compatibility mode
        DatabaseConfig testConfig = DatabaseConfig.createInMemoryH2Config("sqli_test_suite");
        DatabaseManager dbManager = new DatabaseManager(testConfig);

        SchemaInitializer schemaInitializer = new SchemaInitializer(dbManager);
        boolean initialized = schemaInitializer.initializeSchema();
        assertTrue(initialized, "In-memory database schema must initialize successfully");

        AuthenticationService authService = new AuthenticationService(dbManager);
        testRunner = new TestCaseRunnerService(authService);
    }

    @Test
    @DisplayName("Run all 10 deterministic security test scenarios")
    void testAllScenariosPass() {
        List<TestCaseResult> results = testRunner.runAllTests();

        assertEquals(10, results.size(), "Test suite must execute exactly 10 deterministic scenarios");

        long passedCount = results.stream().filter(TestCaseResult::isPassed).count();
        long failedCount = results.stream().filter(r -> !r.isPassed()).count();

        for (TestCaseResult r : results) {
            assertTrue(r.isPassed(), String.format(
                "Test #%d [%s] failed! Expected: %s, Actual: %s, Explanation: %s",
                r.getTestNumber(), r.getTestName(), r.getExpectedOutcome(), r.getActualOutcome(), r.getExplanation()
            ));
        }

        assertEquals(10, passedCount, "All 10 test scenarios must pass");
        assertEquals(0, failedCount, "There should be zero failed tests");
    }

    @Test
    @DisplayName("Verify CSV export excludes sensitive credentials")
    void testCsvExportSecurity() throws Exception {
        List<TestCaseResult> results = testRunner.runAllTests();
        java.io.File tempCsv = java.io.File.createTempFile("sqli_test_export", ".csv");
        tempCsv.deleteOnExit();

        boolean exported = testRunner.exportToCsv(results, tempCsv);
        assertTrue(exported, "CSV export should succeed");

        String content = java.nio.file.Files.readString(tempCsv.toPath());
        assertFalse(content.contains("Admin@123"), "CSV must not contain plaintext password Admin@123");
        assertFalse(content.contains("Alice#Pass99"), "CSV must not contain plaintext password Alice#Pass99");
        assertFalse(content.contains("$2a$10$"), "CSV must not contain BCrypt password hashes");
        assertTrue(content.contains("PASS"), "CSV must contain test results");
    }
}
