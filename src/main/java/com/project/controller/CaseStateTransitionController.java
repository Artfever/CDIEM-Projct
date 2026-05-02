package com.project.controller;

import com.project.model.CaseState;
import com.project.model.Evidence;
import com.project.model.User;
import com.project.model.UserRole;
import com.project.service.CaseFreezeResult;
import com.project.service.CaseReopenResult;
import com.project.service.CaseStateTransitionService;
import com.project.service.EvidenceSnapshot;
import com.project.util.AppNavigator;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Main screen for pause-and-resume workflow changes.
 * Analysts freeze active cases here, and supervisors reopen frozen ones.
 */
public class CaseStateTransitionController {
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
    private VBox freezePanel;

    @FXML
    private Button freezeCaseButton;

    @FXML
    private VBox reopenPanel;

    @FXML
    private TextArea reopenReasonArea;

    @FXML
    private Button reopenCaseButton;

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

    private final CaseStateTransitionService transitionService = new CaseStateTransitionService();
    private User currentUser;
    private EvidenceSnapshot currentSnapshot;

    @FXML
    public void initialize() {
        if (reopenReasonArea != null) {
            reopenReasonArea.textProperty().addListener((observable, oldValue, newValue) -> updateActionAvailability());
        }

        resetSnapshot();
        setStatus("Manage Case State Transitions is ready.", STATUS_NEUTRAL);
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
            // This view loads both the case state and the latest evidence state before showing allowed actions.
            int caseId = parseRequiredInteger(caseIdField.getText(), "Case ID");
            currentSnapshot = transitionService.getTransitionSnapshot(caseId);
            renderSnapshot(currentSnapshot);
            updateActionAvailability();
            setStatus("Case state snapshot loaded.", STATUS_NEUTRAL);
        } catch (Exception e) {
            setStatus(getRootMessage(e), STATUS_ERROR);
        }
    }

    @FXML
    public void freezeCase() {
        try {
            ensureRole(UserRole.ANALYST, "Only a Digital Forensic Analyst can freeze a case.");

            int caseId = parseRequiredInteger(caseIdField.getText(), "Case ID");
            // Freeze is the analyst's way to halt normal handling without deleting case history.
            CaseFreezeResult result = transitionService.freezeCase(caseId, currentUser.getUserId());
            currentSnapshot = transitionService.getTransitionSnapshot(caseId);
            renderSnapshot(currentSnapshot);
            updateActionAvailability();
            setStatus("Case frozen successfully. State is now " + formatCaseState(result.caseState()) + ".", STATUS_SUCCESS);
        } catch (Exception e) {
            setStatus(getRootMessage(e), STATUS_ERROR);
        }
    }

    @FXML
    public void reopenCase() {
        try {
            ensureRole(UserRole.SUPERVISOR, "Only the Supervisory Authority can reopen a case.");

            int caseId = parseRequiredInteger(caseIdField.getText(), "Case ID");
            // Reopen puts a frozen case back into supervised handling and clears a tampered evidence lock if present.
            CaseReopenResult result = transitionService.reopenCase(caseId, reopenReasonArea.getText(), currentUser.getUserId());
            currentSnapshot = transitionService.getTransitionSnapshot(caseId);
            renderSnapshot(currentSnapshot);
            reopenReasonArea.clear();
            updateActionAvailability();
            setStatus(buildReopenStatus(result), STATUS_SUCCESS);
        } catch (Exception e) {
            setStatus(getRootMessage(e), STATUS_ERROR);
        }
    }

    private void applyCurrentUserContext() {
        if (currentUser == null || roleLabel == null) {
            return;
        }

        boolean analystSignedIn = currentUser.getRole() == UserRole.ANALYST;
        boolean supervisorSignedIn = currentUser.getRole() == UserRole.SUPERVISOR;

        roleLabel.setText(currentUser.getRole().getDisplayName());
        identityLabel.setText(currentUser.getName() + " | " + buildUsername(currentUser) + " | ID " + currentUser.getUserId());
        subtitleLabel.setText(analystSignedIn
                ? "Freeze active review cases when forensic handling must be restricted."
                : "Reopen frozen cases, send workflow notifications, and return them to Supervisor Review.");
        accessSummaryLabel.setText(analystSignedIn
                ? "Analysts can freeze cases in evidence or review workflow. Tampered evidence can still freeze a case automatically from the Manage Evidence module."
                : "Supervisory Authority users can reopen only frozen cases. Reopen records the stated reason, restores the case to Supervisor Review, resets tampered evidence to Uploaded when needed, and notifies the assigned officer plus the last analyst who froze or tampered the case.");

        setNodeVisibility(freezePanel, analystSignedIn);
        setNodeVisibility(reopenPanel, supervisorSignedIn);

        if (analystSignedIn) {
            setStatus("Signed in as Digital Forensic Analyst. Freeze actions are available.", STATUS_NEUTRAL);
        } else if (supervisorSignedIn) {
            setStatus("Signed in as Supervisory Authority. Reopen actions are available.", STATUS_NEUTRAL);
        } else {
            setStatus("This module is available only to Digital Forensic Analysts and Supervisory Authority users.", STATUS_ERROR);
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
            integritySummaryValueLabel.setText("Integrity review has not started.");
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
        integritySummaryValueLabel.setText("Integrity review has not started.");
    }

    private String buildIntegritySummary(Evidence evidence) {
        if (!evidence.hasVerificationSnapshot()) {
            return "Integrity verification pending.";
        }

        return evidence.hashesMatch()
                ? "Stored and recalculated hashes match."
                : "Stored and recalculated hashes do not match.";
    }

    private void updateActionAvailability() {
        boolean analystSignedIn = currentUser != null && currentUser.getRole() == UserRole.ANALYST;
        boolean supervisorSignedIn = currentUser != null && currentUser.getRole() == UserRole.SUPERVISOR;
        boolean snapshotLoaded = currentSnapshot != null;
        boolean caseFreezable = snapshotLoaded && isFreezable(currentSnapshot.caseRecord().getStatus());
        boolean caseFrozen = snapshotLoaded && currentSnapshot.caseRecord().getStatus() == CaseState.FROZEN;
        boolean reasonProvided = reopenReasonArea != null && clean(reopenReasonArea.getText()) != null;

        if (loadSnapshotButton != null) {
            loadSnapshotButton.setDisable(currentUser == null);
        }
        if (freezeCaseButton != null) {
            freezeCaseButton.setDisable(!analystSignedIn || !caseFreezable);
        }
        if (reopenCaseButton != null) {
            reopenCaseButton.setDisable(!supervisorSignedIn || !caseFrozen || !reasonProvided);
        }
    }

    private boolean isFreezable(CaseState caseState) {
        return caseState == CaseState.EVIDENCE_UPLOADED
                || caseState == CaseState.FORENSIC_REVIEW
                || caseState == CaseState.SUPERVISOR_REVIEW;
    }

    private String buildReopenStatus(CaseReopenResult result) {
        String resetMessage = result.tamperedEvidenceResetToDefault()
                ? ". Tampered evidence was reset to Uploaded"
                : "";

        if (!result.forensicAnalystNotified()) {
            return "Case reopened to " + formatCaseState(result.caseState()) + resetMessage
                    + ". Assigned investigating officer notified, but no analyst recipient was resolved from the custody log.";
        }

        return "Case reopened to " + formatCaseState(result.caseState()) + resetMessage
                + ". Assigned investigating officer and " + result.forensicAnalystName()
                + " were notified.";
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

    private void setNodeVisibility(Node node, boolean visible) {
        if (node == null) {
            return;
        }

        node.setVisible(visible);
        node.setManaged(visible);
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
