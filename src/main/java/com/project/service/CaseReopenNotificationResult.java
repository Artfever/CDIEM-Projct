package com.project.service;

/**
 * Tracks which users were notified when a frozen case was reopened.
 */
public record CaseReopenNotificationResult(boolean investigatingOfficerNotified, boolean forensicAnalystNotified) {
}
