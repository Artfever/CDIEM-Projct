package com.project.service;

import com.project.controller.SubmissionController;
import com.project.model.Case;
import com.project.repository.AuditRepository;
import com.project.repository.AuditRepositoryImpl;
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

public class CaseSubmissionService {
    private final CaseRepository caseRepository;
    private final EvidenceRepository evidenceRepository;
    private final SubmissionController submissionController;

    public CaseSubmissionService() {
        this(new CaseRepositoryImpl(), new EvidenceRepositoryImpl(), new AuditRepositoryImpl(),
                new UserRepositoryImpl(), new NotificationRepositoryImpl());
    }

    public CaseSubmissionService(CaseRepository caseRepository, EvidenceRepository evidenceRepository,
                                 AuditRepository auditRepository, UserRepository userRepository,
                                 NotificationRepository notificationRepository) {
        this.caseRepository = caseRepository;
        this.evidenceRepository = evidenceRepository;

        AuditLogService auditLogService = new AuditLogService(auditRepository);
        ChainOfCustodyLog chainOfCustodyLog = new ChainOfCustodyLog(auditLogService);
        NotificationService notificationService = new NotificationService(notificationRepository);

        this.submissionController = new SubmissionController(
                caseRepository,
                evidenceRepository,
                userRepository,
                chainOfCustodyLog,
                notificationService
        );
    }

    public CaseSubmissionResult submitForSupervisorReview(int caseId, int userId) {
        return submissionController.submitForSupervisorReview(caseId, userId);
    }

    public EvidenceSnapshot getSubmissionSnapshot(int caseId) {
        try (Connection connection = DBConnection.getConnection()) {
            Case caseRecord = caseRepository.findById(connection, caseId)
                    .orElseThrow(() -> new IllegalArgumentException("Case " + caseId + " does not exist."));
            return new EvidenceSnapshot(caseRecord, evidenceRepository.findLatestByCaseId(connection, caseId).orElse(null));
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load case submission details from the database.", e);
        }
    }
}
