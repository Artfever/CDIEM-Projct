package com.project.service;

import com.project.model.Case;
import com.project.model.Evidence;

/**
 * Read-only bundle used by evidence and state-transition screens.
 */
public record EvidenceSnapshot(Case caseRecord, Evidence evidence) {
    public boolean hasEvidence() {
        return evidence != null;
    }
}
