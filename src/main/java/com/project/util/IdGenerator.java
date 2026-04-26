package com.project.util;

/**
 * Generates unique IDs for cases.
 * Uses the database-generated ID as the source of truth.
 */
public class IdGenerator {
    public int generateUniqueCaseID(int persistedCaseId) {
        if (persistedCaseId <= 0) {
            throw new IllegalArgumentException("A persisted case ID is required.");
        }

        // The database identity remains the source of truth for case IDs.
        return persistedCaseId;
    }
}
