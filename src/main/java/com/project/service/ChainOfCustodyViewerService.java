package com.project.service;

import com.project.model.Case;
import com.project.model.CaseState;
import com.project.repository.AuditRepository;
import com.project.repository.AuditRepositoryImpl;
import com.project.repository.CaseRepository;
import com.project.repository.CaseRepositoryImpl;
import com.project.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Read-only service behind the chain-of-custody screen.
 * It returns the case together with its full audit history and inspection mode flag.
 */
public class ChainOfCustodyViewerService {
    private final CaseRepository caseRepository;
    private final ChainOfCustodyLog chainOfCustodyLog;

    public ChainOfCustodyViewerService() {
        this(new CaseRepositoryImpl(), new AuditRepositoryImpl());
    }

    public ChainOfCustodyViewerService(CaseRepository caseRepository, AuditRepository auditRepository) {
        this.caseRepository = caseRepository;
        this.chainOfCustodyLog = new ChainOfCustodyLog(new AuditLogService(auditRepository));
    }

    public ChainOfCustodySnapshot getSnapshot(int caseId) {
        try (Connection connection = DBConnection.getConnection()) {
            Case caseRecord = caseRepository.findById(connection, caseId)
                    .orElseThrow(() -> new IllegalArgumentException("Case " + caseId + " does not exist."));
            // Frozen cases are highlighted so supervisors know they are looking at a paused workflow.
            return new ChainOfCustodySnapshot(
                    caseRecord,
                    chainOfCustodyLog.retrieveImmutableEntries(connection, caseId),
                    caseRecord.getStatus() == CaseState.FROZEN
            );
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load chain-of-custody log details from the database.", e);
        }
    }
}
