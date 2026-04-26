package com.project.model;

import java.time.LocalDateTime;

/**
 * Records a supervisor's decision on a case closure request.
 * Tracks the decision type, reason, and state transitions.
 */
public class CaseClosureDecision {
    private final int decisionId;
    private final int caseId;
    private final CaseClosureDecisionType decisionType;
    private final String reason;
    private final CaseState previousState;
    private final CaseState resultingState;
    private final int decidedByUserId;
    private final String decidedByName;
    private final LocalDateTime decidedAt;

    public CaseClosureDecision(int decisionId, int caseId, CaseClosureDecisionType decisionType, String reason,
                               CaseState previousState, CaseState resultingState, int decidedByUserId,
                               String decidedByName, LocalDateTime decidedAt) {
        this.decisionId = decisionId;
        this.caseId = caseId;
        this.decisionType = decisionType;
        this.reason = reason;
        this.previousState = previousState;
        this.resultingState = resultingState;
        this.decidedByUserId = decidedByUserId;
        this.decidedByName = decidedByName;
        this.decidedAt = decidedAt;
    }

    public int getDecisionId() {
        return decisionId;
    }

    public int getCaseId() {
        return caseId;
    }

    public CaseClosureDecisionType getDecisionType() {
        return decisionType;
    }

    public String getReason() {
        return reason;
    }

    public CaseState getPreviousState() {
        return previousState;
    }

    public CaseState getResultingState() {
        return resultingState;
    }

    public int getDecidedByUserId() {
        return decidedByUserId;
    }

    public String getDecidedByName() {
        return decidedByName;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }
}
