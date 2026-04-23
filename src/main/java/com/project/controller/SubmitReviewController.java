package com.project.controller;

import com.project.model.CaseState;
import com.project.model.Evidence;
import com.project.model.EvidenceStatus;
import com.project.model.User;
import com.project.model.UserRole;
import com.project.service.CaseSubmissionResult;
import com.project.service.CaseSubmissionService;
import com.project.service.EvidenceSnapshot;
import com.project.util.AppNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SubmitReviewController {
    private static final String STATUS_NEUTRAL = "status-neutral";
    private static final String STATUS_SUCCESS = "status-success";
    private static final String STATUS_ERROR = "status-error";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy | hh:mm a");

    @FXML
    private Label subtitleLabel;

    @FXML
    private Label roleLabel;

    @FXML
    private Label identityLabel;

    @FXML
    private Label accessSummaryLabel;

    @FXML
    private TextField caseIdField;

    @FXML
    private Button loadSnapshotButton;

    @FXML
    private Button submitReviewButton;

    @FXML
    private Label caseStatusValueLabel;

    @FXML
    private Label assignedOfficerValueLabel;

    @FXML
    private Label severityValueLabel;

    @FXML
    private Label priorityValueLabel;

    @FXML
    private Label createdAtValueLabel;

    @FXML
    private Label evidenceStatusValueLabel;

    @FXML
    private Label fileNameValueLabel;

    @FXML
    private Label integritySummaryValueLabel;

    @FXML
    private Label statusLabel;

    private final CaseSubmissionService submissionService = new CaseSubmissionService();
    private User currentUser;
    private EvidenceSnapshot currentSnapshot;

    @FXML
    public void initialize() {
        resetSnapshot();
        setStatus("Submit Case for Supervisor Review is ready.", STATUS_NEUTRAL);
        updateActionAvailability();
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
        applyCurrentUserContext();
    }

    @FXML
    public void goBackToDashboard() {
        if (currentUser == null) {
            setStatus("No authenticated session was found for dashboard navigation.", STATUS_ERROR);
            return;
        }

        try {
            AppNavigator.showDashboard(currentUser);
        } catch (Exception e) {
            setStatus(getRootMessage(e), STATUS_ERROR);
        }
    }

    @FXML
    public void loadCaseSnapshot() {
        try {
            int caseId = parseRequiredInteger(caseIdField.getText(), "Case ID");
            currentSnapshot = submissionService.getSubmissionSnapshot(caseId);
            renderSnapshot(currentSnapshot);
            updateActionAvailability();
            setStatus("Case workflow snapshot loaded.", STATUS_NEUTRAL);
        } catch (Exception e) {
            setStatus(getRootMessage(e), STATUS_ERROR);
        }
    }

    @FXML
    public void submitForSupervisorReview() {
        try {
            ensureRole(UserRole.OFFICER, "Only an Investigating Officer can submit a case for supervisor review.");

            int caseId = parseRequiredInteger(caseIdField.getText(), "Case ID");
            CaseSubmissionResult result = submissionService.submitForSupervisorReview(caseId, currentUser.getUserId());
            currentSnapshot = submissionService.getSubmissionSnapshot(caseId);
            renderSnapshot(currentSnapshot);
            updateActionAvailability();
            setStatus(buildSubmissionStatus(result), STATUS_SUCCESS);
        } catch (Exception e) {
            setStatus(getRootMessage(e), STATUS_ERROR);
        }
    }

    private void applyCurrentUserContext() {
        if (currentUser == null || roleLabel == null) {
            return;
        }

        boolean officerSignedIn = currentUser.getRole() == UserRole.OFFICER;
        roleLabel.setText(currentUser.getRole().getDisplayName());
        identityLabel.setText(currentUser.getName() + " | " + buildUsername(currentUser) + " | ID " + currentUser.getUserId());
        subtitleLabel.setText(officerSignedIn
                ? "Submit completed investigations with verified evidence for supervisory review."
                : "This module is reserved for the assigned Investigating Officer.");
        accessSummaryLabel.setText(officerSignedIn
                ? "Only the assigned Investigating Officer can submit a case. The case must still be in FORENSIC_REVIEW and its latest evidence item must already be marked VERIFIED. Submission records a chain-of-custody entry, moves the case to SUPERVISOR_REVIEW, and notifies supervisory authority users."
                : "You can load case context here, but only Investigating Officers can complete the supervisor-review submission workflow.");

        if (officerSignedIn) {
            setStatus("Signed in as Investigating Officer. Supervisor review submission is available.", STATUS_NEUTRAL);
        } else {
            setStatus("This module is available only to Investigating Officers.", STATUS_ERROR);
        }

        updateActionAvailability();
    }

    private void renderSnapshot(EvidenceSnapshot snapshot) {
        caseStatusValueLabel.setText(formatCaseState(snapshot.caseRecord().getStatus()));
        assignedOfficerValueLabel.setText(defaultText(snapshot.caseRecord().getAssignedOfficerName(), "Unassigned"));
        severityValueLabel.setText(formatEnumName(snapshot.caseRecord().getSeverity() == null
                ? null
                : snapshot.caseRecord().getSeverity().name()));
        priorityValueLabel.setText(formatEnumName(snapshot.caseRecord().getPriorityState() == null
                ? null
                : snapshot.caseRecord().getPriorityState().name()));
        createdAtValueLabel.setText(formatTimestamp(snapshot.caseRecord().getCreatedAt()));

        Evidence evidence = snapshot.evidence();
        if (evidence == null) {
            evidenceStatusValueLabel.setText("No evidence uploaded");
            fileNameValueLabel.setText("No evidence file recorded");
            integritySummaryValueLabel.setText("Submission is blocked until verified evidence exists.");
            return;
        }

        evidenceStatusValueLabel.setText(evidence.getStatus().getDisplayName());
        fileNameValueLabel.setText(defaultText(evidence.getOriginalFileName(), "Unnamed evidence"));
        integritySummaryValueLabel.setText(buildIntegritySummary(evidence));
    }

    private void resetSnapshot() {
        caseStatusValueLabel.setText("No case loaded");
        assignedOfficerValueLabel.setText("Unavailable");
        severityValueLabel.setText("Unavailable");
        priorityValueLabel.setText("Unavailable");
        createdAtValueLabel.setText("Unavailable");
        evidenceStatusValueLabel.setText("No evidence loaded");
        fileNameValueLabel.setText("No evidence file recorded");
        integritySummaryValueLabel.setText("Submission is blocked until verified evidence exists.");
    }

    private String buildIntegritySummary(Evidence evidence) {
        if (evidence.getStatus() == EvidenceStatus.VERIFIED) {
            return "Evidence marked as verified and ready for supervisor submission.";
        }

        if (evidence.getStatus() == EvidenceStatus.TAMPERED) {
            return "Evidence marked as tampered. Submission is blocked.";
        }

        if (!evidence.hasVerificationSnapshot()) {
            return "Integrity verification pending.";
        }

        return evidence.hashesMatch()
                ? "Hashes match, but the analyst has not finalized the evidence as VERIFIED yet."
                : "Hash mismatch detected. Analyst action is still required.";
    }

    private void updateActionAvailability() {
        boolean officerSignedIn = currentUser != null && currentUser.getRole() == UserRole.OFFICER;
        boolean submittable = officerSignedIn && currentSnapshot != null && isSubmittable(currentSnapshot);

        if (loadSnapshotButton != null) {
            loadSnapshotButton.setDisable(currentUser == null);
        }
        if (submitReviewButton != null) {
            submitReviewButton.setDisable(!submittable);
        }
    }

    private boolean isSubmittable(EvidenceSnapshot snapshot) {
        return snapshot.caseRecord().getStatus() == CaseState.FORENSIC_REVIEW
                && snapshot.caseRecord().getAssignedOfficerId() != null
                && currentUser != null
                && snapshot.caseRecord().getAssignedOfficerId() == currentUser.getUserId()
                && snapshot.hasEvidence()
                && snapshot.evidence().getStatus() == EvidenceStatus.VERIFIED;
    }

    private String buildSubmissionStatus(CaseSubmissionResult result) {
        if (result.notifiedSupervisorCount() == 0) {
            return "Case submitted successfully. State is now " + formatCaseState(result.caseState())
                    + ", but no supervisory authority users were available to notify.";
        }

        if (result.notifiedSupervisorCount() == 1) {
            return "Case submitted successfully. State is now " + formatCaseState(result.caseState())
                    + ". 1 supervisory authority user was notified.";
        }

        return "Case submitted successfully. State is now " + formatCaseState(result.caseState())
                + ". " + result.notifiedSupervisorCount() + " supervisory authority users were notified.";
    }

    private void ensureRole(UserRole expectedRole, String message) {
        if (currentUser == null || currentUser.getRole() != expectedRole) {
            throw new IllegalStateException(message);
        }
    }

    private void setStatus(String message, String stateClass) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll(STATUS_NEUTRAL, STATUS_SUCCESS, STATUS_ERROR);
        statusLabel.getStyleClass().add(stateClass);
    }

    private int parseRequiredInteger(String value, String fieldName) {
        String cleaned = clean(value);
        if (cleaned == null) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }

        try {
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a number.");
        }
    }

    private String buildUsername(User user) {
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            return "n/a";
        }

        return "@" + user.getUsername();
    }

    private String formatCaseState(CaseState caseState) {
        return formatEnumName(caseState == null ? null : caseState.name());
    }

    private String formatEnumName(String value) {
        if (value == null || value.isBlank()) {
            return "Unavailable";
        }

        String[] tokens = value.toLowerCase().split("_");
        StringBuilder builder = new StringBuilder();
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append(' ');
            }

            builder.append(Character.toUpperCase(token.charAt(0)));
            if (token.length() > 1) {
                builder.append(token.substring(1));
            }
        }
        return builder.toString();
    }

    private String formatTimestamp(LocalDateTime value) {
        return value == null ? "Unavailable" : value.format(TIMESTAMP_FORMATTER);
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String getRootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }

        return current.getMessage() == null ? "Unknown error." : current.getMessage();
    }
}
