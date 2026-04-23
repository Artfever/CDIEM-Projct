package com.project.service;

import com.project.model.CaseState;

public record CaseClosureRejectionResult(CaseState caseState, String reason) {
}
