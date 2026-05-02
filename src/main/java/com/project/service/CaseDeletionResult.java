package com.project.service;

import com.project.model.CaseState;

/**
 * Summary of what was removed or retained after deleting a case.
 */
public record CaseDeletionResult(int caseId, CaseState previousState, String relatedUserName,
                                 boolean relatedUserNotified, int deletedEvidenceFileCount,
                                 int retainedEvidenceFileCount) {
}
