package com.project.service;

import com.project.model.CaseState;

/**
 * Data returned when case closure is rejected and sent back for more work.
 */
public record CaseClosureRejectionResult(CaseState caseState, String reason) {
}
