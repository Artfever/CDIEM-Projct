package com.project.service;

import com.project.model.CaseState;
import com.project.model.EvidenceStatus;

/**
 * Result returned after evidence is marked verified or tampered.
 */
public record EvidenceDecisionResult(EvidenceStatus evidenceStatus, CaseState caseState,
                                     String recalculatedHash) {
}
