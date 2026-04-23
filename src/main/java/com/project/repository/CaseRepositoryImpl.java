package com.project.repository;

import com.project.model.Case;
import com.project.model.CaseState;
import com.project.model.PriorityState;
import com.project.model.SeverityLevel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Optional;

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
    private static final String UPDATE_STATE_SQL = """
            UPDATE Cases
            SET Status = ?
            WHERE CaseID = ?
            """;
    private static final String FIND_BY_ID_SQL = """
            SELECT CaseID, Title, Description, RelatedInfo, Severity, SlaHours, PriorityState, Status,
                   AssignedOfficerID, AssignedOfficerName, CreatedAt
            FROM Cases
            WHERE CaseID = ?
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
    public void updateState(Connection connection, int caseId, CaseState caseState) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_STATE_SQL)) {
            statement.setString(1, caseState.name());
            statement.setInt(2, caseId);
            statement.executeUpdate();
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
                        createdAt == null ? null : createdAt.toLocalDateTime()
                );
                return Optional.of(caseRecord);
            }
        }
    }
}
