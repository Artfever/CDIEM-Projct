package com.project.util;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class DBConnection {
    private static final Path CONFIG_PATH = Path.of("config", "db.properties");
    private static final String URL_KEY = "db.url";
    private static final String USER_KEY = "db.user";
    private static final String PASSWORD_KEY = "db.password";
    private static final String URL_ENV = "CDIEM_DB_URL";
    private static final String USER_ENV = "CDIEM_DB_USER";
    private static final String PASSWORD_ENV = "CDIEM_DB_PASSWORD";

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        Properties properties = loadProperties();

        String url = resolve(properties, URL_KEY, URL_ENV);
        String user = resolve(properties, USER_KEY, USER_ENV);
        String password = resolve(properties, PASSWORD_KEY, PASSWORD_ENV);

        if (url == null || user == null || password == null) {
            throw new IllegalStateException(
                    "Database configuration is missing. Provide db.url, db.user, and db.password in config/db.properties "
                            + "or set CDIEM_DB_URL, CDIEM_DB_USER, and CDIEM_DB_PASSWORD."
            );
        }

        return DriverManager.getConnection(url, user, password);
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();

        if (!Files.exists(CONFIG_PATH)) {
            return properties;
        }

        try (var inputStream = Files.newInputStream(CONFIG_PATH)) {
            properties.load(inputStream);
            return properties;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read database configuration from " + CONFIG_PATH + ".", e);
        }
    }

    private static String resolve(Properties properties, String propertyKey, String envKey) {
        String systemValue = clean(System.getProperty(propertyKey));
        if (systemValue != null) {
            return systemValue;
        }

        String envValue = clean(System.getenv(envKey));
        if (envValue != null) {
            return envValue;
        }

        return clean(properties.getProperty(propertyKey));
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
