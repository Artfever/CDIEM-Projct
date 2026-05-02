package com.project.service;

import com.project.model.AuditLog;
import com.project.model.Case;

import java.util.List;

/**
 * Read-only data shown by the chain-of-custody screen.
 */
public record ChainOfCustodySnapshot(Case caseRecord, List<AuditLog> auditEntries, boolean inspectionMode) {
    public int entryCount() {
        return auditEntries == null ? 0 : auditEntries.size();
    }
}
