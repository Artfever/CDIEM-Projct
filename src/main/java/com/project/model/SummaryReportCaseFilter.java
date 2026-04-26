package com.project.model;

/**
 * Filter options for summary reports.
 * Determines which cases to include in the report.
 */
public enum SummaryReportCaseFilter {
    ALL("All Matching Cases"),
    ACTIVE("Active Cases"),
    VERIFIED("Verified Evidence Cases"),
    FROZEN("Frozen Cases"),
    CLOSED("Closed Cases");

    private final String displayName;

    SummaryReportCaseFilter(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
