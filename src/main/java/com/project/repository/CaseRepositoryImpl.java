package com.project.repository;

import com.project.model.Case;
import com.project.model.CaseState;
import com.project.model.EvidenceStatus;
import com.project.model.PriorityState;
import com.project.model.SeverityLevel;
import com.project.model.SummaryReportCaseFilter;
import com.project.model.SummaryReportCaseRecord;
import com.project.model.SummaryReportRequest;

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
 * Implementation of CaseRepository using SQL Server.
 * Handles case persistence, state transitions, and summary report queries.
 */
public class CaseRepositoryImpl implements CaseRepository {
    private static final String INSERT_SQL = """
            INSERT INTO Cases (Title, Description, RelatedInfo, Severity, SlaHours, PriorityState, Status, AssignedOfficerID, AssignedOfficerName)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String UPDATE_SEVERITY_SQL = """
            UPDATE Cases
            SET Severity = ?, SlaHours = ?, PriorityState = ?
            WHERE CaseID = ?
            """;
    private static final String UPDATE_ASSIGNED_OFFICER_SQL = """
            UPDATE Cases
            SET AssignedOfficerID = ?, AssignedOfficerName = ?, Status = ?
            WHERE CaseID = ?
            """;
    private static final String UPDATE_PRIORITY_STATE_SQL = """
            UPDATE Cases
            SET PriorityState = ?
            WHERE CaseID = ?
            """;
    private static final String UPDATE_STATE_SQL = """
            UPDATE Cases
            SET Status = ?, ClosedAt = ?
            WHERE CaseID = ?
            """;
    private static final String DELETE_SQL = """
            DELETE FROM Cases
            WHERE CaseID = ?
            """;
    private static final String FIND_BY_ID_SQL = """
            SELECT CaseID, Title, Description, RelatedInfo, Severity, SlaHours, PriorityState, Status,
                   AssignedOfficerID, AssignedOfficerName, CreatedAt, ClosedAt
            FROM Cases
            WHERE CaseID = ?
            """;
    private static final String SUMMARY_REPORT_SQL_PREFIX = """
            SELECT c.CaseID,
                   c.Title,
                   c.Status,
                   c.Severity,
                   c.SlaHours,
                   c.PriorityState,
                   c.AssignedOfficerName,
                   c.CreatedAt,
                   c.ClosedAt,
                   latestEvidence.IntegrityStatus AS LatestEvidenceStatus
            FROM Cases c
            OUTER APPLY (
                SELECT TOP (1) e.IntegrityStatus
                FROM Evidence e
                WHERE e.CaseID = c.CaseID
                ORDER BY e.UploadedAt DESC, e.EvidenceID DESC
            ) latestEvidence
            WHERE 1 = 1
            """;
    private static final String SUMMARY_REPORT_SQL_ORDER = """
            ORDER BY c.CreatedAt DESC, c.CaseID DESC
            """;

    @Override
    public int save(Connection connection, Case c) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, c.getTitle());
            statement.setString(2, c.getDescription());
            statement.setString(3, c.getRelatedInfo());
            statement.setString(4, c.getSeverity().name());
            statement.setInt(5, c.getSlaHours());
            statement.setString(6, c.getPriorityState().name());
            statement.setString(7, c.getStatus().name());

            if (c.getAssignedOfficerId() == null) {
                statement.setNull(8, java.sql.Types.INTEGER);
            } else {
                statement.setInt(8, c.getAssignedOfficerId());
            }

            statement.setString(9, c.getAssignedOfficerName());

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int caseId = generatedKeys.getInt(1);
                    c.setCaseId(caseId);
                    return caseId;
                }
            }
        }

        throw new SQLException("Case insert completed without returning a generated CaseID.");
    }

    @Override
    public void updateSeverityProfile(Connection connection, int caseId, SeverityLevel severity, int slaHours,
                                      PriorityState priorityState) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_SEVERITY_SQL)) {
            statement.setString(1, severity.name());
            statement.setInt(2, slaHours);
            statement.setString(3, priorityState.name());
            statement.setInt(4, caseId);
            statement.executeUpdate();
        }
    }

    @Override
    public void updateAssignedOfficer(Connection connection, int caseId, Integer officerId, String officerName,
                                      CaseState caseState) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_ASSIGNED_OFFICER_SQL)) {
            if (officerId == null) {
                statement.setNull(1, java.sql.Types.INTEGER);
            } else {
                statement.setInt(1, officerId);
            }

            statement.setString(2, officerName);
            statement.setString(3, caseState.name());
            statement.setInt(4, caseId);
            statement.executeUpdate();
        }
    }

    @Override
    public void updatePriorityState(Connection connection, int caseId, PriorityState priorityState) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_PRIORITY_STATE_SQL)) {
            statement.setString(1, priorityState.name());
            statement.setInt(2, caseId);
            statement.executeUpdate();
        }
    }

    @Override
    public void updateState(Connection connection, int caseId, CaseState caseState) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_STATE_SQL)) {
            statement.setString(1, caseState.name());
            if (caseState == CaseState.CLOSED) {
                statement.setTimestamp(2, Timestamp.valueOf(java.time.LocalDateTime.now()));
            } else {
                statement.setTimestamp(2, null);
            }
            statement.setInt(3, caseId);
            statement.executeUpdate();
        }
    }

    @Override
    public void deleteById(Connection connection, int caseId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setInt(1, caseId);
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Case delete completed without affecting a record.");
            }
        }
    }

    @Override
    public Optional<Case> findById(Connection connection, int caseId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
            statement.setInt(1, caseId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                Timestamp createdAt = resultSet.getTimestamp("CreatedAt");
                Timestamp closedAt = resultSet.getTimestamp("ClosedAt");
                Integer assignedOfficerId = (Integer) resultSet.getObject("AssignedOfficerID");
                String assignedOfficerName = resultSet.getString("AssignedOfficerName");

                Case caseRecord = new Case(
                        resultSet.getInt("CaseID"),
                        resultSet.getString("Title"),
                        resultSet.getString("Description"),
                        resultSet.getString("RelatedInfo"),
                        SeverityLevel.valueOf(resultSet.getString("Severity")),
                        resultSet.getInt("SlaHours"),
                        PriorityState.valueOf(resultSet.getString("PriorityState")),
                        CaseState.valueOf(resultSet.getString("Status")),
                        assignedOfficerId,
                        assignedOfficerName,
                        createdAt == null ? null : createdAt.toLocalDateTime(),
                        closedAt == null ? null : closedAt.toLocalDateTime()
                );
                return Optional.of(caseRecord);
            }
        }
    }

    @Override
    public List<SummaryReportCaseRecord> findCasesForSummaryReport(Connection connection,
                                                                   SummaryReportRequest request) throws SQLException {
        StringBuilder sql = new StringBuilder(SUMMARY_REPORT_SQL_PREFIX);
        List<Object> parameters = new ArrayList<>();

        if (request.fromDateTime() != null) {
            sql.append(" AND c.CreatedAt >= ?");
            parameters.add(Timestamp.valueOf(request.fromDateTime()));
        }
        if (request.toDateTimeExclusive() != null) {
            sql.append(" AND c.CreatedAt < ?");
            parameters.add(Timestamp.valueOf(request.toDateTimeExclusive()));
        }
        if (request.priorityState() != null) {
            sql.append(" AND c.PriorityState = ?");
            parameters.add(request.priorityState().name());
        }

        appendCaseFilter(sql, parameters, request.caseFilter());
        sql.append('\n').append(SUMMARY_REPORT_SQL_ORDER);

        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) {
                Object parameter = parameters.get(i);
                if (parameter instanceof Timestamp timestamp) {
                    statement.setTimestamp(i + 1, timestamp);
                } else {
                    statement.setString(i + 1, String.valueOf(parameter));
                }
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                List<SummaryReportCaseRecord> matchingCases = new ArrayList<>();
                while (resultSet.next()) {
                    String evidenceStatusValue = resultSet.getString("LatestEvidenceStatus");
                    matchingCases.add(new SummaryReportCaseRecord(
                            resultSet.getInt("CaseID"),
                            resultSet.getString("Title"),
                            CaseState.valueOf(resultSet.getString("Status")),
                            SeverityLevel.valueOf(resultSet.getString("Severity")),
                            PriorityState.valueOf(resultSet.getString("PriorityState")),
                            resultSet.getString("AssignedOfficerName"),
                            toLocalDateTime(resultSet.getTimestamp("CreatedAt")),
                            toLocalDateTime(resultSet.getTimestamp("ClosedAt")),
                            resultSet.getInt("SlaHours"),
                            evidenceStatusValue == null ? null : EvidenceStatus.valueOf(evidenceStatusValue)
                    ));
                }
                return matchingCases;
            }
        }
    }

    private void appendCaseFilter(StringBuilder sql, List<Object> parameters, SummaryReportCaseFilter caseFilter) {
        if (caseFilter == null || caseFilter == SummaryReportCaseFilter.ALL) {
            return;
        }

        switch (caseFilter) {
            case ACTIVE -> {
                sql.append(" AND c.Status NOT IN (?, ?)");
                parameters.add(CaseState.FROZEN.name());
                parameters.add(CaseState.CLOSED.name());
            }
            case VERIFIED -> {
                sql.append(" AND latestEvidence.IntegrityStatus = ?");
                parameters.add(EvidenceStatus.VERIFIED.name());
            }
            case FROZEN -> {
                sql.append(" AND c.Status = ?");
                parameters.add(CaseState.FROZEN.name());
            }
            case CLOSED -> {
                sql.append(" AND c.Status = ?");
                parameters.add(CaseState.CLOSED.name());
            }
            default -> {
            }
        }
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
