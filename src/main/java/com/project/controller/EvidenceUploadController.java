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
import com.project.service.ChainOfCustodyLog;
import com.project.service.EvidenceUploadResult;
import com.project.service.HashService;
import com.project.service.SecureFileStorage;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * Transaction workflow for evidence upload.
 * It stores the file securely, records its hash, and moves the case into evidence intake.
 */
public class EvidenceUploadController extends AbstractCaseWorkflowController {
    private final EvidenceRepository evidenceRepository;
    private final ChainOfCustodyLog chainOfCustodyLog;
    private final HashService hashService;
    private final SecureFileStorage secureFileStorage;

    public EvidenceUploadController() {
        this(new CaseRepositoryImpl(), new EvidenceRepositoryImpl(), new UserRepositoryImpl(),
                new ChainOfCustodyLog(), new HashService(), new SecureFileStorage());
    }

    public EvidenceUploadController(CaseRepository caseRepository, EvidenceRepository evidenceRepository,
                                    UserRepository userRepository, ChainOfCustodyLog chainOfCustodyLog,
                                    HashService hashService, SecureFileStorage secureFileStorage) {
        super(caseRepository, userRepository);
        this.evidenceRepository = evidenceRepository;
        this.chainOfCustodyLog = chainOfCustodyLog;
        this.hashService = hashService;
        this.secureFileStorage = secureFileStorage;
    }

    public EvidenceUploadResult uploadEvidenceFile(int caseId, Path sourceFile, int userId) {
        if (sourceFile == null) {
            throw new IllegalArgumentException("An evidence file must be selected.");
        }

        Path storedFile = null;

        try (Connection connection = openConnection()) {
            try {
                User currentUser = requireUserWithRole(
                        connection,
                        userId,
                        UserRole.OFFICER,
                        "Only an Investigating Officer can upload evidence."
                );
                Case existingCase = requireCase(connection, caseId);
                existingCase.validateEvidenceUploadState();
                validateAssignedOfficerOwnership(existingCase, currentUser);

                // The stored copy becomes the official evidence file used for later integrity checks.
                storedFile = secureFileStorage.storeSecurely(sourceFile, caseId);
                String hashValue = hashService.generateSHA256(storedFile);
                Evidence evidence = new Evidence(
                        caseId,
                        sourceFile.getFileName().toString(),
                        storedFile.toString(),
                        hashValue,
                        EvidenceStatus.UPLOADED,
                        currentUser.getUserId(),
                        LocalDateTime.now()
                );

                int evidenceId = evidenceRepository.save(connection, evidence);
                chainOfCustodyLog.recordEvidenceUpload(connection, existingCase, evidence, currentUser);
                // Once evidence is on record, the case leaves creation/reassignment and enters evidence intake.
                existingCase.moveToState(CaseState.EVIDENCE_UPLOADED);
                caseRepository.updateState(connection, caseId, existingCase.getStatus());

                connection.commit();
                return new EvidenceUploadResult(
                        evidenceId,
                        evidence.getOriginalFileName(),
                        hashValue,
                        evidence.getStoredFilePath(),
                        existingCase.getStatus()
                );
            } catch (Exception e) {
                rollbackQuietly(connection);
                secureFileStorage.deleteQuietly(storedFile);
                throw wrapException("Failed to upload evidence.", e);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not access the database.", e);
        }
    }

    private void validateAssignedOfficerOwnership(Case existingCase, User currentUser) {
        if (existingCase.getAssignedOfficerId() == null || existingCase.getAssignedOfficerId() != currentUser.getUserId()) {
            throw new IllegalStateException("Only the assigned Investigating Officer can upload evidence for this case.");
        }
    }
}
