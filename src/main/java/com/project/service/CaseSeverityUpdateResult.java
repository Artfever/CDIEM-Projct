package com.project.service;

import com.project.model.PriorityState;
import com.project.model.SeverityLevel;

public record CaseSeverityUpdateResult(SeverityLevel severity, int slaHours, PriorityState priorityState) {
}
