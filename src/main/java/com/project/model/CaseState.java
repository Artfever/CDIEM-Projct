package com.project.model;

/**
 * Represents the lifecycle states of a case.
 * Flow: CASE_CREATED → EVIDENCE_UPLOADED → FORENSIC_REVIEW → SUPERVISOR_REVIEW → CLOSED
 * Can also transition to FROZEN, CASE_REASSIGNED at various points.
 */
public enum CaseState {
    CASE_CREATED,
    EVIDENCE_UPLOADED,
    FORENSIC_REVIEW,
    SUPERVISOR_REVIEW,
    FROZEN,
    CASE_REASSIGNED,
    CLOSED
}
