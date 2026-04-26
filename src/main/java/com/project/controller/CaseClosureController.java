package com.project.controller;

import com.project.model.Case;
import com.project.model.CaseClosureDecisionType;
import com.project.model.Evidence;
import com.project.model.EvidenceStatus;
import com.project.model.User;
import com.project.model.UserRole;
import com.project.repository.AuditRepositoryImpl;
import com.project.repository.CaseClosureDecisionRepository;
import com.project.repository.CaseClosureDecisionRepositoryImpl;
import com.project.repository.CaseRepository;
import com.project.repository.CaseRepositoryImpl;
import com.project.repository.EvidenceRepository;
import com.project.repository.EvidenceRepositoryImpl;
import com.project.repository.NotificationRepositoryImpl;
import com.project.repository.UserRepository;
import com.project.repository.UserRepositoryImpl;
import com.project.service.AuditLogService;
import com.project.service.CaseClosureApprovalResult;
import com.project.service.NotificationService;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Transaction workflow for approving case closure.
 * It marks the case closed, stores the decision, and informs the assigned officer.
 */
public class CaseClosureController extends AbstractCaseWorkflowController {
    private final EvidenceRepository evidenceRepository;
    private final CaseClosureDecisionRepository closureDecisionRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public CaseClosureController() {
        this(new CaseRepositoryImpl(), new EvidenceRepositoryImpl(), new CaseClosureDecisionRepositoryImpl(),
                new UserRepositoryImpl(), new AuditLogService(new AuditRepositoryImpl()),
                new NotificationService(new NotificationRepositoryImpl()));
    }

    public CaseClosureController(CaseRepository caseRepository, EvidenceRepository evidenceRepository,
                                 CaseClosureDecisionRepository closureDecisionRepository,
                                 UserRepository userRepository, AuditLogService auditLogService,
                                 NotificationService notificationService) {
        super(caseRepository, userRepository);
        this.evidenceRepository = evidenceRepository;
        this.closureDecisionRepository = closureDecisionRepository;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    public CaseClosureApprovalResult approveClosure(int caseId, int userId) {
        try (Connection connection = openConnection()) {
            try {
                User currentUser = requireUserWithRole(
                        connection,
                        userId,
                        UserRole.SUPERVISOR,
                        "Only the Supervisory Authority can approve case closure."
                );
                Case existingCase = requireCase(connection, caseId);
                existingCase.validateSupervisorReviewState();
                validateVerifiedEvidence(connection, caseId);

                User assignedOfficer = existingCase.getAssignedOfficerId() == null
                        ? null
                        : requireUser(connection, existingCase.getAssignedOfficerId());

                // Approval updates the case itself and also records the supervisor's decision trail.
                var previousState = existingCase.getStatus();
                existingCase.closeCase();
                caseRepository.updateState(connection, caseId, existingCase.getStatus());
                closureDecisionRepository.save(
                        connection,
                        caseId,
                        CaseClosureDecisionType.APPROVED,
                        null,
                        previousState,
                        existingCase.getStatus(),
                        currentUser.getUserId()
                );
                auditLogService.recordAudit(
                        connection,
                        caseId,
                        "CASE_CLOSURE_APPROVED state CLOSED by " + currentUser.getRole().getDisplayName(),
                        currentUser.getUserId()
                );

                boolean officerNotified = notificationService.notifyInvestigatingOfficerOfClosureApproval(
                        connection,
                        caseId,
                        assignedOfficer,
                        currentUser
                );

                connection.commit();
                return new CaseClosureApprovalResult(
                        existingCase.getStatus(),
                        officerNotified,
                        assignedOfficer == null ? null : assignedOfficer.getName()
                );
            } catch (Exception e) {
                rollbackQuietly(connection);
                throw wrapException("Failed to approve case closure.", e);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not access the database.", e);
        }
    }

    private void validateVerifiedEvidence(Connection connection, int caseId) throws SQLException {
        Evidence evidence = evidenceRepository.findLatestByCaseId(connection, caseId)
                .orElseThrow(() -> new IllegalStateException("Case closure requires a verified evidence record."));

        if (!evidence.hasVerificationSnapshot()) {
            throw new IllegalStateException("Evidence verification must be completed before the case can be closed.");
        }

        if (evidence.getStatus() == EvidenceStatus.TAMPERED) {
            throw new IllegalStateException("Cases with tampered evidence cannot be approved for closure.");
        }

        if (evidence.getStatus() != EvidenceStatus.VERIFIED) {
            throw new IllegalStateException("Only cases with evidence marked as VERIFIED can be approved for closure.");
        }
    }
}
