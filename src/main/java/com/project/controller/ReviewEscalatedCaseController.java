package com.project.controller;

import com.project.model.EscalatedCaseReview;
import com.project.model.PriorityState;
import com.project.model.User;
import com.project.model.UserRole;
import com.project.service.EscalatedCaseReviewResult;
import com.project.service.EscalatedCaseReviewService;
import com.project.service.EscalatedCaseSnapshot;
import com.project.util.AppNavigator;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ReviewEscalatedCaseController {
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
    private VBox reviewPanel;

    @FXML
    private TextArea instructionsArea;

    @FXML
    private Button recordReviewButton;

    @FXML
    private Label caseStatusValueLabel;

    @FXML
    private Label priorityValueLabel;

    @FXML
    private Label severityValueLabel;

    @FXML
    private Label assignedOfficerValueLabel;

    @FXML
    private Label createdAtValueLabel;

    @FXML
    private Label dueAtValueLabel;

    @FXML
    private Label breachStatusValueLabel;

    @FXML
    private Label latestReviewByValueLabel;

    @FXML
    private Label latestReviewTimeValueLabel;

    @FXML
    private Label latestInstructionsValueLabel;

    @FXML
    private Label statusLabel;

    private final EscalatedCaseReviewService escalatedCaseReviewService = new EscalatedCaseReviewService();
    private User currentUser;
    private EscalatedCaseSnapshot currentSnapshot;

    @FXML
    public void initialize() {
        if (instructionsArea != null) {
            instructionsArea.textProperty().addListener((observable, oldValue, newValue) -> updateAvailability());
        }

        resetSnapshot();
        setStatus("Review Escalated Case is ready.", STATUS_NEUTRAL);
        updateAvailability();
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
            ensureRole(UserRole.SUPERVISOR, "Only the Supervisory Authority can review escalated cases.");

            int caseId = parseRequiredInteger(caseIdField.getText(), "Case ID");
            currentSnapshot = escalatedCaseReviewService.getSnapshot(caseId);
            renderSnapshot(currentSnapshot);
            updateAvailability();
            setStatus("Escalated case snapshot loaded.", STATUS_NEUTRAL);
        } catch (Exception e) {
            setStatus(getRootMessage(e), STATUS_ERROR);
        }
    }

    @FXML
    public void recordReviewDecision() {
        try {
            ensureRole(UserRole.SUPERVISOR, "Only the Supervisory Authority can review escalated cases.");

            int caseId = parseRequiredInteger(caseIdField.getText(), "Case ID");
            EscalatedCaseReviewResult result = escalatedCaseReviewService.recordReviewDecision(
                    caseId,
                    instructionsArea.getText(),
                    currentUser.getUserId()
            );
            currentSnapshot = escalatedCaseReviewService.getSnapshot(caseId);
            renderSnapshot(currentSnapshot);
            instructionsArea.clear();
            updateAvailability();
            setStatus("Escalated review recorded. Priority is now " + result.priorityState().getDisplayName() + ".", STATUS_SUCCESS);
        } catch (Exception e) {
            setStatus(getRootMessage(e), STATUS_ERROR);
        }
    }

    private void applyCurrentUserContext() {
        if (currentUser == null || roleLabel == null) {
            return;
        }

        boolean supervisorSignedIn = currentUser.getRole() == UserRole.SUPERVISOR;
        roleLabel.setText(currentUser.getRole().getDisplayName());
        identityLabel.setText(currentUser.getName() + " | " + buildUsername(currentUser) + " | ID " + currentUser.getUserId());
        subtitleLabel.setText(supervisorSignedIn
                ? "Review breached escalated cases and assign corrective actions for active supervisory handling."
                : "This module is reserved for the Supervisory Authority.");
        accessSummaryLabel.setText(supervisorSignedIn
                ? "Supervisory Authority users can act only on cases whose priority is ESCALATED and whose SLA deadline has already been breached. Recording a review stores the instructions and moves priority to UNDER_ACTIVE_REVIEW."
                : "Only Supervisory Authority users can review escalated cases.");

        setNodeVisibility(reviewPanel, supervisorSignedIn);

        if (supervisorSignedIn) {
            setStatus("Signed in as Supervisory Authority. Escalated case review is available.", STATUS_NEUTRAL);
        } else {
            setStatus("This module is available only to Supervisory Authority users.", STATUS_ERROR);
        }

        updateAvailability();
    }

    private void renderSnapshot(EscalatedCaseSnapshot snapshot) {
        caseStatusValueLabel.setText(formatEnumName(snapshot.caseRecord().getStatus().name()));
        priorityValueLabel.setText(snapshot.caseRecord().getPriorityState() == null
                ? "Unavailable"
                : snapshot.caseRecord().getPriorityState().getDisplayName());
        severityValueLabel.setText(formatEnumName(snapshot.caseRecord().getSeverity() == null
                ? null
                : snapshot.caseRecord().getSeverity().name()));
        assignedOfficerValueLabel.setText(defaultText(snapshot.caseRecord().getAssignedOfficerName(), "Unassigned"));
        createdAtValueLabel.setText(formatTimestamp(snapshot.caseRecord().getCreatedAt()));
        dueAtValueLabel.setText(formatTimestamp(snapshot.slaDueAt()));
        breachStatusValueLabel.setText(buildBreachSummary(snapshot));
        renderLatestReview(snapshot.latestReview());
    }

    private void renderLatestReview(EscalatedCaseReview latestReview) {
        if (latestReview == null) {
            latestReviewByValueLabel.setText("No review recorded");
            latestReviewTimeValueLabel.setText("Unavailable");
            latestInstructionsValueLabel.setText("No corrective instructions have been recorded for this case yet.");
            return;
        }

        latestReviewByValueLabel.setText(defaultText(latestReview.getReviewedByName(), "User ID " + latestReview.getReviewedByUserId()));
        latestReviewTimeValueLabel.setText(formatTimestamp(latestReview.getReviewedAt()));
        latestInstructionsValueLabel.setText(defaultText(latestReview.getInstructions(), "No instructions recorded."));
    }

    private void resetSnapshot() {
        caseStatusValueLabel.setText("No case loaded");
        priorityValueLabel.setText("Unavailable");
        severityValueLabel.setText("Unavailable");
        assignedOfficerValueLabel.setText("Unavailable");
        createdAtValueLabel.setText("Unavailable");
        dueAtValueLabel.setText("Unavailable");
        breachStatusValueLabel.setText("Load a case to evaluate its SLA status.");
        renderLatestReview(null);
    }

    private void updateAvailability() {
        boolean supervisorSignedIn = currentUser != null && currentUser.getRole() == UserRole.SUPERVISOR;
        boolean snapshotLoaded = currentSnapshot != null;
        boolean instructionsProvided = instructionsArea != null && clean(instructionsArea.getText()) != null;
        boolean eligibleForReview = snapshotLoaded
                && currentSnapshot.caseRecord().getPriorityState() == PriorityState.ESCALATED
                && currentSnapshot.slaBreached();

        if (loadSnapshotButton != null) {
            loadSnapshotButton.setDisable(!supervisorSignedIn);
        }
        if (recordReviewButton != null) {
            recordReviewButton.setDisable(!supervisorSignedIn || !eligibleForReview || !instructionsProvided);
        }
    }

    private String buildBreachSummary(EscalatedCaseSnapshot snapshot) {
        if (snapshot.slaDueAt() == null) {
            return "SLA deadline is unavailable for this case.";
        }

        if (!snapshot.slaBreached()) {
            return "SLA deadline has not been breached.";
        }

        return "Breached by " + formatDuration(snapshot.breachDuration()) + ".";
    }

    private String formatDuration(Duration duration) {
        if (duration == null || duration.isNegative() || duration.isZero()) {
            return "less than 1 minute";
        }

        long totalMinutes = duration.toMinutes();
        long days = totalMinutes / (60 * 24);
        long hours = (totalMinutes % (60 * 24)) / 60;
        long minutes = totalMinutes % 60;

        StringBuilder builder = new StringBuilder();
        if (days > 0) {
            builder.append(days).append(days == 1 ? " day" : " days");
        }
        if (hours > 0) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(hours).append(hours == 1 ? " hour" : " hours");
        }
        if (minutes > 0 && days == 0) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(minutes).append(minutes == 1 ? " minute" : " minutes");
        }
        return builder.toString();
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
