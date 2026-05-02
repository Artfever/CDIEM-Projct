package com.project.service;

import com.project.model.CaseState;

public record CaseReopenResult(CaseState caseState, String reason, boolean tamperedEvidenceResetToDefault,
                               boolean investigatingOfficerNotified, boolean forensicAnalystNotified,
                               String forensicAnalystName) {
}
