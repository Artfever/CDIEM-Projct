package com.project.service;

import com.project.model.AuditLog;
import com.project.model.Case;
import com.project.model.Evidence;
import com.project.model.PriorityState;
import com.project.model.SeverityLevel;
import com.project.model.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Builds the audit messages that form the chain-of-custody log.
 * Each public method records one important workflow event in readable form.
 */
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
                        + ", case remains FORENSIC_REVIEW awaiting officer submission by "
                        + analyst.getRole().getDisplayName(),
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

    public void recordFreeze(Connection connection, Case existingCase, User analyst) throws SQLException {
        auditLogService.recordAudit(
                connection,
                existingCase.getCaseId(),
                "CASE_FROZEN by " + analyst.getRole().getDisplayName(),
                analyst.getUserId()
        );
    }

    public void recordReopen(Connection connection, Case existingCase, User supervisor, String reason)
            throws SQLException {
        auditLogService.recordAudit(
                connection,
                existingCase.getCaseId(),
                "CASE_REOPENED reason: " + reason + ", state SUPERVISOR_REVIEW by "
                        + supervisor.getRole().getDisplayName(),
                supervisor.getUserId()
        );
    }

    public void recordTamperedEvidenceReset(Connection connection, Case existingCase, Evidence evidence,
                                            User supervisor) throws SQLException {
        auditLogService.recordAudit(
                connection,
                existingCase.getCaseId(),
                "EVIDENCE_TAMPER_STATUS_RESET_TO_UPLOADED file " + evidence.getOriginalFileName()
                        + " during case reopen by " + supervisor.getRole().getDisplayName(),
                supervisor.getUserId()
        );
    }

    public void recordClosureRejection(Connection connection, Case existingCase, User supervisor, String reason)
            throws SQLException {
        auditLogService.recordAudit(
                connection,
                existingCase.getCaseId(),
                "CASE_CLOSURE_REJECTED reason: " + reason + ", state FORENSIC_REVIEW by "
                        + supervisor.getRole().getDisplayName(),
                supervisor.getUserId()
        );
    }

    public void recordSubmission(Connection connection, Case existingCase, User officer) throws SQLException {
        auditLogService.recordAudit(
                connection,
                existingCase.getCaseId(),
                "CASE_SUBMITTED_FOR_SUPERVISOR_REVIEW by " + officer.getRole().getDisplayName(),
                officer.getUserId()
        );
    }

    public void recordEscalatedReview(Connection connection, Case existingCase, User supervisor, String instructions)
            throws SQLException {
        auditLogService.recordAudit(
                connection,
                existingCase.getCaseId(),
                "ESCALATED_CASE_REVIEWED instructions: " + instructions + ", priority UNDER_ACTIVE_REVIEW by "
                        + supervisor.getRole().getDisplayName(),
                supervisor.getUserId()
        );
    }

    public List<AuditLog> retrieveImmutableEntries(Connection connection, int caseId) throws SQLException {
        return auditLogService.getAuditTrailByCaseId(connection, caseId);
    }

    public Optional<Integer> findLatestFreezingAnalystId(Connection connection, int caseId) throws SQLException {
        // Reopen notifications need the last analyst who froze or marked evidence as tampered.
        return auditLogService.findMostRecentActorByActionPrefixes(
                connection,
                caseId,
                List.of("CASE_FROZEN%", "EVIDENCE_MARKED_TAMPERED%")
        );
    }

    private String buildOfficerIdentity(User officer) {
        if (officer == null) {
            return "UNASSIGNED";
        }

        return officer.getName() + " (ID " + officer.getUserId() + ")";
    }

    private String abbreviateHash(String hash) {
        // Audit messages show enough of the hash to compare entries without flooding the log UI.
        if (hash == null || hash.isBlank()) {
            return "n/a";
        }

        return hash.length() <= 12 ? hash : hash.substring(0, 12);
    }
}
