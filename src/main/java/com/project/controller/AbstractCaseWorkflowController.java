package com.project.controller;

import com.project.model.Case;
import com.project.model.User;
import com.project.model.UserRole;
import com.project.repository.CaseRepository;
import com.project.repository.CaseRepositoryImpl;
import com.project.repository.UserRepository;
import com.project.repository.UserRepositoryImpl;
import com.project.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;

abstract class AbstractCaseWorkflowController {
    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MAX_DESCRIPTION_LENGTH = 1000;
    private static final int MAX_RELATED_INFO_LENGTH = 1000;

    protected final CaseRepository caseRepository;
    protected final UserRepository userRepository;

    protected AbstractCaseWorkflowController() {
        this(new CaseRepositoryImpl(), new UserRepositoryImpl());
    }

    protected AbstractCaseWorkflowController(CaseRepository caseRepository, UserRepository userRepository) {
        this.caseRepository = caseRepository;
        this.userRepository = userRepository;
    }

    protected Connection openConnection() throws SQLException {
        Connection connection = DBConnection.getConnection();
        connection.setAutoCommit(false);
        return connection;
    }

    protected void validateCaseRegistration(Case caseRecord) {
        if (caseRecord.getTitle() == null || caseRecord.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title is required.");
        }

        if (caseRecord.getTitle().length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("Title must not exceed " + MAX_TITLE_LENGTH + " characters.");
        }

        if (caseRecord.getDescription() == null || caseRecord.getDescription().isBlank()) {
            throw new IllegalArgumentException("Description is required.");
        }

        if (caseRecord.getDescription().length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Description must not exceed " + MAX_DESCRIPTION_LENGTH + " characters.");
        }

        if (caseRecord.getRelatedInfo() != null && caseRecord.getRelatedInfo().length() > MAX_RELATED_INFO_LENGTH) {
            throw new IllegalArgumentException("Related information must not exceed " + MAX_RELATED_INFO_LENGTH + " characters.");
        }

        if (caseRecord.getSeverity() == null) {
            throw new IllegalArgumentException("Severity is required.");
        }
    }

    protected User requireUser(Connection connection, int userId) throws SQLException {
        return userRepository.findById(connection, userId)
                .orElseThrow(() -> new IllegalArgumentException("User " + userId + " does not exist."));
    }

    protected User requireUserWithRole(Connection connection, int userId, UserRole expectedRole, String message) throws SQLException {
        User user = requireUser(connection, userId);
        if (user.getRole() != expectedRole) {
            throw new IllegalStateException(message);
        }

        return user;
    }

    protected User validateAssignedOfficer(Connection connection, Integer officerId) throws SQLException {
        if (officerId == null) {
            throw new IllegalArgumentException("Assigned Investigating Officer ID is required.");
        }

        User officer = requireUser(connection, officerId);
        if (officer.getRole() != UserRole.OFFICER) {
            throw new IllegalStateException("Assigned user must be an Investigating Officer.");
        }

        return officer;
    }

    protected Case requireCase(Connection connection, int caseId) throws SQLException {
        return caseRepository.findById(connection, caseId)
                .orElseThrow(() -> new IllegalArgumentException("Case " + caseId + " does not exist."));
    }

    protected void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
        }
    }

    protected RuntimeException wrapException(String message, Exception e) {
        if (e instanceof RuntimeException runtimeException) {
            return new IllegalStateException(message, runtimeException);
        }

        return new IllegalStateException(message, e);
    }
}
