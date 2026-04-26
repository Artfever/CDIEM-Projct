package com.project.repository;

import com.project.model.AuditLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Implementation of AuditRepository using SQL Server.
 * Handles logging case actions and retrieving audit history.
 */
public class AuditRepositoryImpl implements AuditRepository {
    private static final String INSERT_AUDIT_SQL = """
            INSERT INTO AuditLogs (CaseID, Action, PerformedBy)
            VALUES (?, ?, ?)
            """;
    private static final String DELETE_BY_CASE_ID_SQL = """
            DELETE FROM AuditLogs
            WHERE CaseID = ?
            """;
    private static final String FIND_BY_CASE_ID_SQL = """
            SELECT a.LogID, a.CaseID, a.Action, a.PerformedBy, a.[Timestamp], u.Name AS PerformedByName
            FROM AuditLogs a
            JOIN Users u
                ON u.UserID = a.PerformedBy
            WHERE a.CaseID = ?
            ORDER BY a.[Timestamp], a.LogID
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
    public void deleteByCaseId(Connection connection, int caseId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(DELETE_BY_CASE_ID_SQL)) {
            statement.setInt(1, caseId);
            statement.executeUpdate();
        }
    }

    @Override
    public List<AuditLog> findByCaseId(Connection connection, int caseId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_CASE_ID_SQL)) {
            statement.setInt(1, caseId);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<AuditLog> entries = new ArrayList<>();
                while (resultSet.next()) {
                    Timestamp timestamp = resultSet.getTimestamp("Timestamp");
                    entries.add(new AuditLog(
                            resultSet.getInt("LogID"),
                            (Integer) resultSet.getObject("CaseID"),
                            resultSet.getString("Action"),
                            resultSet.getInt("PerformedBy"),
                            resultSet.getString("PerformedByName"),
                            timestamp == null ? null : timestamp.toLocalDateTime()
                    ));
                }
                return entries;
            }
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
