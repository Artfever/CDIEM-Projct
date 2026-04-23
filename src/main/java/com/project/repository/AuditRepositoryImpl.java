package com.project.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

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

    @Override
    public Optional<Integer> findMostRecentActorByActionPrefixes(Connection connection, int caseId,
                                                                 List<String> actionPrefixes) throws SQLException {
        if (actionPrefixes == null || actionPrefixes.isEmpty()) {
            return Optional.empty();
        }

        StringJoiner conditions = new StringJoiner(" OR ");
        for (int i = 0; i < actionPrefixes.size(); i++) {
            conditions.add("Action LIKE ?");
        }

        String sql = """
                SELECT TOP (1) PerformedBy
                FROM AuditLogs
                WHERE CaseID = ?
                  AND (%s)
                ORDER BY [Timestamp] DESC, LogID DESC
                """.formatted(conditions);

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, caseId);
            for (int i = 0; i < actionPrefixes.size(); i++) {
                statement.setString(i + 2, actionPrefixes.get(i));
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                return Optional.of(resultSet.getInt("PerformedBy"));
            }
        }
    }
}
