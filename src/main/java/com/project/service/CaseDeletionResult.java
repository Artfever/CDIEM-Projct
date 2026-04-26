package com.project.service;

import com.project.model.CaseState;

public record CaseDeletionResult(int caseId, CaseState previousState, String relatedUserName,
                                 boolean relatedUserNotified, int deletedEvidenceFileCount,
                                 int retainedEvidenceFileCount) {
}
