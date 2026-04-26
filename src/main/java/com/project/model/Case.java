package com.project.model;

import java.time.LocalDateTime;

/**
 * Represents a case in the system.
 * Contains case details, assignment info, severity, priority, and lifecycle timestamps.
 */
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
    private LocalDateTime closedAt;

    public Case(String title, String description, SeverityLevel severity, String relatedInfo) {
        this(null, title, description, relatedInfo, severity, null, null, CaseState.CASE_CREATED, null, null, null, null);
    }

    public Case(Integer caseId, String title, String description, String relatedInfo, SeverityLevel severity,
                Integer slaHours, PriorityState priorityState, CaseState status,
                Integer assignedOfficerId, String assignedOfficerName, LocalDateTime createdAt) {
        this(caseId, title, description, relatedInfo, severity, slaHours, priorityState, status,
                assignedOfficerId, assignedOfficerName, createdAt, null);
    }

    public Case(Integer caseId, String title, String description, String relatedInfo, SeverityLevel severity,
                Integer slaHours, PriorityState priorityState, CaseState status,
                Integer assignedOfficerId, String assignedOfficerName, LocalDateTime createdAt,
                LocalDateTime closedAt) {
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
        this.closedAt = closedAt;
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

    public void validateVerifiedState() {
        validateActiveState();

        if (status != CaseState.FORENSIC_REVIEW) {
            throw new IllegalStateException("Only cases in FORENSIC_REVIEW with verified evidence can be submitted for supervisor review.");
        }
    }

    public void validateSupervisorReviewState() {
        validateActiveState();

        if (status != CaseState.SUPERVISOR_REVIEW) {
            throw new IllegalStateException("This action requires the case to be in SUPERVISOR_REVIEW.");
        }
    }

    public void validateFreezableState() {
        if (status == CaseState.CLOSED) {
            throw new IllegalStateException("Closed cases cannot be frozen.");
        }

        if (status == CaseState.FROZEN) {
            throw new IllegalStateException("Case is already in FROZEN state.");
        }

        if (status != CaseState.EVIDENCE_UPLOADED
                && status != CaseState.FORENSIC_REVIEW
                && status != CaseState.SUPERVISOR_REVIEW) {
            throw new IllegalStateException("Only cases in evidence or review workflow can be frozen.");
        }
    }

    public void validateFrozenState() {
        if (status != CaseState.FROZEN) {
            throw new IllegalStateException("This action requires the case to be in FROZEN state.");
        }
    }

    public void validateEscalatedReviewState(LocalDateTime referenceTime) {
        if (status == CaseState.CLOSED) {
            throw new IllegalStateException("Closed cases cannot be reviewed as escalated cases.");
        }

        if (priorityState != PriorityState.ESCALATED) {
            throw new IllegalStateException("This action requires the case priority to be ESCALATED.");
        }

        if (!hasBreachedSla(referenceTime)) {
            throw new IllegalStateException("This case has not breached its SLA and cannot enter escalated review.");
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

    public LocalDateTime getSlaDueAt() {
        if (createdAt == null || slaHours == null) {
            return null;
        }

        return createdAt.plusHours(slaHours);
    }

    public boolean hasBreachedSla(LocalDateTime referenceTime) {
        LocalDateTime dueAt = getSlaDueAt();
        return dueAt != null && referenceTime != null && referenceTime.isAfter(dueAt);
    }

    public void moveToState(CaseState nextState) {
        this.status = nextState;
    }

    public void triggerFreezeWorkflow() {
        this.status = CaseState.FROZEN;
    }

    public void reopenToSupervisorReview() {
        this.status = CaseState.SUPERVISOR_REVIEW;
    }

    public void closeCase() {
        this.status = CaseState.CLOSED;
    }

    public void returnToForensicReview() {
        this.status = CaseState.FORENSIC_REVIEW;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }
}
