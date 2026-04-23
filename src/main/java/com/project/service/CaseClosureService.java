package com.project.service;

import com.project.controller.CaseClosureController;
import com.project.controller.RejectionController;
import com.project.model.Case;
import com.project.repository.AuditRepository;
import com.project.repository.AuditRepositoryImpl;
import com.project.repository.CaseClosureDecisionRepository;
import com.project.repository.CaseClosureDecisionRepositoryImpl;
import com.project.repository.CaseRepository;
import com.project.repository.CaseRepositoryImpl;
import com.project.repository.EvidenceRepository;
import com.project.repository.EvidenceRepositoryImpl;
import com.project.repository.NotificationRepository;
import com.project.repository.NotificationRepositoryImpl;
import com.project.repository.UserRepository;
import com.project.repository.UserRepositoryImpl;
import com.project.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;

public class CaseClosureService {
    private final CaseRepository caseRepository;
    private final EvidenceRepository evidenceRepository;
    private final CaseClosureDecisionRepository closureDecisionRepository;
    private final CaseClosureController caseClosureController;
    private final RejectionController rejectionController;

    public CaseClosureService() {
        this(new CaseRepositoryImpl(), new EvidenceRepositoryImpl(), new CaseClosureDecisionRepositoryImpl(),
                new AuditRepositoryImpl(), new UserRepositoryImpl(), new NotificationRepositoryImpl());
    }

    public CaseClosureService(CaseRepository caseRepository, EvidenceRepository evidenceRepository,
                              CaseClosureDecisionRepository closureDecisionRepository, AuditRepository auditRepository,
                              UserRepository userRepository, NotificationRepository notificationRepository) {
        this.caseRepository = caseRepository;
        this.evidenceRepository = evidenceRepository;
        this.closureDecisionRepository = closureDecisionRepository;

        AuditLogService auditLogService = new AuditLogService(auditRepository);
        ChainOfCustodyLog chainOfCustodyLog = new ChainOfCustodyLog(auditLogService);
        NotificationService notificationService = new NotificationService(notificationRepository);

        this.caseClosureController = new CaseClosureController(
                caseRepository,
                evidenceRepository,
                closureDecisionRepository,
                userRepository,
                auditLogService,
                notificationService
        );
        this.rejectionController = new RejectionController(
                caseRepository,
                userRepository,
                chainOfCustodyLog,
                closureDecisionRepository
        );
    }

    public CaseClosureApprovalResult approveClosure(int caseId, int userId) {
        return caseClosureController.approveClosure(caseId, userId);
    }

    public CaseClosureRejectionResult rejectClosure(int caseId, String reason, int userId) {
        return rejectionController.submitRejection(caseId, reason, userId);
    }

    public CaseClosureSnapshot getClosureSnapshot(int caseId) {
        try (Connection connection = DBConnection.getConnection()) {
            Case caseRecord = caseRepository.findById(connection, caseId)
                    .orElseThrow(() -> new IllegalArgumentException("Case " + caseId + " does not exist."));
            return new CaseClosureSnapshot(
                    caseRecord,
                    evidenceRepository.findLatestByCaseId(connection, caseId).orElse(null),
                    closureDecisionRepository.findLatestByCaseId(connection, caseId).orElse(null)
            );
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load case closure details from the database.", e);
        }
    }
}
