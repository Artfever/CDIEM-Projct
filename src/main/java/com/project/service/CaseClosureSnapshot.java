package com.project.service;

import com.project.model.Case;
import com.project.model.CaseClosureDecision;
import com.project.model.Evidence;

/**
 * Read-only bundle used to show the current closure state on the UI.
 */
public record CaseClosureSnapshot(Case caseRecord, Evidence evidence, CaseClosureDecision latestDecision) {
    public boolean hasEvidence() {
        return evidence != null;
    }

    public boolean hasLatestDecision() {
        return latestDecision != null;
    }
}
