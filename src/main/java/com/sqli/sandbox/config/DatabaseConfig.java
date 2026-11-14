package com.sqli.sandbox.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Externalized database configuration reader.
 * Evaluates configuration in the following order of precedence:
 * 1. System Environment Variables (DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD)
 * 2. .env file in the current working directory
 * 3. db.properties in the working directory or classpath
 * 4. Fallback defaults (host: localhost, port: 3306, name: oops, user: oops_user)
 *
 * Enforces strict security by NEVER printing or logging the actual database password.
 */
public final class DatabaseConfig {

    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 3306;
    public static final String DEFAULT_DATABASE = "oops";
    public static final String DEFAULT_USER = "oops_user";

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final String dbType; // "mysql" or "h2"
    private final String configSource;

    private DatabaseConfig(String host, int port, String database, String username, String password, String dbType, String configSource) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = (password == null) ? "" : password;
        this.dbType = (dbType == null || dbType.isBlank()) ? "mysql" : dbType.toLowerCase().trim();
        this.configSource = configSource;
    }

    /**
     * Loads the active database configuration from environment variables, .env, or db.properties.
     */
    public static DatabaseConfig load() {
        Properties fileProps = new Properties();
        String source = "Defaults";

        // Try reading .env file first
        File envFile = new File(".env");
        if (envFile.exists() && envFile.isFile()) {
            try (FileInputStream fis = new FileInputStream(envFile)) {
                fileProps.load(fis);
                source = ".env file";
            } catch (IOException ignored) {}
        }

        // Try reading db.properties if .env did not supply everything
        File propFile = new File("db.properties");
        if (propFile.exists() && propFile.isFile()) {
            try (FileInputStream fis = new FileInputStream(propFile)) {
                Properties p = new Properties();
                p.load(fis);
                for (String key : p.stringPropertyNames()) {
                    fileProps.putIfAbsent(key, p.getProperty(key));
                }
                source = (source.equals("Defaults")) ? "db.properties file" : source + " + db.properties";
            } catch (IOException ignored) {}
        }

        // Also check classpath for db.properties
        try (InputStream is = DatabaseConfig.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (is != null) {
                Properties cp = new Properties();
                cp.load(is);
                for (String key : cp.stringPropertyNames()) {
                    fileProps.putIfAbsent(key, cp.getProperty(key));
                }
            }
        } catch (IOException ignored) {}

        String host = getVal("DB_HOST", fileProps, DEFAULT_HOST);
        String portStr = getVal("DB_PORT", fileProps, String.valueOf(DEFAULT_PORT));
        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            port = DEFAULT_PORT;
        }

        String dbName = getVal("DB_NAME", fileProps, DEFAULT_DATABASE);
        String user = getVal("DB_USER", fileProps, DEFAULT_USER);
        String pass = getVal("DB_PASSWORD", fileProps, "");
        String dbType = getVal("DB_TYPE", fileProps, "mysql");

        if (System.getenv("DB_HOST") != null || System.getenv("DB_PASSWORD") != null) {
            source = "Environment Variables" + (source.equals("Defaults") ? "" : " + " + source);
        }

        return new DatabaseConfig(host, port, dbName, user, pass, dbType, source);
    }

    /**
     * Creates an in-memory H2 configuration for unit testing or offline simulation.
     */
    public static DatabaseConfig createInMemoryH2Config(String dbName) {
        return new DatabaseConfig("localhost", 0, dbName, "sa", "", "h2", "In-Memory H2 (Test Mode)");
    }

    private static String getVal(String key, Properties props, String defaultVal) {
        // 1. Environment variable
        String val = System.getenv(key);
        if (val != null && !val.isBlank()) {
            return val.trim();
        }
        // 2. File properties
        val = props.getProperty(key);
        if (val != null && !val.isBlank()) {
            return val.trim();
        }
        // Also check lowercase with dot (e.g., db.host)
        String dotKey = key.toLowerCase().replace('_', '.');
        val = props.getProperty(dotKey);
        if (val != null && !val.isBlank()) {
            return val.trim();
        }
        return defaultVal;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDatabase() {
        return database;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getDbType() {
        return dbType;
    }

    public String getConfigSource() {
        return configSource;
    }

    /**
     * Builds the JDBC Connection URL.
     * Uses strict timeouts (3s connect, 5s socket) so that the application fails fast
     * if the target database host or port is offline.
     */
    public String getJdbcUrl() {
        if ("h2".equalsIgnoreCase(dbType)) {
            return "jdbc:h2:mem:" + database + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        }
        return String.format(
            "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=3000&socketTimeout=5000",
            host, port, database
        );
    }

    /**
     * Safely masks the password for diagnostic output and logs.
     * NEVER prints the raw password.
     */
    public String getMaskedPassword() {
        if (password == null || password.isEmpty()) {
            return "<EMPTY / NOT SET>";
        }
        return "******** (length: " + password.length() + ")";
    }

    @Override
    public String toString() {
        return String.format(
            "DatabaseConfig[type=%s, host=%s, port=%d, db=%s, user=%s, pass=%s, source=%s]",
            dbType, host, port, database, username, getMaskedPassword(), configSource
        );
    }
}
