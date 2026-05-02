package com.project.service;

import com.project.model.CaseState;

/**
 * Data returned after a supervisor approves case closure.
 */
public record CaseClosureApprovalResult(CaseState caseState, boolean investigatingOfficerNotified,
                                        String investigatingOfficerName) {
}
