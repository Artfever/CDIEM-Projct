package com.project.service;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Thin service facade for creating audit entries from UI workflows.
 */
public class LogService {
    private final AuditLogService auditLogService;

    public LogService() {
        this(AuditLogService.getInstance());
    }

    public LogService(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    public void recordAudit(Connection connection, Integer caseId, String action, int userId) throws SQLException {
        auditLogService.recordAudit(connection, caseId, action, userId);
    }
}
