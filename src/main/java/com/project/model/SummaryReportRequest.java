package com.project.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Request parameters for generating a summary report.
 * Contains date range, case filter, and priority filter.
 */
public record SummaryReportRequest(LocalDate fromDate, LocalDate toDate, SummaryReportCaseFilter caseFilter,
                                   PriorityState priorityState) {
    public SummaryReportRequest {
        if (caseFilter == null) {
            caseFilter = SummaryReportCaseFilter.ALL;
        }
    }

    public LocalDateTime fromDateTime() {
        return fromDate == null ? null : fromDate.atStartOfDay();
    }

    public LocalDateTime toDateTimeExclusive() {
        return toDate == null ? null : toDate.plusDays(1).atStartOfDay();
    }
}
