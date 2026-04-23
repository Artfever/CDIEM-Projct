package com.project.service;

import com.project.model.PriorityState;

public record EscalatedCaseReviewResult(PriorityState priorityState, String instructions) {
}
