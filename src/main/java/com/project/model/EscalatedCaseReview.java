package com.project.model;

import java.time.LocalDateTime;

/**
 * Represents a supervisor's review of an escalated case.
 * Tracks instructions given and priority state changes.
 */
public class EscalatedCaseReview {
    private final int reviewId;
    private final int caseId;
    private final String instructions;
    private final PriorityState previousPriorityState;
    private final PriorityState resultingPriorityState;
    private final int reviewedByUserId;
    private final String reviewedByName;
    private final LocalDateTime reviewedAt;

    public EscalatedCaseReview(int reviewId, int caseId, String instructions, PriorityState previousPriorityState,
                               PriorityState resultingPriorityState, int reviewedByUserId, String reviewedByName,
                               LocalDateTime reviewedAt) {
        this.reviewId = reviewId;
        this.caseId = caseId;
        this.instructions = instructions;
        this.previousPriorityState = previousPriorityState;
        this.resultingPriorityState = resultingPriorityState;
        this.reviewedByUserId = reviewedByUserId;
        this.reviewedByName = reviewedByName;
        this.reviewedAt = reviewedAt;
    }

    public int getReviewId() {
        return reviewId;
    }

    public int getCaseId() {
        return caseId;
    }

    public String getInstructions() {
        return instructions;
    }

    public PriorityState getPreviousPriorityState() {
        return previousPriorityState;
    }

    public PriorityState getResultingPriorityState() {
        return resultingPriorityState;
    }

    public int getReviewedByUserId() {
        return reviewedByUserId;
    }

    public String getReviewedByName() {
        return reviewedByName;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }
}
