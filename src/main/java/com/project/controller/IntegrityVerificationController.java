package com.project.controller;

import com.project.model.Case;
import com.project.model.CaseState;
import com.project.model.Evidence;
import com.project.model.User;
import com.project.model.UserRole;
import com.project.repository.CaseRepository;
import com.project.repository.CaseRepositoryImpl;
import com.project.repository.EvidenceRepository;
import com.project.repository.EvidenceRepositoryImpl;
import com.project.repository.UserRepository;
import com.project.repository.UserRepositoryImpl;
import com.project.service.ChainOfCustodyLog;
import com.project.service.HashService;
import com.project.service.IntegrityVerificationResult;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * Transaction workflow for analyst integrity checks.
 * It recalculates the evidence hash and records whether the stored and live values match.
 */
public class IntegrityVerificationController extends AbstractCaseWorkflowController {
    private final EvidenceRepository evidenceRepository;
    private final ChainOfCustodyLog chainOfCustodyLog;
    private final HashService hashService;

    public IntegrityVerificationController() {
        this(new CaseRepositoryImpl(), new EvidenceRepositoryImpl(), new UserRepositoryImpl(),
                new ChainOfCustodyLog(), new HashService());
    }

    public IntegrityVerificationController(CaseRepository caseRepository, EvidenceRepository evidenceRepository,
                                           UserRepository userRepository, ChainOfCustodyLog chainOfCustodyLog,
                                           HashService hashService) {
        super(caseRepository, userRepository);
        this.evidenceRepository = evidenceRepository;
        this.chainOfCustodyLog = chainOfCustodyLog;
        this.hashService = hashService;
    }

    public IntegrityVerificationResult initiateVerification(int caseId, int userId) {
        try (Connection connection = openConnection()) {
            try {
                User currentUser = requireUserWithRole(
                        connection,
                        userId,
                        UserRole.ANALYST,
                        "Only a Digital Forensic Analyst can verify evidence integrity."
                );
                Case existingCase = requireCase(connection, caseId);
                existingCase.validateEvidenceVerificationState();
                Evidence evidence = requireEvidence(connection, caseId);

                // The analyst compares what was stored earlier with what the file looks like right now.
                String storedHash = hashService.retrieveStoredHash(evidence);
                String recalculatedHash = hashService.recalculateHash(evidence);
                boolean matched = hashService.compareHashes(storedHash, recalculatedHash);
                LocalDateTime verifiedAt = LocalDateTime.now();

                evidence.recordVerification(recalculatedHash, currentUser.getUserId(), verifiedAt);
                evidenceRepository.updateVerificationSnapshot(
                        connection,
                        evidence.getEvidenceId(),
                        recalculatedHash,
                        currentUser.getUserId(),
                        verifiedAt
                );

                // After the check, the case sits in forensic review until the analyst finalizes the outcome.
                existingCase.moveToState(CaseState.FORENSIC_REVIEW);
                caseRepository.updateState(connection, caseId, existingCase.getStatus());
                chainOfCustodyLog.recordIntegrityVerification(
                        connection,
                        existingCase,
                        evidence,
                        recalculatedHash,
                        matched,
                        currentUser
                );

                connection.commit();
                return new IntegrityVerificationResult(
                        evidence.getEvidenceId(),
                        storedHash,
                        recalculatedHash,
                        matched,
                        evidence.getStatus(),
                        existingCase.getStatus()
                );
            } catch (Exception e) {
                rollbackQuietly(connection);
                throw wrapException("Failed to verify evidence integrity.", e);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not access the database.", e);
        }
    }

    private Evidence requireEvidence(Connection connection, int caseId) throws SQLException {
        return evidenceRepository.findLatestByCaseId(connection, caseId)
                .orElseThrow(() -> new IllegalArgumentException("No evidence has been uploaded for case " + caseId + "."));
    }
}
