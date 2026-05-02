package com.project.service;

import com.project.model.CaseState;

/**
 * Result returned after an officer submits a case for supervisor review.
 */
public record CaseSubmissionResult(CaseState caseState, int notifiedSupervisorCount) {
}
