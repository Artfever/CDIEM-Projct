package com.project.service;

public record CaseReassignmentResult(String previousOfficerName, String newOfficerName,
                                     boolean outgoingOfficerNotified, boolean incomingOfficerNotified) {
}
