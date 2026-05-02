package com.project.service;

import com.project.model.AuditLog;
import com.project.repository.AuditRepository;
import com.project.repository.AuditRepositoryImpl;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Service wrapper for writing and reading audit records.
 * Keeping this logic central makes workflow actions easier to trace consistently.
 */
public final class AuditLogService {
    // Shared instance is used by simple workflow helpers that do not need custom repository injection.
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

    public void deleteAuditTrailByCaseId(Connection connection, int caseId) throws SQLException {
        auditRepository.deleteByCaseId(connection, caseId);
    }

    public List<AuditLog> getAuditTrailByCaseId(Connection connection, int caseId) throws SQLException {
        return auditRepository.findByCaseId(connection, caseId);
    }

    public Optional<Integer> findMostRecentActorByActionPrefixes(Connection connection, int caseId,
                                                                 List<String> actionPrefixes) throws SQLException {
        return auditRepository.findMostRecentActorByActionPrefixes(connection, caseId, actionPrefixes);
    }
}
