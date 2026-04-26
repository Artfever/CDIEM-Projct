package com.project.controller;

import com.project.model.Case;
import com.project.model.CaseState;
import com.project.model.Evidence;
import com.project.model.User;
import com.project.model.UserRole;
import com.project.repository.AuditRepositoryImpl;
import com.project.repository.CaseClosureDecisionRepository;
import com.project.repository.CaseClosureDecisionRepositoryImpl;
import com.project.repository.CaseRepository;
import com.project.repository.CaseRepositoryImpl;
import com.project.repository.EscalatedCaseReviewRepository;
import com.project.repository.EscalatedCaseReviewRepositoryImpl;
import com.project.repository.EvidenceRepository;
import com.project.repository.EvidenceRepositoryImpl;
import com.project.repository.NotificationRepositoryImpl;
import com.project.repository.UserRepository;
import com.project.repository.UserRepositoryImpl;
import com.project.service.AuditLogService;
import com.project.service.CaseDeletionResult;
import com.project.service.NotificationService;
import com.project.service.SecureFileStorage;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class CaseDeletionController extends AbstractCaseWorkflowController {
    private final EvidenceRepository evidenceRepository;
    private final CaseClosureDecisionRepository caseClosureDecisionRepository;
    private final EscalatedCaseReviewRepository escalatedCaseReviewRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final SecureFileStorage secureFileStorage;

    public CaseDeletionController() {
        this(new CaseRepositoryImpl(), new EvidenceRepositoryImpl(), new CaseClosureDecisionRepositoryImpl(),
                new EscalatedCaseReviewRepositoryImpl(), new UserRepositoryImpl(),
                new AuditLogService(new AuditRepositoryImpl()), new NotificationService(new NotificationRepositoryImpl()),
                new SecureFileStorage());
    }

    public CaseDeletionController(CaseRepository caseRepository, EvidenceRepository evidenceRepository,
                                  CaseClosureDecisionRepository caseClosureDecisionRepository,
                                  EscalatedCaseReviewRepository escalatedCaseReviewRepository,
                                  UserRepository userRepository, AuditLogService auditLogService,
                                  NotificationService notificationService, SecureFileStorage secureFileStorage) {
        super(caseRepository, userRepository);
        this.evidenceRepository = evidenceRepository;
        this.caseClosureDecisionRepository = caseClosureDecisionRepository;
        this.escalatedCaseReviewRepository = escalatedCaseReviewRepository;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
        this.secureFileStorage = secureFileStorage;
    }

    public CaseDeletionResult deleteCase(int caseId, int userId) {
        try (Connection connection = openConnection()) {
            try {
                User currentUser = requireUserWithRole(
                        connection,
                        userId,
                        UserRole.SUPERVISOR,
                        "Only the Supervisory Authority can delete a case."
                );
                Case existingCase = requireCase(connection, caseId);
                CaseState previousState = existingCase.getStatus();
                User relatedUser = existingCase.getAssignedOfficerId() == null
                        ? null
                        : requireUser(connection, existingCase.getAssignedOfficerId());
                // Capture file paths now because the evidence rows will be removed before the case row is deleted.
                List<Evidence> evidenceRecords = evidenceRepository.findByCaseId(connection, caseId);

                // Remove linked records first so the case no longer has anything pointing at it.
                caseClosureDecisionRepository.deleteByCaseId(connection, caseId);
                escalatedCaseReviewRepository.deleteByCaseId(connection, caseId);
                notificationService.deleteCaseNotifications(connection, caseId);
                auditLogService.deleteAuditTrailByCaseId(connection, caseId);
                evidenceRepository.deleteByCaseId(connection, caseId);
                caseRepository.deleteById(connection, caseId);

                // Send the user-facing delete notice after the case row is gone, so the notification stands on its own.
                boolean relatedUserNotified = notificationService.notifyInvestigatingOfficerOfCaseDeletion(
                        connection,
                        caseId,
                        relatedUser,
                        currentUser
                );
                // Keep an accountability record without tying it to a deleted case row.
                auditLogService.recordAudit(
                        connection,
                        null,
                        buildDeletionAuditMessage(existingCase, currentUser, relatedUser, relatedUserNotified),
                        currentUser.getUserId()
                );

                connection.commit();

                // Database cleanup happens inside the transaction; file cleanup happens after commit.
                StorageCleanupResult cleanupResult = deleteStoredEvidenceFiles(evidenceRecords);
                return new CaseDeletionResult(
                        caseId,
                        previousState,
                        relatedUser == null ? null : relatedUser.getName(),
                        relatedUserNotified,
                        cleanupResult.deletedCount(),
                        cleanupResult.retainedCount()
                );
            } catch (Exception e) {
                rollbackQuietly(connection);
                throw wrapException("Failed to delete case.", e);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not access the database.", e);
        }
    }

    private String buildDeletionAuditMessage(Case existingCase, User supervisor, User relatedUser,
                                             boolean relatedUserNotified) {
        String relatedUserSummary = relatedUser == null
                ? "no related user notification target was available"
                : "related user " + relatedUser.getName() + (relatedUserNotified ? " was notified" : " was not notified");

        return "CASE_DELETED former CaseID " + existingCase.getCaseId()
                + ", title \"" + sanitizeForAudit(existingCase.getTitle()) + "\""
                + ", former state " + existingCase.getStatus().name()
                + " by " + supervisor.getRole().getDisplayName() + " " + supervisor.getName()
                + "; " + relatedUserSummary + ".";
    }

    private StorageCleanupResult deleteStoredEvidenceFiles(List<Evidence> evidenceRecords) {
        int deletedCount = 0;
        int retainedCount = 0;

        for (Evidence evidence : evidenceRecords) {
            Path storedFile = resolveStoredFile(evidence.getStoredFilePath());
            if (storedFile == null) {
                retainedCount++;
                continue;
            }

            if (secureFileStorage.deleteStoredFile(storedFile)) {
                deletedCount++;
            } else {
                retainedCount++;
            }
        }

        return new StorageCleanupResult(deletedCount, retainedCount);
    }

    private Path resolveStoredFile(String storedFilePath) {
        if (storedFilePath == null || storedFilePath.isBlank()) {
            return null;
        }

        try {
            return Path.of(storedFilePath);
        } catch (InvalidPathException ignored) {
            return null;
        }
    }

    private String sanitizeForAudit(String value) {
        if (value == null || value.isBlank()) {
            return "Untitled";
        }

        return value.replace('"', '\'');
    }

    private record StorageCleanupResult(int deletedCount, int retainedCount) {
    }
}
