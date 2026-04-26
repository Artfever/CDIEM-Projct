package com.project.model;

/**
 * Defines user roles in the system.
 * - OFFICER: Investigating Officer (frontline case work)
 * - ANALYST: Digital Forensic Analyst (evidence analysis)
 * - SUPERVISOR: Supervisory Authority (approvals and reviews)
 */
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
