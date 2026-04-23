package com.project.service;

import com.project.repository.AuditRepository;
import com.project.repository.AuditRepositoryImpl;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public final class AuditLogService {
    private static final AuditLogService INSTANCE = new AuditLogService(new AuditRepositoryImpl());

    private final AuditRepository auditRepository;

    public static AuditLogService getInstance() {
        return INSTANCE;
    }

    public AuditLogService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    public void recordAudit(Connection connection, Integer caseId, String action, int userId) throws SQLException {
        auditRepository.logAction(connection, caseId, action, userId);
    }

    public Optional<Integer> findMostRecentActorByActionPrefixes(Connection connection, int caseId,
                                                                 List<String> actionPrefixes) throws SQLException {
        return auditRepository.findMostRecentActorByActionPrefixes(connection, caseId, actionPrefixes);
    }
}
