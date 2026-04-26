package com.project.controller;

import com.project.model.Case;
import com.project.model.User;
import com.project.model.UserRole;
import com.project.repository.CaseRepository;
import com.project.repository.CaseRepositoryImpl;
import com.project.repository.UserRepository;
import com.project.repository.UserRepositoryImpl;
import com.project.service.CaseReopenNotificationResult;
import com.project.service.CaseReopenResult;
import com.project.service.ChainOfCustodyLog;
import com.project.service.NotificationService;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Transaction workflow for reopening a frozen case.
 * The supervisor records a reason, restores the case, and notifies the relevant users.
 */
public class ReopenController extends AbstractCaseWorkflowController {
    private static final int MAX_REASON_LENGTH = 320;

    private final ChainOfCustodyLog chainOfCustodyLog;
    private final NotificationService notificationService;

    public ReopenController() {
        this(new CaseRepositoryImpl(), new UserRepositoryImpl(), new ChainOfCustodyLog(), new NotificationService());
    }

    public ReopenController(CaseRepository caseRepository, UserRepository userRepository,
                            ChainOfCustodyLog chainOfCustodyLog, NotificationService notificationService) {
        super(caseRepository, userRepository);
        this.chainOfCustodyLog = chainOfCustodyLog;
        this.notificationService = notificationService;
    }

    public CaseReopenResult submitReopen(int caseId, String reason, int userId) {
        String cleanedReason = validateReason(reason);

        try (Connection connection = openConnection()) {
            try {
                User currentUser = requireUserWithRole(
                        connection,
                        userId,
                        UserRole.SUPERVISOR,
                        "Only the Supervisory Authority can reopen a case."
                );
                Case existingCase = requireCase(connection, caseId);
                existingCase.validateFrozenState();

                User assignedOfficer = existingCase.getAssignedOfficerId() == null
                        ? null
                        : requireUser(connection, existingCase.getAssignedOfficerId());
                User forensicAnalyst = chainOfCustodyLog.findLatestFreezingAnalystId(connection, caseId)
                        .flatMap(analystId -> {
                            try {
                                return userRepository.findById(connection, analystId);
                            } catch (SQLException e) {
                                throw new IllegalStateException("Could not resolve the analyst to notify.", e);
                            }
                        })
                        .orElse(null);

                // Reopening reverses the freeze and tells both the officer and the last analyst what changed.
                chainOfCustodyLog.recordReopen(connection, existingCase, currentUser, cleanedReason);
                existingCase.reopenToSupervisorReview();
                caseRepository.updateState(connection, caseId, existingCase.getStatus());

                CaseReopenNotificationResult notificationResult =
                        notificationService.notifyInvestigatingOfficerAndAnalyst(
                                connection,
                                caseId,
                                assignedOfficer,
                                forensicAnalyst,
                                currentUser
                        );

                connection.commit();
                return new CaseReopenResult(
                        existingCase.getStatus(),
                        cleanedReason,
                        notificationResult.investigatingOfficerNotified(),
                        notificationResult.forensicAnalystNotified(),
                        forensicAnalyst == null ? null : forensicAnalyst.getName()
                );
            } catch (Exception e) {
                rollbackQuietly(connection);
                throw wrapException("Failed to reopen the case.", e);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not access the database.", e);
        }
    }

    private String validateReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reopen reason is required.");
        }

        String cleanedReason = reason.trim();
        if (cleanedReason.length() > MAX_REASON_LENGTH) {
            throw new IllegalArgumentException("Reopen reason must not exceed " + MAX_REASON_LENGTH + " characters.");
        }

        return cleanedReason;
    }
}
