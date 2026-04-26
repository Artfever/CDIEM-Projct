package com.project.model;

/**
 * Status of evidence in the system.
 * - UPLOADED: Initial state when evidence is added
 * - VERIFIED: Hash matches, evidence is intact
 * - TAMPERED: Hash mismatch, evidence may have been altered
 */
public enum EvidenceStatus {
    UPLOADED("Uploaded"),
    VERIFIED("Verified"),
    TAMPERED("Tampered");

    private final String displayName;

    EvidenceStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
