package com.project.service;

import com.project.model.CaseState;

public record CaseClosureApprovalResult(CaseState caseState, boolean investigatingOfficerNotified,
                                        String investigatingOfficerName) {
}
