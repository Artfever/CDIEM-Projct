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
    private Integer createdByUserId;
    private Integer assignedOfficerId;
    private LocalDateTime createdAt;

    public Case(String title, String description, SeverityLevel severity, String relatedInfo) {
        this(null, title, description, relatedInfo, severity, null, null, CaseState.CASE_CREATED, null, null, null);
    }

    public Case(Integer caseId, String title, String description, String relatedInfo, SeverityLevel severity,
                Integer slaHours, PriorityState priorityState, CaseState status,
                Integer createdByUserId, Integer assignedOfficerId, LocalDateTime createdAt) {
        this.caseId = caseId;
        this.title = title;
        this.description = description;
        this.relatedInfo = relatedInfo;
        this.severity = severity;
        this.slaHours = slaHours;
        this.priorityState = priorityState;
        this.status = status;
        this.createdByUserId = createdByUserId;
        this.assignedOfficerId = assignedOfficerId;
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

    public Integer getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(Integer createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public Integer getAssignedOfficerId() {
        return assignedOfficerId;
    }

    public void setAssignedOfficerId(Integer assignedOfficerId) {
        this.assignedOfficerId = assignedOfficerId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
