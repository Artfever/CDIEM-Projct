package com.project.model;

/**
 * Outcome of a case closure review.
 * - APPROVED: Case can be closed
 * - REJECTED: Case needs revision before closure
 */
public enum CaseClosureDecisionType {
    APPROVED("Approved"),
    REJECTED("Rejected");

    private final String displayName;

    CaseClosureDecisionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
