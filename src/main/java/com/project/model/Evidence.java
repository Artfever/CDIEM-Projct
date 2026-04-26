package com.project.model;

import java.time.LocalDateTime;

/**
 * Represents digital evidence associated with a case.
 * Tracks file info, hash for integrity verification, and verification history.
 */
public class Evidence {
    private Integer evidenceId;
    private Integer caseId;
    private String originalFileName;
    private String storedFilePath;
    private String originalSha256;
    private String recalculatedSha256;
    private EvidenceStatus status;
    private Integer uploadedByUserId;
    private LocalDateTime uploadedAt;
    private Integer lastVerifiedByUserId;
    private LocalDateTime lastVerifiedAt;

    public Evidence(Integer caseId, String originalFileName, String storedFilePath, String originalSha256,
                    EvidenceStatus status, Integer uploadedByUserId, LocalDateTime uploadedAt) {
        this(null, caseId, originalFileName, storedFilePath, originalSha256, null, status,
                uploadedByUserId, uploadedAt, null, null);
    }

    public Evidence(Integer evidenceId, Integer caseId, String originalFileName, String storedFilePath,
                    String originalSha256, String recalculatedSha256, EvidenceStatus status,
                    Integer uploadedByUserId, LocalDateTime uploadedAt,
                    Integer lastVerifiedByUserId, LocalDateTime lastVerifiedAt) {
        this.evidenceId = evidenceId;
        this.caseId = caseId;
        this.originalFileName = originalFileName;
        this.storedFilePath = storedFilePath;
        this.originalSha256 = originalSha256;
        this.recalculatedSha256 = recalculatedSha256;
        this.status = status;
        this.uploadedByUserId = uploadedByUserId;
        this.uploadedAt = uploadedAt;
        this.lastVerifiedByUserId = lastVerifiedByUserId;
        this.lastVerifiedAt = lastVerifiedAt;
    }

    public Integer getEvidenceId() {
        return evidenceId;
    }

    public void setEvidenceId(Integer evidenceId) {
        this.evidenceId = evidenceId;
    }

    public Integer getCaseId() {
        return caseId;
    }

    public void setCaseId(Integer caseId) {
        this.caseId = caseId;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getStoredFilePath() {
        return storedFilePath;
    }

    public void setStoredFilePath(String storedFilePath) {
        this.storedFilePath = storedFilePath;
    }

    public String getOriginalSha256() {
        return originalSha256;
    }

    public void setOriginalSha256(String originalSha256) {
        this.originalSha256 = originalSha256;
    }

    public String getRecalculatedSha256() {
        return recalculatedSha256;
    }

    public EvidenceStatus getStatus() {
        return status;
    }

    public void markStatus(EvidenceStatus status) {
        this.status = status;
    }

    public Integer getUploadedByUserId() {
        return uploadedByUserId;
    }

    public void setUploadedByUserId(Integer uploadedByUserId) {
        this.uploadedByUserId = uploadedByUserId;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public Integer getLastVerifiedByUserId() {
        return lastVerifiedByUserId;
    }

    public LocalDateTime getLastVerifiedAt() {
        return lastVerifiedAt;
    }

    public void recordVerification(String recalculatedSha256, Integer verifiedByUserId, LocalDateTime verifiedAt) {
        this.recalculatedSha256 = recalculatedSha256;
        this.lastVerifiedByUserId = verifiedByUserId;
        this.lastVerifiedAt = verifiedAt;
    }

    public boolean hasVerificationSnapshot() {
        return recalculatedSha256 != null && !recalculatedSha256.isBlank();
    }

    public boolean hashesMatch() {
        return originalSha256 != null
                && recalculatedSha256 != null
                && originalSha256.equalsIgnoreCase(recalculatedSha256);
    }
}
