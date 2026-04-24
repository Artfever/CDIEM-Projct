package com.project.model;

import java.time.LocalDateTime;

public record SummaryReportCaseRecord(int caseId, String title, CaseState caseState, SeverityLevel severity,
                                      PriorityState priorityState, String assignedOfficerName, LocalDateTime createdAt,
                                      LocalDateTime closedAt, Integer slaHours, EvidenceStatus latestEvidenceStatus) {
    public LocalDateTime dueAt() {
        if (createdAt == null || slaHours == null) {
            return null;
        }

        return createdAt.plusHours(slaHours);
    }

    public boolean hasSlaData() {
        return dueAt() != null;
    }

    public boolean isSlaBreached(LocalDateTime referenceTime) {
        LocalDateTime dueAt = dueAt();
        LocalDateTime effectiveReference = effectiveReferenceTime(referenceTime);
        return dueAt != null && effectiveReference != null && effectiveReference.isAfter(dueAt);
    }

    public boolean isSlaCompliant(LocalDateTime referenceTime) {
        LocalDateTime dueAt = dueAt();
        LocalDateTime effectiveReference = effectiveReferenceTime(referenceTime);
        return dueAt != null && effectiveReference != null && !effectiveReference.isAfter(dueAt);
    }

    public boolean isFrozenCase() {
        return caseState == CaseState.FROZEN;
    }

    public boolean hasTamperedEvidence() {
        return latestEvidenceStatus == EvidenceStatus.TAMPERED;
    }

    private LocalDateTime effectiveReferenceTime(LocalDateTime fallbackReference) {
        if (caseState == CaseState.CLOSED && closedAt != null) {
            return closedAt;
        }

        return fallbackReference;
    }
}
