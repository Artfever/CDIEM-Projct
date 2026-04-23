package com.project.repository;

import com.project.model.CaseClosureDecision;
import com.project.model.CaseClosureDecisionType;
import com.project.model.CaseState;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Optional;

public class CaseClosureDecisionRepositoryImpl implements CaseClosureDecisionRepository {
    private static final String INSERT_SQL = """
            INSERT INTO CaseClosureDecisions (CaseID, DecisionType, Reason, PreviousState, ResultingState, DecidedByUserID)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
    private static final String FIND_LATEST_BY_CASE_ID_SQL = """
            SELECT TOP (1)
                   d.DecisionID,
                   d.CaseID,
                   d.DecisionType,
                   d.Reason,
                   d.PreviousState,
                   d.ResultingState,
                   d.DecidedByUserID,
                   d.DecidedAt,
                   u.Name AS DecidedByName
            FROM CaseClosureDecisions d
            JOIN Users u
                ON u.UserID = d.DecidedByUserID
            WHERE d.CaseID = ?
            ORDER BY d.DecidedAt DESC, d.DecisionID DESC
            """;

    @Override
    public int save(Connection connection, int caseId, CaseClosureDecisionType decisionType, String reason,
                    CaseState previousState, CaseState resultingState, int decidedByUserId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, caseId);
            statement.setString(2, decisionType.name());

            String cleanedReason = clean(reason);
            if (cleanedReason == null) {
                statement.setNull(3, java.sql.Types.NVARCHAR);
            } else {
                statement.setString(3, cleanedReason);
            }

            statement.setString(4, previousState.name());
            statement.setString(5, resultingState.name());
            statement.setInt(6, decidedByUserId);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }

        throw new SQLException("Case closure decision insert completed without returning a generated DecisionID.");
    }

    @Override
    public Optional<CaseClosureDecision> findLatestByCaseId(Connection connection, int caseId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(FIND_LATEST_BY_CASE_ID_SQL)) {
            statement.setInt(1, caseId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                Timestamp decidedAt = resultSet.getTimestamp("DecidedAt");
                return Optional.of(new CaseClosureDecision(
                        resultSet.getInt("DecisionID"),
                        resultSet.getInt("CaseID"),
                        CaseClosureDecisionType.valueOf(resultSet.getString("DecisionType")),
                        resultSet.getString("Reason"),
                        CaseState.valueOf(resultSet.getString("PreviousState")),
                        CaseState.valueOf(resultSet.getString("ResultingState")),
                        resultSet.getInt("DecidedByUserID"),
                        resultSet.getString("DecidedByName"),
                        decidedAt == null ? null : decidedAt.toLocalDateTime()
                ));
            }
        }
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
