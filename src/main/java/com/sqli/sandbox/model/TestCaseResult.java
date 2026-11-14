package com.sqli.sandbox.model;

/**
 * Encapsulates the deterministic outcome of an automated or UI test case.
 */
public final class TestCaseResult {

    private final int testNumber;
    private final String testName;
    private final SecurityMode mode;
    private final String inputUsername;
    private final String inputPasswordMasked;
    private final String expectedOutcome;
    private final String actualOutcome;
    private final boolean passed;
    private final String explanation;

    public TestCaseResult(int testNumber,
                          String testName,
                          SecurityMode mode,
                          String inputUsername,
                          String inputPasswordMasked,
                          String expectedOutcome,
                          String actualOutcome,
                          boolean passed,
                          String explanation) {
        this.testNumber = testNumber;
        this.testName = testName;
        this.mode = mode;
        this.inputUsername = inputUsername;
        this.inputPasswordMasked = inputPasswordMasked;
        this.expectedOutcome = expectedOutcome;
        this.actualOutcome = actualOutcome;
        this.passed = passed;
        this.explanation = explanation;
    }

    public int getTestNumber() {
        return testNumber;
    }

    public String getTestName() {
        return testName;
    }

    public SecurityMode getMode() {
        return mode;
    }

    public String getInputUsername() {
        return inputUsername;
    }

    public String getInputPasswordMasked() {
        return inputPasswordMasked;
    }

    public String getExpectedOutcome() {
        return expectedOutcome;
    }

    public String getActualOutcome() {
        return actualOutcome;
    }

    public boolean isPassed() {
        return passed;
    }

    public String getExplanation() {
        return explanation;
    }
}
