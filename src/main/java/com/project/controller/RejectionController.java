package com.project.controller;

import com.project.model.Case;
import com.project.model.CaseClosureDecisionType;
import com.project.model.User;
import com.project.model.UserRole;
import com.project.repository.CaseClosureDecisionRepository;
import com.project.repository.CaseClosureDecisionRepositoryImpl;
import com.project.repository.CaseRepository;
import com.project.repository.CaseRepositoryImpl;
import com.project.repository.UserRepository;
import com.project.repository.UserRepositoryImpl;
import com.project.service.CaseClosureRejectionResult;
import com.project.service.ChainOfCustodyLog;

import java.sql.Connection;
import java.sql.SQLException;

public class RejectionController extends AbstractCaseWorkflowController {
    private static final int MAX_REASON_LENGTH = 320;

    private final ChainOfCustodyLog chainOfCustodyLog;
    private final CaseClosureDecisionRepository closureDecisionRepository;

    public RejectionController() {
        this(new CaseRepositoryImpl(), new UserRepositoryImpl(), new ChainOfCustodyLog(),
                new CaseClosureDecisionRepositoryImpl());
    }

    public RejectionController(CaseRepository caseRepository, UserRepository userRepository,
                               ChainOfCustodyLog chainOfCustodyLog,
                               CaseClosureDecisionRepository closureDecisionRepository) {
        super(caseRepository, userRepository);
        this.chainOfCustodyLog = chainOfCustodyLog;
        this.closureDecisionRepository = closureDecisionRepository;
    }

    public CaseClosureRejectionResult submitRejection(int caseId, String reason, int userId) {
        String cleanedReason = validateReason(reason);

        try (Connection connection = openConnection()) {
            try {
                User currentUser = requireUserWithRole(
                        connection,
                        userId,
                        UserRole.SUPERVISOR,
                        "Only the Supervisory Authority can reject case closure."
                );
                Case existingCase = requireCase(connection, caseId);
                existingCase.validateSupervisorReviewState();

                var previousState = existingCase.getStatus();
                chainOfCustodyLog.recordClosureRejection(connection, existingCase, currentUser, cleanedReason);
                existingCase.returnToForensicReview();
                caseRepository.updateState(connection, caseId, existingCase.getStatus());
                closureDecisionRepository.save(
                        connection,
                        caseId,
                        CaseClosureDecisionType.REJECTED,
                        cleanedReason,
                        previousState,
                        existingCase.getStatus(),
                        currentUser.getUserId()
                );

                connection.commit();
                return new CaseClosureRejectionResult(existingCase.getStatus(), cleanedReason);
            } catch (Exception e) {
                rollbackQuietly(connection);
                throw wrapException("Failed to reject case closure.", e);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not access the database.", e);
        }
    }

    private String validateReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A rejection reason is required.");
        }

        String cleanedReason = reason.trim();
        if (cleanedReason.length() > MAX_REASON_LENGTH) {
            throw new IllegalArgumentException("Rejection reason must not exceed " + MAX_REASON_LENGTH + " characters.");
        }

        return cleanedReason;
    }
}
