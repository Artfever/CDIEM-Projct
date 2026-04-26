package com.project.controller;

import com.project.model.Case;
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
import com.project.service.ChainOfCustodyLog;
import com.project.service.EvidenceDecisionResult;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * Transaction workflow for confirming evidence as trustworthy.
 * The analyst finalizes the evidence as verified but leaves the case in forensic review.
 */
public class VerificationMarkController extends AbstractCaseWorkflowController {
    private final EvidenceRepository evidenceRepository;
    private final ChainOfCustodyLog chainOfCustodyLog;

    public VerificationMarkController() {
        this(new CaseRepositoryImpl(), new EvidenceRepositoryImpl(), new UserRepositoryImpl(), new ChainOfCustodyLog());
    }

    public VerificationMarkController(CaseRepository caseRepository, EvidenceRepository evidenceRepository,
                                      UserRepository userRepository, ChainOfCustodyLog chainOfCustodyLog) {
        super(caseRepository, userRepository);
        this.evidenceRepository = evidenceRepository;
        this.chainOfCustodyLog = chainOfCustodyLog;
    }

    public EvidenceDecisionResult markAsVerified(int caseId, int userId) {
        try (Connection connection = openConnection()) {
            try {
                User currentUser = requireUserWithRole(
                        connection,
                        userId,
                        UserRole.ANALYST,
                        "Only a Digital Forensic Analyst can mark evidence as verified."
                );
                Case existingCase = requireCase(connection, caseId);
                existingCase.validateForensicReviewState();
                Evidence evidence = requireEvidence(connection, caseId);
                validateVerificationReady(evidence);

                // Verified evidence is finalized here so the assigned officer can later submit the case upward.
                LocalDateTime decisionTime = LocalDateTime.now();
                evidence.markStatus(EvidenceStatus.VERIFIED);
                evidence.recordVerification(evidence.getRecalculatedSha256(), currentUser.getUserId(), decisionTime);
                evidenceRepository.updateStatus(
                        connection,
                        evidence.getEvidenceId(),
                        evidence.getStatus(),
                        evidence.getRecalculatedSha256(),
                        currentUser.getUserId(),
                        decisionTime
                );

                chainOfCustodyLog.recordVerifiedDecision(connection, existingCase, evidence, currentUser);

                connection.commit();
                return new EvidenceDecisionResult(evidence.getStatus(), existingCase.getStatus(),
                        evidence.getRecalculatedSha256());
            } catch (Exception e) {
                rollbackQuietly(connection);
                throw wrapException("Failed to mark evidence as verified.", e);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not access the database.", e);
        }
    }

    private Evidence requireEvidence(Connection connection, int caseId) throws SQLException {
        return evidenceRepository.findLatestByCaseId(connection, caseId)
                .orElseThrow(() -> new IllegalArgumentException("No evidence has been uploaded for case " + caseId + "."));
    }

    private void validateVerificationReady(Evidence evidence) {
        if (!evidence.hasVerificationSnapshot()) {
            throw new IllegalStateException("Run integrity verification before marking evidence as verified.");
        }

        if (evidence.getStatus() == EvidenceStatus.VERIFIED) {
            throw new IllegalStateException("Evidence is already marked as verified and is awaiting officer submission.");
        }

        if (evidence.getStatus() == EvidenceStatus.TAMPERED) {
            throw new IllegalStateException("Tampered evidence cannot be marked as verified.");
        }

        if (!evidence.hashesMatch()) {
            throw new IllegalStateException("The recalculated hash does not match the stored hash. Mark the evidence as tampered instead.");
        }
    }
}
