package com.project.service;

import com.project.model.CaseState;
import com.project.model.SeverityLevel;
import com.project.model.SummaryReportCaseRecord;
import com.project.model.SummaryReportRequest;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable report snapshot containing metrics, distributions, and matching cases.
 */
public record SummaryReportResult(SummaryReportRequest request, String generatedByName, int generatedByUserId,
                                  LocalDateTime generatedAt, List<SummaryReportCaseRecord> matchingCases,
                                  Map<CaseState, Long> statusCounts, Map<SeverityLevel, Long> severityCounts,
                                  long matchedCaseCount, long slaCompliantCount, long slaBreachedCount,
                                  long frozenCaseCount, long tamperedCaseCount) {
    public SummaryReportResult {
        // Defensive copies keep exported reports stable even if the caller later changes source collections.
        matchingCases = List.copyOf(matchingCases);
        statusCounts = Collections.unmodifiableMap(new EnumMap<>(statusCounts));
        severityCounts = Collections.unmodifiableMap(new EnumMap<>(severityCounts));
    }
}
