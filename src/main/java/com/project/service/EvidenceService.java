package com.project.service;

import com.project.controller.EvidenceUploadController;
import com.project.controller.IntegrityVerificationController;
import com.project.controller.TamperMarkController;
import com.project.controller.VerificationMarkController;
import com.project.model.Case;
import com.project.repository.AuditRepository;
import com.project.repository.AuditRepositoryImpl;
import com.project.repository.CaseRepository;
import com.project.repository.CaseRepositoryImpl;
import com.project.repository.EvidenceRepository;
import com.project.repository.EvidenceRepositoryImpl;
import com.project.repository.UserRepository;
import com.project.repository.UserRepositoryImpl;
import com.project.util.DBConnection;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

public class EvidenceService {
    private final CaseRepository caseRepository;
    private final EvidenceRepository evidenceRepository;
    private final EvidenceUploadController evidenceUploadController;
    private final IntegrityVerificationController integrityVerificationController;
    private final VerificationMarkController verificationMarkController;
    private final TamperMarkController tamperMarkController;

    public EvidenceService() {
        this(new CaseRepositoryImpl(), new EvidenceRepositoryImpl(), new AuditRepositoryImpl(), new UserRepositoryImpl());
    }

    public EvidenceService(CaseRepository caseRepository, EvidenceRepository evidenceRepository,
                           AuditRepository auditRepository, UserRepository userRepository) {
        this.caseRepository = caseRepository;
        this.evidenceRepository = evidenceRepository;

        AuditLogService auditLogService = new AuditLogService(auditRepository);
        ChainOfCustodyLog chainOfCustodyLog = new ChainOfCustodyLog(auditLogService);
        HashService hashService = new HashService();
        SecureFileStorage secureFileStorage = new SecureFileStorage();

        this.evidenceUploadController = new EvidenceUploadController(
                caseRepository,
                evidenceRepository,
                userRepository,
                chainOfCustodyLog,
                hashService,
                secureFileStorage
        );
        this.integrityVerificationController = new IntegrityVerificationController(
                caseRepository,
                evidenceRepository,
                userRepository,
                chainOfCustodyLog,
                hashService
        );
        this.verificationMarkController = new VerificationMarkController(
                caseRepository,
                evidenceRepository,
                userRepository,
                chainOfCustodyLog
        );
        this.tamperMarkController = new TamperMarkController(
                caseRepository,
                evidenceRepository,
                userRepository,
                chainOfCustodyLog
        );
    }

    public EvidenceUploadResult uploadEvidence(int caseId, Path sourceFile, int userId) {
        return evidenceUploadController.uploadEvidenceFile(caseId, sourceFile, userId);
    }

    public IntegrityVerificationResult verifyIntegrity(int caseId, int userId) {
        return integrityVerificationController.initiateVerification(caseId, userId);
    }

    public EvidenceDecisionResult markEvidenceVerified(int caseId, int userId) {
        return verificationMarkController.markAsVerified(caseId, userId);
    }

    public EvidenceDecisionResult markEvidenceTampered(int caseId, int userId) {
        return tamperMarkController.markAsTampered(caseId, userId);
    }

    public EvidenceSnapshot getEvidenceSnapshot(int caseId) {
        try (Connection connection = DBConnection.getConnection()) {
            Case caseRecord = caseRepository.findById(connection, caseId)
                    .orElseThrow(() -> new IllegalArgumentException("Case " + caseId + " does not exist."));
            return new EvidenceSnapshot(caseRecord, evidenceRepository.findLatestByCaseId(connection, caseId).orElse(null));
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load evidence details from the database.", e);
        }
    }
}
