package com.project.service;

import com.project.model.PriorityState;
import com.project.model.SeverityLevel;

public class SLAManager {
    private static final int LOW_SLA_HOURS = 120;
    private static final int MEDIUM_SLA_HOURS = 72;
    private static final int HIGH_SLA_HOURS = 24;
    private static final int CRITICAL_SLA_HOURS = 8;

    public int reEvaluateThreshold(SeverityLevel severityLevel) {
        return switch (severityLevel) {
            case LOW -> LOW_SLA_HOURS;
            case MEDIUM -> MEDIUM_SLA_HOURS;
            case HIGH -> HIGH_SLA_HOURS;
            case CRITICAL -> CRITICAL_SLA_HOURS;
        };
    }

    public PriorityState transitionPriorityState(SeverityLevel severityLevel) {
        return switch (severityLevel) {
            case LOW, MEDIUM -> PriorityState.STANDARD;
            case HIGH -> PriorityState.PRIORITY;
            case CRITICAL -> PriorityState.ESCALATED;
        };
    }
}
