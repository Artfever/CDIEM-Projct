package com.project.service;

import com.project.model.Case;
import com.project.model.Evidence;
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

    public void recordEvidenceUpload(Connection connection, Case existingCase, Evidence evidence, User officer)
            throws SQLException {
        auditLogService.recordAudit(
                connection,
                existingCase.getCaseId(),
                "EVIDENCE_UPLOADED file " + evidence.getOriginalFileName()
                        + ", hash " + abbreviateHash(evidence.getOriginalSha256())
                        + ", state EVIDENCE_UPLOADED by " + officer.getRole().getDisplayName(),
                officer.getUserId()
        );
    }

    public void recordIntegrityVerification(Connection connection, Case existingCase, Evidence evidence,
                                            String recalculatedHash, boolean matched, User analyst)
            throws SQLException {
        auditLogService.recordAudit(
                connection,
                existingCase.getCaseId(),
                "EVIDENCE_HASH_CHECK " + (matched ? "MATCH" : "MISMATCH")
                        + " stored " + abbreviateHash(evidence.getOriginalSha256())
                        + " vs " + abbreviateHash(recalculatedHash)
                        + " by " + analyst.getRole().getDisplayName(),
                analyst.getUserId()
        );
    }

    public void recordVerifiedDecision(Connection connection, Case existingCase, Evidence evidence, User analyst)
            throws SQLException {
        auditLogService.recordAudit(
                connection,
                existingCase.getCaseId(),
                "EVIDENCE_MARKED_VERIFIED file " + evidence.getOriginalFileName()
                        + ", state SUPERVISOR_REVIEW by " + analyst.getRole().getDisplayName(),
                analyst.getUserId()
        );
    }

    public void recordTamperedDecision(Connection connection, Case existingCase, Evidence evidence, User analyst)
            throws SQLException {
        auditLogService.recordAudit(
                connection,
                existingCase.getCaseId(),
                "EVIDENCE_MARKED_TAMPERED file " + evidence.getOriginalFileName()
                        + ", state FROZEN by " + analyst.getRole().getDisplayName(),
                analyst.getUserId()
        );
    }

    private String buildOfficerIdentity(User officer) {
        if (officer == null) {
            return "UNASSIGNED";
        }

        return officer.getName() + " (ID " + officer.getUserId() + ")";
    }

    private String abbreviateHash(String hash) {
        if (hash == null || hash.isBlank()) {
            return "n/a";
        }

        return hash.length() <= 12 ? hash : hash.substring(0, 12);
    }
}
