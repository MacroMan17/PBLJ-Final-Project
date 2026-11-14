package com.sqli.sandbox.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable container encapsulating the results of an authentication or SQL injection demonstration.
 * Never stores or exposes raw candidate passwords or database password hashes.
 */
public final class LoginResult {

    private final boolean success;
    private final SecurityMode mode;
    private final String queryTemplate;
    private final String executedQuery;
    private final List<String> boundParameters;
    private final String message;
    private final List<User> users;
    private final boolean syntaxError;
    private final long executionTimeMs;

    public LoginResult(boolean success,
                       SecurityMode mode,
                       String queryTemplate,
                       String executedQuery,
                       List<String> boundParameters,
                       String message,
                       List<User> users,
                       boolean syntaxError,
                       long executionTimeMs) {
        this.success = success;
        this.mode = mode;
        this.queryTemplate = (queryTemplate != null) ? queryTemplate : "";
        this.executedQuery = (executedQuery != null) ? executedQuery : "";
        this.boundParameters = (boundParameters != null) ? Collections.unmodifiableList(new ArrayList<>(boundParameters)) : Collections.emptyList();
        this.message = (message != null) ? message : "";
        this.users = (users != null) ? Collections.unmodifiableList(new ArrayList<>(users)) : Collections.emptyList();
        this.syntaxError = syntaxError;
        this.executionTimeMs = executionTimeMs;
    }

    public boolean isSuccess() {
        return success;
    }

    public SecurityMode getMode() {
        return mode;
    }

    public String getQueryTemplate() {
        return queryTemplate;
    }

    public String getExecutedQuery() {
        return executedQuery;
    }

    public List<String> getBoundParameters() {
        return boundParameters;
    }

    public String getMessage() {
        return message;
    }

    public List<User> getUsers() {
        return users;
    }

    public boolean hasUsers() {
        return !users.isEmpty();
    }

    public boolean isSyntaxError() {
        return syntaxError;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public int getRowCount() {
        return users.size();
    }
}
