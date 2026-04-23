package com.project.controller;

import com.project.model.AuditLog;
import com.project.model.CaseState;
import com.project.model.User;
import com.project.model.UserRole;
import com.project.service.ChainOfCustodySnapshot;
import com.project.service.ChainOfCustodyViewerService;
import com.project.util.AppNavigator;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogViewerController {
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
    private Button loadLogButton;

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
    private Label entryCountValueLabel;

    @FXML
    private Label inspectionModeValueLabel;

    @FXML
    private VBox logEntriesContainer;

    @FXML
    private Label statusLabel;

    private final ChainOfCustodyViewerService viewerService = new ChainOfCustodyViewerService();
    private User currentUser;
    private ChainOfCustodySnapshot currentSnapshot;

    @FXML
    public void initialize() {
        resetSnapshot();
        setStatus("View Chain-of-Custody Log is ready.", STATUS_NEUTRAL);
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
    public void loadLog() {
        try {
            ensureRole(UserRole.SUPERVISOR, "Only the Supervisory Authority can view chain-of-custody logs.");

            int caseId = parseRequiredInteger(caseIdField.getText(), "Case ID");
            currentSnapshot = viewerService.getSnapshot(caseId);
            renderSnapshot(currentSnapshot);
            updateAvailability();
            setStatus("Immutable chain-of-custody log loaded.", STATUS_SUCCESS);
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
                ? "Inspect the immutable action timeline for any case without modifying workflow state."
                : "This module is reserved for the Supervisory Authority.");
        accessSummaryLabel.setText(supervisorSignedIn
                ? "Supervisory Authority users can inspect all recorded case actions here. Frozen cases open in inspection mode only; the log remains read-only and immutable."
                : "Only Supervisory Authority users can access the chain-of-custody audit history.");

        if (supervisorSignedIn) {
            setStatus("Signed in as Supervisory Authority. Read-only log access is available.", STATUS_NEUTRAL);
        } else {
            setStatus("This module is available only to Supervisory Authority users.", STATUS_ERROR);
        }

        updateAvailability();
    }

    private void renderSnapshot(ChainOfCustodySnapshot snapshot) {
        caseStatusValueLabel.setText(formatCaseState(snapshot.caseRecord().getStatus()));
        priorityValueLabel.setText(snapshot.caseRecord().getPriorityState() == null
                ? "Unavailable"
                : snapshot.caseRecord().getPriorityState().getDisplayName());
        severityValueLabel.setText(formatEnumName(snapshot.caseRecord().getSeverity() == null
                ? null
                : snapshot.caseRecord().getSeverity().name()));
        assignedOfficerValueLabel.setText(defaultText(snapshot.caseRecord().getAssignedOfficerName(), "Unassigned"));
        createdAtValueLabel.setText(formatTimestamp(snapshot.caseRecord().getCreatedAt()));
        entryCountValueLabel.setText(String.valueOf(snapshot.entryCount()));
        inspectionModeValueLabel.setText(snapshot.inspectionMode() ? "Frozen Case Inspection" : "Standard Read Only");
        renderEntries(snapshot);
    }

    private void renderEntries(ChainOfCustodySnapshot snapshot) {
        logEntriesContainer.getChildren().clear();

        if (snapshot.auditEntries() == null || snapshot.auditEntries().isEmpty()) {
            Label emptyState = new Label("No audit entries were found for this case.");
            emptyState.getStyleClass().add("timeline-empty");
            emptyState.setWrapText(true);
            logEntriesContainer.getChildren().add(emptyState);
            return;
        }

        int index = 1;
        for (AuditLog entry : snapshot.auditEntries()) {
            VBox entryCard = new VBox(8.0);
            entryCard.getStyleClass().add("timeline-card");
            entryCard.setPadding(new Insets(16.0));

            HBox metaRow = new HBox(10.0);
            Label sequenceLabel = new Label("Entry " + index++);
            sequenceLabel.getStyleClass().add("timeline-pill");
            Label actorLabel = new Label(buildActorLabel(entry));
            actorLabel.getStyleClass().add("timeline-meta");
            Label timeLabel = new Label(formatTimestamp(entry.getTimestamp()));
            timeLabel.getStyleClass().add("timeline-meta");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            metaRow.getChildren().addAll(sequenceLabel, actorLabel, spacer, timeLabel);

            Label actionLabel = new Label(entry.getAction());
            actionLabel.getStyleClass().add("timeline-action");
            actionLabel.setWrapText(true);

            entryCard.getChildren().addAll(metaRow, actionLabel);
            logEntriesContainer.getChildren().add(entryCard);
        }
    }

    private void resetSnapshot() {
        caseStatusValueLabel.setText("No case loaded");
        priorityValueLabel.setText("Unavailable");
        severityValueLabel.setText("Unavailable");
        assignedOfficerValueLabel.setText("Unavailable");
        createdAtValueLabel.setText("Unavailable");
        entryCountValueLabel.setText("0");
        inspectionModeValueLabel.setText("Not active");
        logEntriesContainer.getChildren().setAll(buildPlaceholderLabel());
    }

    private Label buildPlaceholderLabel() {
        Label placeholder = new Label("Load a case to inspect its full chain-of-custody history.");
        placeholder.getStyleClass().add("timeline-empty");
        placeholder.setWrapText(true);
        return placeholder;
    }

    private void updateAvailability() {
        boolean supervisorSignedIn = currentUser != null && currentUser.getRole() == UserRole.SUPERVISOR;
        if (loadLogButton != null) {
            loadLogButton.setDisable(!supervisorSignedIn);
        }
    }

    private String buildActorLabel(AuditLog entry) {
        if (entry.getPerformedByName() == null || entry.getPerformedByName().isBlank()) {
            return "Actor ID " + entry.getPerformedBy();
        }

        return entry.getPerformedByName() + " | ID " + entry.getPerformedBy();
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
