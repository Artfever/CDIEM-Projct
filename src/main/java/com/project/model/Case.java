package com.project.model;

public class Case {
    private final String title;
    private final String severity;

    public Case(String title, String severity) {
        this.title = title;
        this.severity = severity;
    }

    public String getTitle() {
        return title;
    }

    public String getSeverity() {
        return severity;
    }
}
