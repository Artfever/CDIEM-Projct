package com.project.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AuditRepositoryImpl implements AuditRepository {
    private static final String INSERT_AUDIT_SQL = """
            INSERT INTO AuditLogs (CaseID, Action, PerformedBy)
            VALUES (?, ?, ?)
            """;

    @Override
    public void logAction(Connection connection, Integer caseId, String action, int userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_AUDIT_SQL)) {
            if (caseId == null) {
                statement.setNull(1, java.sql.Types.INTEGER);
            } else {
                statement.setInt(1, caseId);
            }

            statement.setString(2, action);
            statement.setInt(3, userId);
            statement.executeUpdate();
        }
    }
}
