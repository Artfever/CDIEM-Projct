package com.project.service;

import com.project.model.Case;
import com.project.model.EscalatedCaseReview;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Read-only bundle used to display an escalated case and its SLA status.
 */
public record EscalatedCaseSnapshot(Case caseRecord, EscalatedCaseReview latestReview, LocalDateTime slaDueAt,
                                    boolean slaBreached, Duration breachDuration) {
    public boolean hasLatestReview() {
        return latestReview != null;
    }
}
