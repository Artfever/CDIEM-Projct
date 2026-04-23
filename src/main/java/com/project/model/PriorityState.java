package com.project.model;

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
