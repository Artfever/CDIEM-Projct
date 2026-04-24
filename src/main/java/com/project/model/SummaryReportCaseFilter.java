package com.project.model;

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
