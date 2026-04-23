package com.project.model;

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
