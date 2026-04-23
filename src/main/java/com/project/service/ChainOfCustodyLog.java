package com.project.service;

import com.project.model.Case;
import com.project.model.PriorityState;
import com.project.model.SeverityLevel;
import com.project.model.User;

import java.sql.Connection;
import java.sql.SQLException;

public final class ChainOfCustodyLog {
    private final AuditLogService auditLogService;

    public ChainOfCustodyLog() {
        this(AuditLogService.getInstance());
    }

    public ChainOfCustodyLog(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    public void recordSeverityChange(Connection connection, Case existingCase, SeverityLevel newLevel,
                                     int recalculatedSlaHours, PriorityState recalculatedPriorityState,
                                     User supervisor) throws SQLException {
        auditLogService.recordAudit(
                connection,
                existingCase.getCaseId(),
                "CASE_SEVERITY_CHANGED from " + existingCase.getSeverity().name() + " to " + newLevel.name()
                        + ", SLA " + existingCase.getSlaHours() + "h -> " + recalculatedSlaHours + "h"
                        + ", priority " + existingCase.getPriorityState().name() + " -> "
                        + recalculatedPriorityState.name() + " by " + supervisor.getRole().getDisplayName(),
                supervisor.getUserId()
        );
    }

    public void recordReassignment(Connection connection, Case existingCase, User previousOfficer,
                                   User newOfficer, User supervisor) throws SQLException {
        auditLogService.recordAudit(
                connection,
                existingCase.getCaseId(),
                "CASE_REASSIGNED from Investigating Officer "
                        + buildOfficerIdentity(previousOfficer)
                        + " to " + buildOfficerIdentity(newOfficer)
                        + " by " + supervisor.getRole().getDisplayName(),
                supervisor.getUserId()
        );
    }

    private String buildOfficerIdentity(User officer) {
        if (officer == null) {
            return "UNASSIGNED";
        }

        return officer.getName() + " (ID " + officer.getUserId() + ")";
    }
}
