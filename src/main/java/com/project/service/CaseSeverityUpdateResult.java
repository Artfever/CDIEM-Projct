package com.project.service;

import com.project.model.PriorityState;
import com.project.model.SeverityLevel;

/**
 * Recalculated severity profile after a supervisor updates a case.
 */
public record CaseSeverityUpdateResult(SeverityLevel severity, int slaHours, PriorityState priorityState) {
}
