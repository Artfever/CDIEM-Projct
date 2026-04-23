package com.project.service;

import com.project.model.CaseState;

public record EvidenceUploadResult(int evidenceId, String originalFileName, String sha256Hash,
                                   String storedFilePath, CaseState caseState) {
}
