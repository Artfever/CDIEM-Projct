package com.project.controller;

import com.project.model.Case;
import com.project.model.CaseState;
import com.project.model.Evidence;
import com.project.model.EvidenceStatus;
import com.project.model.User;
import com.project.model.UserRole;
import com.project.repository.CaseRepository;
import com.project.repository.CaseRepositoryImpl;
import com.project.repository.EvidenceRepository;
import com.project.repository.EvidenceRepositoryImpl;
import com.project.repository.UserRepository;
import com.project.repository.UserRepositoryImpl;
import com.project.service.CaseSubmissionResult;
import com.project.service.ChainOfCustodyLog;
import com.project.service.NotificationService;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class SubmissionController extends AbstractCaseWorkflowController {
    private final EvidenceRepository evidenceRepository;
    private final ChainOfCustodyLog chainOfCustodyLog;
    private final NotificationService notificationService;

    public SubmissionController() {
        this(new CaseRepositoryImpl(), new EvidenceRepositoryImpl(), new UserRepositoryImpl(),
                new ChainOfCustodyLog(), new NotificationService());
    }

    public SubmissionController(CaseRepository caseRepository, EvidenceRepository evidenceRepository,
                                UserRepository userRepository, ChainOfCustodyLog chainOfCustodyLog,
                                NotificationService notificationService) {
        super(caseRepository, userRepository);
        this.evidenceRepository = evidenceRepository;
        this.chainOfCustodyLog = chainOfCustodyLog;
        this.notificationService = notificationService;
    }

    public CaseSubmissionResult submitForSupervisorReview(int caseId, int userId) {
        try (Connection connection = openConnection()) {
            try {
                User currentUser = requireUserWithRole(
                        connection,
                        userId,
                        UserRole.OFFICER,
                        "Only an Investigating Officer can submit a case for supervisor review."
                );
                Case existingCase = requireCase(connection, caseId);
                validateAssignedOfficerOwnership(existingCase, currentUser);
                existingCase.validateVerifiedState();

                Evidence evidence = requireEvidence(connection, caseId);
                validateVerifiedEvidence(evidence);

                chainOfCustodyLog.recordSubmission(connection, existingCase, currentUser);
                existingCase.moveToState(CaseState.SUPERVISOR_REVIEW);
                caseRepository.updateState(connection, caseId, existingCase.getStatus());

                List<User> supervisors = userRepository.findByRole(connection, UserRole.SUPERVISOR);
                int notifiedSupervisorCount = notificationService.notifySupervisorsForReviewSubmission(
                        connection,
                        caseId,
                        supervisors,
                        currentUser
                );

                connection.commit();
                return new CaseSubmissionResult(existingCase.getStatus(), notifiedSupervisorCount);
            } catch (Exception e) {
                rollbackQuietly(connection);
                throw wrapException("Failed to submit the case for supervisor review.", e);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not access the database.", e);
        }
    }

    private Evidence requireEvidence(Connection connection, int caseId) throws SQLException {
        return evidenceRepository.findLatestByCaseId(connection, caseId)
                .orElseThrow(() -> new IllegalArgumentException("No evidence has been uploaded for case " + caseId + "."));
    }

    private void validateAssignedOfficerOwnership(Case existingCase, User currentUser) {
        if (existingCase.getAssignedOfficerId() == null || existingCase.getAssignedOfficerId() != currentUser.getUserId()) {
            throw new IllegalStateException("Only the assigned Investigating Officer can submit this case for supervisor review.");
        }
    }

    private void validateVerifiedEvidence(Evidence evidence) {
        if (!evidence.hasVerificationSnapshot()) {
            throw new IllegalStateException("Evidence must be verified before the case can be submitted for supervisor review.");
        }

        if (evidence.getStatus() == EvidenceStatus.TAMPERED) {
            throw new IllegalStateException("Cases with tampered evidence cannot be submitted for supervisor review.");
        }

        if (evidence.getStatus() != EvidenceStatus.VERIFIED) {
            throw new IllegalStateException("Only cases with evidence marked as VERIFIED can be submitted for supervisor review.");
        }
    }
}
