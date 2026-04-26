package com.project.repository;

import com.project.model.Evidence;
import com.project.model.EvidenceStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of EvidenceRepository using SQL Server.
 * Handles evidence storage and integrity verification tracking.
 */
public class EvidenceRepositoryImpl implements EvidenceRepository {
    private static final String INSERT_SQL = """
            INSERT INTO Evidence (
                CaseID,
                OriginalFileName,
                StoredFilePath,
                OriginalSHA256,
                RecalculatedSHA256,
                IntegrityStatus,
                UploadedByUserID,
                UploadedAt,
                LastVerifiedByUserID,
                LastVerifiedAt
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String FIND_LATEST_BY_CASE_SQL = """
            SELECT TOP (1)
                   EvidenceID,
                   CaseID,
                   OriginalFileName,
                   StoredFilePath,
                   OriginalSHA256,
                   RecalculatedSHA256,
                   IntegrityStatus,
                   UploadedByUserID,
                   UploadedAt,
                   LastVerifiedByUserID,
                   LastVerifiedAt
            FROM Evidence
            WHERE CaseID = ?
            ORDER BY UploadedAt DESC, EvidenceID DESC
            """;
    private static final String FIND_BY_CASE_SQL = """
            SELECT EvidenceID,
                   CaseID,
                   OriginalFileName,
                   StoredFilePath,
                   OriginalSHA256,
                   RecalculatedSHA256,
                   IntegrityStatus,
                   UploadedByUserID,
                   UploadedAt,
                   LastVerifiedByUserID,
                   LastVerifiedAt
            FROM Evidence
            WHERE CaseID = ?
            ORDER BY UploadedAt DESC, EvidenceID DESC
            """;
    private static final String UPDATE_VERIFICATION_SQL = """
            UPDATE Evidence
            SET RecalculatedSHA256 = ?, LastVerifiedByUserID = ?, LastVerifiedAt = ?
            WHERE EvidenceID = ?
            """;
    private static final String DELETE_BY_CASE_SQL = """
            DELETE FROM Evidence
            WHERE CaseID = ?
            """;
    private static final String UPDATE_STATUS_SQL = """
            UPDATE Evidence
            SET IntegrityStatus = ?, RecalculatedSHA256 = ?, LastVerifiedByUserID = ?, LastVerifiedAt = ?
            WHERE EvidenceID = ?
            """;

    @Override
    public int save(Connection connection, Evidence evidence) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, evidence.getCaseId());
            statement.setString(2, evidence.getOriginalFileName());
            statement.setString(3, evidence.getStoredFilePath());
            statement.setString(4, evidence.getOriginalSha256());
            statement.setString(5, evidence.getRecalculatedSha256());
            statement.setString(6, evidence.getStatus().name());
            statement.setInt(7, evidence.getUploadedByUserId());
            statement.setTimestamp(8, asTimestamp(evidence.getUploadedAt()));
            setNullableInteger(statement, 9, evidence.getLastVerifiedByUserId());
            statement.setTimestamp(10, asTimestamp(evidence.getLastVerifiedAt()));
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int evidenceId = generatedKeys.getInt(1);
                    evidence.setEvidenceId(evidenceId);
                    return evidenceId;
                }
            }
        }

        throw new SQLException("Evidence insert completed without returning a generated EvidenceID.");
    }

    @Override
    public List<Evidence> findByCaseId(Connection connection, int caseId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_CASE_SQL)) {
            statement.setInt(1, caseId);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<Evidence> evidenceRecords = new ArrayList<>();
                while (resultSet.next()) {
                    evidenceRecords.add(mapEvidence(resultSet));
                }
                return evidenceRecords;
            }
        }
    }

    @Override
    public void deleteByCaseId(Connection connection, int caseId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(DELETE_BY_CASE_SQL)) {
            statement.setInt(1, caseId);
            statement.executeUpdate();
        }
    }

    @Override
    public Optional<Evidence> findLatestByCaseId(Connection connection, int caseId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(FIND_LATEST_BY_CASE_SQL)) {
            statement.setInt(1, caseId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                return Optional.of(mapEvidence(resultSet));
            }
        }
    }

    @Override
    public void updateVerificationSnapshot(Connection connection, int evidenceId, String recalculatedSha256,
                                           Integer verifiedByUserId, LocalDateTime verifiedAt) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_VERIFICATION_SQL)) {
            statement.setString(1, recalculatedSha256);
            setNullableInteger(statement, 2, verifiedByUserId);
            statement.setTimestamp(3, asTimestamp(verifiedAt));
            statement.setInt(4, evidenceId);
            statement.executeUpdate();
        }
    }

    @Override
    public void updateStatus(Connection connection, int evidenceId, EvidenceStatus status, String recalculatedSha256,
                             Integer verifiedByUserId, LocalDateTime verifiedAt) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_STATUS_SQL)) {
            statement.setString(1, status.name());
            statement.setString(2, recalculatedSha256);
            setNullableInteger(statement, 3, verifiedByUserId);
            statement.setTimestamp(4, asTimestamp(verifiedAt));
            statement.setInt(5, evidenceId);
            statement.executeUpdate();
        }
    }

    private void setNullableInteger(PreparedStatement statement, int parameterIndex, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(parameterIndex, java.sql.Types.INTEGER);
            return;
        }

        statement.setInt(parameterIndex, value);
    }

    private Timestamp asTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private Evidence mapEvidence(ResultSet resultSet) throws SQLException {
        return new Evidence(
                resultSet.getInt("EvidenceID"),
                resultSet.getInt("CaseID"),
                resultSet.getString("OriginalFileName"),
                resultSet.getString("StoredFilePath"),
                resultSet.getString("OriginalSHA256"),
                resultSet.getString("RecalculatedSHA256"),
                EvidenceStatus.valueOf(resultSet.getString("IntegrityStatus")),
                resultSet.getInt("UploadedByUserID"),
                toLocalDateTime(resultSet.getTimestamp("UploadedAt")),
                (Integer) resultSet.getObject("LastVerifiedByUserID"),
                toLocalDateTime(resultSet.getTimestamp("LastVerifiedAt"))
        );
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
