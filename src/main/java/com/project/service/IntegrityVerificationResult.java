package com.project.service;

import com.project.model.CaseState;
import com.project.model.EvidenceStatus;

/**
 * Result returned after recalculating and comparing an evidence hash.
 */
public record IntegrityVerificationResult(int evidenceId, String storedHash, String recalculatedHash,
                                          boolean matched, EvidenceStatus evidenceStatus,
                                          CaseState caseState) {
}
