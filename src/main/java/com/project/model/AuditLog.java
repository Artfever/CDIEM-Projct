package com.project.model;

import java.time.LocalDateTime;

public class AuditLog {
    private final int logId;
    private final Integer caseId;
    private final String action;
    private final int performedBy;
    private final LocalDateTime timestamp;

    public AuditLog(int logId, Integer caseId, String action, int performedBy, LocalDateTime timestamp) {
        this.logId = logId;
        this.caseId = caseId;
        this.action = action;
        this.performedBy = performedBy;
        this.timestamp = timestamp;
    }

    public int getLogId() {
        return logId;
    }

    public Integer getCaseId() {
        return caseId;
    }

    public String getAction() {
        return action;
    }

    public int getPerformedBy() {
        return performedBy;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
