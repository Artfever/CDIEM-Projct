package com.project.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DBConnection {
    private static final String URL =
            "jdbc:sqlserver://localhost\\SQLEXPRESS;databaseName=CDIEM;encrypt=true;trustServerCertificate=true";
    private static final String USER = "TAHASOHAIL";
    private static final String PASSWORD = "1234";

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
