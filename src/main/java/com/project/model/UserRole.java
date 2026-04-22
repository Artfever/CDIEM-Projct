package com.project.model;

public enum UserRole {
    OFFICER("Investigating Officer"),
    ANALYST("Digital Forensic Analyst"),
    SUPERVISOR("Supervisory Authority");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
