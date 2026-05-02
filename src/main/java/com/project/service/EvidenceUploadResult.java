package com.project.service;

import com.project.model.CaseState;

/**
 * Data returned after an evidence file is copied, hashed, and recorded.
 */
public record EvidenceUploadResult(int evidenceId, String originalFileName, String sha256Hash,
                                   String storedFilePath, CaseState caseState) {
}
