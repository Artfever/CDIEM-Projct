package com.project.service;

/**
 * Result returned after a case changes investigating officers.
 */
public record CaseReassignmentResult(String previousOfficerName, String newOfficerName,
                                     boolean outgoingOfficerNotified, boolean incomingOfficerNotified) {
}
