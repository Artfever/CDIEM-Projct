package com.project.model;

import java.time.LocalDateTime;

public class Case {
    private Integer caseId;
    private String title;
    private String description;
    private String relatedInfo;
    private SeverityLevel severity;
    private Integer slaHours;
    private PriorityState priorityState;
    private CaseState status;
    private Integer assignedOfficerId;
    private String assignedOfficerName;
    private LocalDateTime createdAt;

    public Case(String title, String description, SeverityLevel severity, String relatedInfo) {
        this(null, title, description, relatedInfo, severity, null, null, CaseState.CASE_CREATED, null, null, null);
    }

    public Case(Integer caseId, String title, String description, String relatedInfo, SeverityLevel severity,
                Integer slaHours, PriorityState priorityState, CaseState status,
                Integer assignedOfficerId, String assignedOfficerName, LocalDateTime createdAt) {
        this.caseId = caseId;
        this.title = title;
        this.description = description;
        this.relatedInfo = relatedInfo;
        this.severity = severity;
        this.slaHours = slaHours;
        this.priorityState = priorityState;
        this.status = status;
        this.assignedOfficerId = assignedOfficerId;
        this.assignedOfficerName = assignedOfficerName;
        this.createdAt = createdAt;
    }

    public Integer getCaseId() {
        return caseId;
    }

    public void setCaseId(Integer caseId) {
        this.caseId = caseId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRelatedInfo() {
        return relatedInfo;
    }

    public void setRelatedInfo(String relatedInfo) {
        this.relatedInfo = relatedInfo;
    }

    public SeverityLevel getSeverity() {
        return severity;
    }

    public void setSeverity(SeverityLevel severity) {
        this.severity = severity;
    }

    public Integer getSlaHours() {
        return slaHours;
    }

    public void setSlaHours(Integer slaHours) {
        this.slaHours = slaHours;
    }

    public PriorityState getPriorityState() {
        return priorityState;
    }

    public void setPriorityState(PriorityState priorityState) {
        this.priorityState = priorityState;
    }

    public CaseState getStatus() {
        return status;
    }

    public void setStatus(CaseState status) {
        this.status = status;
    }

    public void validateActiveState() {
        if (status == CaseState.CLOSED) {
            throw new IllegalStateException("Closed cases cannot be modified.");
        }

        if (status == CaseState.FROZEN) {
            throw new IllegalStateException("Frozen cases cannot be changed from the Manage Case module.");
        }
    }

    public void validateReassignableState() {
        validateActiveState();
    }

    public void validateEvidenceUploadState() {
        validateActiveState();

        if (status != CaseState.CASE_CREATED
                && status != CaseState.EVIDENCE_UPLOADED
                && status != CaseState.CASE_REASSIGNED) {
            throw new IllegalStateException("Evidence can only be uploaded while the case is awaiting evidence intake.");
        }
    }

    public void validateEvidenceVerificationState() {
        validateActiveState();

        if (status != CaseState.EVIDENCE_UPLOADED && status != CaseState.FORENSIC_REVIEW) {
            throw new IllegalStateException("Evidence integrity can only be verified after an upload is recorded.");
        }
    }

    public void validateForensicReviewState() {
        validateActiveState();

        if (status != CaseState.FORENSIC_REVIEW) {
            throw new IllegalStateException("This action requires the case to be in FORENSIC_REVIEW.");
        }
    }

    public Integer getAssignedOfficerId() {
        return assignedOfficerId;
    }

    public void setAssignedOfficerId(Integer assignedOfficerId) {
        this.assignedOfficerId = assignedOfficerId;
    }

    public String getAssignedOfficerName() {
        return assignedOfficerName;
    }

    public void setAssignedOfficerName(String assignedOfficerName) {
        this.assignedOfficerName = assignedOfficerName;
    }

    public void updateAssignedOfficer(Integer officerId, String officerName) {
        this.assignedOfficerId = officerId;
        this.assignedOfficerName = officerName;
    }

    public void transitionPriorityState(PriorityState priorityState) {
        this.priorityState = priorityState;
    }

    public void moveToState(CaseState nextState) {
        this.status = nextState;
    }

    public void triggerFreezeWorkflow() {
        this.status = CaseState.FROZEN;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
