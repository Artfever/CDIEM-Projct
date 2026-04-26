package com.project.model;

/**
 * Case priority states for workflow management.
 * Determines how urgent a case is and how it should be handled.
 */
public enum PriorityState {
    STANDARD("Standard"),
    PRIORITY("Priority"),
    ESCALATED("Escalated"),
    UNDER_ACTIVE_REVIEW("Under Active Review");

    private final String displayName;

    PriorityState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
