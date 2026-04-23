package com.project.service;

import com.project.model.CaseState;

public record CaseSubmissionResult(CaseState caseState, int notifiedSupervisorCount) {
}
