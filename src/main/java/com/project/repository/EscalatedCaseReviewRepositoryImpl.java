package com.project.repository;

import com.project.model.EscalatedCaseReview;
import com.project.model.PriorityState;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Optional;

/**
 * Implementation of EscalatedCaseReviewRepository using SQL Server.
 * Handles storing supervisor review decisions on escalated cases.
 */
public class EscalatedCaseReviewRepositoryImpl implements EscalatedCaseReviewRepository {
    private static final String INSERT_SQL = """
            INSERT INTO EscalatedCaseReviews (CaseID, Instructions, PreviousPriorityState, ResultingPriorityState, ReviewedByUserID)
            VALUES (?, ?, ?, ?, ?)
            """;
    private static final String DELETE_BY_CASE_ID_SQL = """
            DELETE FROM EscalatedCaseReviews
            WHERE CaseID = ?
            """;
    private static final String FIND_LATEST_BY_CASE_ID_SQL = """
            SELECT TOP (1)
                   r.ReviewID,
                   r.CaseID,
                   r.Instructions,
                   r.PreviousPriorityState,
                   r.ResultingPriorityState,
                   r.ReviewedByUserID,
                   r.ReviewedAt,
                   u.Name AS ReviewedByName
            FROM EscalatedCaseReviews r
            JOIN Users u
                ON u.UserID = r.ReviewedByUserID
            WHERE r.CaseID = ?
            ORDER BY r.ReviewedAt DESC, r.ReviewID DESC
            """;

    @Override
    public int save(Connection connection, int caseId, String instructions, PriorityState previousPriorityState,
                    PriorityState resultingPriorityState, int reviewedByUserId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, caseId);
            statement.setString(2, instructions);
            statement.setString(3, previousPriorityState.name());
            statement.setString(4, resultingPriorityState.name());
            statement.setInt(5, reviewedByUserId);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }

        throw new SQLException("Escalated case review insert completed without returning a generated ReviewID.");
    }

    @Override
    public void deleteByCaseId(Connection connection, int caseId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(DELETE_BY_CASE_ID_SQL)) {
            statement.setInt(1, caseId);
            statement.executeUpdate();
        }
    }

    @Override
    public Optional<EscalatedCaseReview> findLatestByCaseId(Connection connection, int caseId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(FIND_LATEST_BY_CASE_ID_SQL)) {
            statement.setInt(1, caseId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                Timestamp reviewedAt = resultSet.getTimestamp("ReviewedAt");
                return Optional.of(new EscalatedCaseReview(
                        resultSet.getInt("ReviewID"),
                        resultSet.getInt("CaseID"),
                        resultSet.getString("Instructions"),
                        PriorityState.valueOf(resultSet.getString("PreviousPriorityState")),
                        PriorityState.valueOf(resultSet.getString("ResultingPriorityState")),
                        resultSet.getInt("ReviewedByUserID"),
                        resultSet.getString("ReviewedByName"),
                        reviewedAt == null ? null : reviewedAt.toLocalDateTime()
                ));
            }
        }
    }
}
