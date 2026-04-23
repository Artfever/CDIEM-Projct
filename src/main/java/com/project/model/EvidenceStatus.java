package com.project.model;

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
