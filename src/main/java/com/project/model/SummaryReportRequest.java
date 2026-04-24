package com.project.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
