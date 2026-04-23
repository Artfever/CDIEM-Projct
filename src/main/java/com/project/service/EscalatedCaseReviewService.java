package com.project.service;

import com.project.controller.EscalatedCaseController;
import com.project.model.Case;
import com.project.repository.AuditRepository;
import com.project.repository.AuditRepositoryImpl;
import com.project.repository.CaseRepository;
import com.project.repository.CaseRepositoryImpl;
import com.project.repository.EscalatedCaseReviewRepository;
import com.project.repository.EscalatedCaseReviewRepositoryImpl;
import com.project.repository.UserRepository;
import com.project.repository.UserRepositoryImpl;
import com.project.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;

public class EscalatedCaseReviewService {
    private final CaseRepository caseRepository;
    private final EscalatedCaseReviewRepository escalatedCaseReviewRepository;
    private final EscalatedCaseController escalatedCaseController;

    public EscalatedCaseReviewService() {
        this(new CaseRepositoryImpl(), new EscalatedCaseReviewRepositoryImpl(),
                new AuditRepositoryImpl(), new UserRepositoryImpl());
    }

    public EscalatedCaseReviewService(CaseRepository caseRepository,
                                      EscalatedCaseReviewRepository escalatedCaseReviewRepository,
                                      AuditRepository auditRepository,
                                      UserRepository userRepository) {
        this.caseRepository = caseRepository;
        this.escalatedCaseReviewRepository = escalatedCaseReviewRepository;

        ChainOfCustodyLog chainOfCustodyLog = new ChainOfCustodyLog(new AuditLogService(auditRepository));
        this.escalatedCaseController = new EscalatedCaseController(
                caseRepository,
                userRepository,
                chainOfCustodyLog,
                escalatedCaseReviewRepository
        );
    }

    public EscalatedCaseReviewResult recordReviewDecision(int caseId, String instructions, int userId) {
        return escalatedCaseController.recordReviewDecision(caseId, instructions, userId);
    }

    public EscalatedCaseSnapshot getSnapshot(int caseId) {
        try (Connection connection = DBConnection.getConnection()) {
            Case caseRecord = caseRepository.findById(connection, caseId)
                    .orElseThrow(() -> new IllegalArgumentException("Case " + caseId + " does not exist."));

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime slaDueAt = caseRecord.getSlaDueAt();
            boolean slaBreached = caseRecord.hasBreachedSla(now);
            Duration breachDuration = !slaBreached || slaDueAt == null ? Duration.ZERO : Duration.between(slaDueAt, now);

            return new EscalatedCaseSnapshot(
                    caseRecord,
                    escalatedCaseReviewRepository.findLatestByCaseId(connection, caseId).orElse(null),
                    slaDueAt,
                    slaBreached,
                    breachDuration
            );
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load escalated case details from the database.", e);
        }
    }
}
