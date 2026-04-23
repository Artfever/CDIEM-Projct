package com.project.service;

import com.project.model.Case;
import com.project.model.CaseState;
import com.project.model.PriorityState;
import com.project.model.SeverityLevel;
import com.project.model.User;
import com.project.model.UserRole;
import com.project.repository.AuditRepository;
import com.project.repository.AuditRepositoryImpl;
import com.project.repository.CaseRepository;
import com.project.repository.CaseRepositoryImpl;
import com.project.repository.UserRepository;
import com.project.repository.UserRepositoryImpl;
import com.project.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class CaseService {
    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MAX_DESCRIPTION_LENGTH = 1000;
    private static final int MAX_RELATED_INFO_LENGTH = 1000;
    private static final int LOW_SLA_HOURS = 120;
    private static final int MEDIUM_SLA_HOURS = 72;
    private static final int HIGH_SLA_HOURS = 24;
    private static final int CRITICAL_SLA_HOURS = 8;

    private final CaseRepository caseRepository;
    private final AuditRepository auditRepository;
    private final UserRepository userRepository;

    public CaseService() {
        this(new CaseRepositoryImpl(), new AuditRepositoryImpl(), new UserRepositoryImpl());
    }

    public CaseService(CaseRepository caseRepository, AuditRepository auditRepository, UserRepository userRepository) {
        this.caseRepository = caseRepository;
        this.auditRepository = auditRepository;
        this.userRepository = userRepository;
    }

    public int registerCase(Case c, int userId) {
        validateCaseRegistration(c);

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                User currentUser = requireUserForCreate(connection, userId);
                c.setCreatedByUserId(currentUser.getUserId());
                c.setSlaHours(determineSlaHours(c.getSeverity()));
                c.setPriorityState(determinePriorityState(c.getSeverity()));
                c.setStatus(CaseState.CASE_CREATED);
                c.setAssignedOfficerId(null);
                int caseId = caseRepository.save(connection, c);
                auditRepository.logAction(connection, caseId,
                        "CASE_REGISTERED with initial state CASE_CREATED, severity " + c.getSeverity().name()
                                + ", SLA " + c.getSlaHours() + "h, priority " + c.getPriorityState().name()
                                + " by " + currentUser.getRole().getDisplayName(),
                        currentUser.getUserId());

                connection.commit();
                return caseId;
            } catch (Exception e) {
                rollbackQuietly(connection);
                throw wrapException("Failed to create case.", e);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not access the database.", e);
        }
    }

    public CaseSeverityUpdateResult updateSeverityLevel(int caseId, SeverityLevel severity, int userId) {
        if (severity == null) {
            throw new IllegalArgumentException("Severity is required.");
        }

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                User currentUser = requireUserForSeverityChange(connection, userId);
                Case existingCase = requireCase(connection, caseId);
                ensureCaseAllowsModuleOneChanges(existingCase);

                if (existingCase.getSeverity() == severity) {
                    throw new IllegalArgumentException("Case is already marked with severity " + severity.name() + ".");
                }

                int recalculatedSlaHours = determineSlaHours(severity);
                PriorityState recalculatedPriorityState = determinePriorityState(severity);

                caseRepository.updateSeverityProfile(connection, caseId, severity, recalculatedSlaHours, recalculatedPriorityState);
                auditRepository.logAction(connection, caseId,
                        "CASE_SEVERITY_CHANGED from " + existingCase.getSeverity().name() + " to " + severity.name()
                                + ", SLA " + existingCase.getSlaHours() + "h -> " + recalculatedSlaHours + "h"
                                + ", priority " + existingCase.getPriorityState().name() + " -> "
                                + recalculatedPriorityState.name() + " by " + currentUser.getRole().getDisplayName(),
                        currentUser.getUserId());

                connection.commit();
                return new CaseSeverityUpdateResult(severity, recalculatedSlaHours, recalculatedPriorityState);
            } catch (Exception e) {
                rollbackQuietly(connection);
                throw wrapException("Failed to update case severity.", e);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not access the database.", e);
        }
    }

    public CaseReassignmentResult reassignOfficer(int caseId, Integer officerId, int userId) {
        if (officerId == null) {
            throw new IllegalArgumentException("Assigned Investigating Officer ID is required.");
        }

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                User currentUser = requireUserForReassignment(connection, userId);
                Case existingCase = requireCase(connection, caseId);
                ensureCaseAllowsModuleOneChanges(existingCase);
                User newOfficer = validateAssignedOfficer(connection, officerId);
                User previousOfficer = existingCase.getAssignedOfficerId() == null
                        ? null
                        : requireUser(connection, existingCase.getAssignedOfficerId());

                if (officerId.equals(existingCase.getAssignedOfficerId())) {
                    throw new IllegalArgumentException("This case is already assigned to Investigating Officer " + officerId + ".");
                }

                Integer previousOfficerId = existingCase.getAssignedOfficerId();
                caseRepository.updateAssignedOfficer(connection, caseId, officerId, CaseState.CASE_REASSIGNED);
                auditRepository.logAction(connection, caseId,
                        "CASE_REASSIGNED from Investigating Officer "
                                + buildOfficerIdentity(previousOfficer)
                                + " to " + buildOfficerIdentity(newOfficer)
                                + " by " + currentUser.getRole().getDisplayName(),
                        currentUser.getUserId());

                connection.commit();
                return new CaseReassignmentResult(
                        previousOfficer == null ? "Unassigned" : previousOfficer.getName(),
                        newOfficer.getName(),
                        previousOfficerId != null,
                        true
                );
            } catch (Exception e) {
                rollbackQuietly(connection);
                throw wrapException("Failed to reassign investigating officer.", e);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not access the database.", e);
        }
    }

    public List<User> getCaseActors() {
        try (Connection connection = DBConnection.getConnection()) {
            return userRepository.findCaseActors(connection);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load case actors from the database.", e);
        }
    }

    public List<User> getInvestigatingOfficers() {
        try (Connection connection = DBConnection.getConnection()) {
            return userRepository.findByRole(connection, UserRole.OFFICER);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load investigating officers from the database.", e);
        }
    }

    private void validateCaseRegistration(Case c) {
        if (c.getTitle() == null || c.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title is required.");
        }

        if (c.getTitle().length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("Title must not exceed " + MAX_TITLE_LENGTH + " characters.");
        }

        if (c.getDescription() == null || c.getDescription().isBlank()) {
            throw new IllegalArgumentException("Description is required.");
        }

        if (c.getDescription().length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Description must not exceed " + MAX_DESCRIPTION_LENGTH + " characters.");
        }

        if (c.getRelatedInfo() != null && c.getRelatedInfo().length() > MAX_RELATED_INFO_LENGTH) {
            throw new IllegalArgumentException("Related information must not exceed " + MAX_RELATED_INFO_LENGTH + " characters.");
        }

        if (c.getSeverity() == null) {
            throw new IllegalArgumentException("Severity is required.");
        }
    }

    private User requireUserForCreate(Connection connection, int userId) throws SQLException {
        User user = requireUser(connection, userId);
        ensureRole(user, UserRole.OFFICER, "Only an Investigating Officer can create a case.");
        return user;
    }

    private User requireUserForSeverityChange(Connection connection, int userId) throws SQLException {
        User user = requireUser(connection, userId);
        ensureRole(user, UserRole.SUPERVISOR, "Only the Supervisory Authority can update case severity.");
        return user;
    }

    private User requireUserForReassignment(Connection connection, int userId) throws SQLException {
        User user = requireUser(connection, userId);
        ensureRole(user, UserRole.SUPERVISOR, "Only the Supervisory Authority can reassign an Investigating Officer.");
        return user;
    }

    private User requireUser(Connection connection, int userId) throws SQLException {
        return userRepository.findById(connection, userId)
                .orElseThrow(() -> new IllegalArgumentException("User " + userId + " does not exist."));
    }

    private void ensureRole(User user, UserRole expectedRole, String message) {
        if (user.getRole() != expectedRole) {
            throw new IllegalStateException(message);
        }
    }

    private User validateAssignedOfficer(Connection connection, Integer officerId) throws SQLException {
        if (officerId == null) {
            return null;
        }

        User officer = userRepository.findById(connection, officerId)
                .orElseThrow(() -> new IllegalArgumentException("Assigned Investigating Officer " + officerId + " does not exist."));

        if (officer.getRole() != UserRole.OFFICER) {
            throw new IllegalStateException("Assigned user must be an Investigating Officer.");
        }

        return officer;
    }

    private Case requireCase(Connection connection, int caseId) throws SQLException {
        Optional<Case> caseRecord = caseRepository.findById(connection, caseId);
        return caseRecord.orElseThrow(() -> new IllegalArgumentException("Case " + caseId + " does not exist."));
    }

    private void ensureCaseAllowsModuleOneChanges(Case existingCase) {
        if (existingCase.getStatus() == CaseState.CLOSED) {
            throw new IllegalStateException("Closed cases cannot be modified.");
        }

        if (existingCase.getStatus() == CaseState.FROZEN) {
            throw new IllegalStateException("Frozen cases cannot be changed from the Manage Case module.");
        }
    }

    private int determineSlaHours(SeverityLevel severity) {
        return switch (severity) {
            case LOW -> LOW_SLA_HOURS;
            case MEDIUM -> MEDIUM_SLA_HOURS;
            case HIGH -> HIGH_SLA_HOURS;
            case CRITICAL -> CRITICAL_SLA_HOURS;
        };
    }

    private PriorityState determinePriorityState(SeverityLevel severity) {
        return switch (severity) {
            case LOW, MEDIUM -> PriorityState.STANDARD;
            case HIGH -> PriorityState.PRIORITY;
            case CRITICAL -> PriorityState.ESCALATED;
        };
    }

    private String buildOfficerIdentity(User officer) {
        if (officer == null) {
            return "UNASSIGNED";
        }

        return officer.getName() + " (ID " + officer.getUserId() + ")";
    }

    private void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
        }
    }

    private RuntimeException wrapException(String message, Exception e) {
        if (e instanceof RuntimeException runtimeException) {
            return new IllegalStateException(message, runtimeException);
        }

        return new IllegalStateException(message, e);
    }
}
