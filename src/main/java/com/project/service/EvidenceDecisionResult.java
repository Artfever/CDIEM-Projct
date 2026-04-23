package com.project.service;

import com.project.model.CaseState;
import com.project.model.EvidenceStatus;

public record EvidenceDecisionResult(EvidenceStatus evidenceStatus, CaseState caseState,
                                     String recalculatedHash) {
}
