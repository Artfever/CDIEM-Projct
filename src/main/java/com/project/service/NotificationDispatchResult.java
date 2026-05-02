package com.project.service;

/**
 * Tracks delivery outcomes for reassignment notifications.
 */
public record NotificationDispatchResult(boolean outgoingOfficerNotified, boolean incomingOfficerNotified) {
}
