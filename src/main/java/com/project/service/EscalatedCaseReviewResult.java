package com.project.service;

import com.project.model.PriorityState;

/**
 * Result returned after a supervisor reviews an escalated case.
 */
public record EscalatedCaseReviewResult(PriorityState priorityState, String instructions) {
}
