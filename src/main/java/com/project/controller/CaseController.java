package com.project.controller;

import com.project.model.Case;
import com.project.model.SeverityLevel;
import com.project.model.User;
import com.project.model.UserRole;
import com.project.service.CaseDeletionResult;
import com.project.service.CaseReassignmentResult;
import com.project.service.CaseService;
import com.project.service.CaseSeverityUpdateResult;
import com.project.util.AppNavigator;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/**
 * Main screen for the Manage Case use case.
 * Officers create cases here, while supervisors adjust, reassign, or delete them.
 */
public class CaseController {
    private static final String STATUS_NEUTRAL = "status-neutral";
    private static final String STATUS_SUCCESS = "status-success";
    private static final String STATUS_ERROR = "status-error";

    @FXML
    private ComboBox<User> currentUserBox;

    @FXML
    private TextField caseIdField;

    @FXML
    private TextField titleField;

    @FXML
    private ComboBox<String> severityBox;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private Label descriptionLabel;

    @FXML
    private TextArea relatedInfoArea;

    @FXML
    private Label relatedInfoLabel;

    @FXML
    private ComboBox<User> assignedOfficerBox;

    @FXML
    private Label assignedOfficerLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Button registerCaseButton;

    @FXML
    private Button updateSeverityButton;

    @FXML
    private Button reassignOfficerButton;

    @FXML
    private Button deleteCaseButton;

    private final CaseService caseService = new CaseService();
    private final CaseRegistrationController caseRegistrationController = new CaseRegistrationController();
    private final SeverityUpdateController severityUpdateController = new SeverityUpdateController();
    private final ReassignmentController reassignmentController = new ReassignmentController();
    private User currentUser;

    @FXML
    public void initialize() {
        for (SeverityLevel severityLevel : SeverityLevel.values()) {
            severityBox.getItems().add(severityLevel.name());
        }
        setStatus("Case operations ready.", STATUS_NEUTRAL);
        loadUsers();
        applyCurrentUserContext();
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
        applyCurrentUserContext();
    }

    @FXML
    public void registerCase() {
        try {
            // This path is the starting point of a case: collect the story, store it, and return the new case ID.
            Case c = new Case(
                    clean(titleField.getText()),
                    clean(descriptionArea.getText()),
                    parseSeverity(),
                    clean(relatedInfoArea.getText())
            );

            int caseId = caseRegistrationController.submitCaseDetails(
                    c,
                    requireSelectedUser(currentUserBox, "Acting User").getUserId()
            );
            caseIdField.setText(String.valueOf(caseId));
            clearRegistrationFields();
            setStatus("Case created successfully. Case ID: " + caseId, STATUS_SUCCESS);
        } catch (Exception e) {
            setStatus(getRootMessage(e), STATUS_ERROR);
        }
    }

    @FXML
    public void updateSeverity() {
        try {
            // Supervisors can change severity later, which also changes the case priority and SLA target.
            CaseSeverityUpdateResult result = severityUpdateController.updateSeverity(
                    parseRequiredInteger(caseIdField.getText(), "Case ID"),
                    parseSeverity(),
                    requireSelectedUser(currentUserBox, "Acting User").getUserId()
            );
            setStatus(buildSeverityStatus(result), STATUS_SUCCESS);
        } catch (Exception e) {
            setStatus(getRootMessage(e), STATUS_ERROR);
        }
    }

    @FXML
    public void reassignOfficer() {
        try {
            // Reassignment changes case ownership and lets both the old and new officers know what happened.
            CaseReassignmentResult result = reassignmentController.reassignTo(
                    parseRequiredInteger(caseIdField.getText(), "Case ID"),
                    requireSelectedUser(assignedOfficerBox, "Assigned Investigating Officer").getUserId(),
                    requireSelectedUser(currentUserBox, "Acting User").getUserId()
            );
            setStatus(buildReassignmentStatus(result), STATUS_SUCCESS);
        } catch (Exception e) {
            setStatus(getRootMessage(e), STATUS_ERROR);
        }
    }

    @FXML
    public void deleteCase() {
        try {
            // Delete is a supervisor-only end-of-line action that removes the case from workflow entirely.
            CaseDeletionResult result = caseService.deleteCase(
                    parseRequiredInteger(caseIdField.getText(), "Case ID"),
                    requireSelectedUser(currentUserBox, "Acting User").getUserId()
            );
            caseIdField.clear();
            assignedOfficerBox.getSelectionModel().clearSelection();
            setStatus(buildDeletionStatus(result), STATUS_SUCCESS);
        } catch (Exception e) {
            setStatus(getRootMessage(e), STATUS_ERROR);
        }
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

    private void setStatus(String message, String stateClass) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll(STATUS_NEUTRAL, STATUS_SUCCESS, STATUS_ERROR);
        statusLabel.getStyleClass().add(stateClass);
    }

    private SeverityLevel parseSeverity() {
        String severity = clean(severityBox.getValue());
        if (severity == null) {
            throw new IllegalArgumentException("Severity is required.");
        }

        return SeverityLevel.valueOf(severity);
    }

    private void loadUsers() {
        try {
            currentUserBox.getItems().setAll(caseService.getCaseActors());
            assignedOfficerBox.getItems().setAll(caseService.getInvestigatingOfficers());
            applyCurrentUserSelection();
        } catch (Exception e) {
            setStatus(getRootMessage(e), STATUS_ERROR);
        }
    }

    private void applyCurrentUserContext() {
        if (currentUser == null || currentUserBox == null) {
            return;
        }

        applyCurrentUserSelection();
        currentUserBox.setDisable(true);

        boolean officerSignedIn = currentUser.getRole() == UserRole.OFFICER;
        boolean supervisorSignedIn = currentUser.getRole() == UserRole.SUPERVISOR;
        boolean showNarrativeFields = !supervisorSignedIn;

        // One screen serves two audiences, so the visible actions change with the signed-in role.
        setNodeVisibility(registerCaseButton, officerSignedIn);
        setNodeVisibility(updateSeverityButton, supervisorSignedIn);
        setNodeVisibility(reassignOfficerButton, supervisorSignedIn);
        setNodeVisibility(deleteCaseButton, supervisorSignedIn);
        setNodeVisibility(descriptionLabel, showNarrativeFields);
        setNodeVisibility(descriptionArea, showNarrativeFields);
        setNodeVisibility(relatedInfoLabel, showNarrativeFields);
        setNodeVisibility(relatedInfoArea, showNarrativeFields);
        setNodeVisibility(assignedOfficerLabel, supervisorSignedIn);
        setNodeVisibility(assignedOfficerBox, supervisorSignedIn);

        if (!showNarrativeFields) {
            descriptionArea.clear();
            relatedInfoArea.clear();
        }

        if (!supervisorSignedIn) {
            assignedOfficerBox.getSelectionModel().clearSelection();
        }

        if (officerSignedIn) {
            setStatus("Signed in as Investigating Officer. Registration actions are available.", STATUS_NEUTRAL);
        } else if (supervisorSignedIn) {
            setStatus("Signed in as Supervisory Authority. Severity updates, reassignment, and deletion are available.",
                    STATUS_NEUTRAL);
        }
    }

    private void applyCurrentUserSelection() {
        if (currentUser == null || currentUserBox == null || currentUserBox.getItems().isEmpty()) {
            return;
        }

        for (User user : currentUserBox.getItems()) {
            if (user.getUserId() == currentUser.getUserId()) {
                currentUserBox.getSelectionModel().select(user);
                break;
            }
        }
    }

    private void setNodeVisibility(Node node, boolean visible) {
        if (node == null) {
            return;
        }

        node.setVisible(visible);
        node.setManaged(visible);
    }

    private String buildSeverityStatus(CaseSeverityUpdateResult result) {
        return "Case severity updated to " + result.severity().name() + ". SLA threshold recalculated to "
                + result.slaHours() + " hours and priority state set to "
                + result.priorityState().getDisplayName() + ".";
    }

    private String buildReassignmentStatus(CaseReassignmentResult result) {
        if (!result.incomingOfficerNotified()) {
            return "Investigating Officer updated to " + result.newOfficerName()
                    + ", but the notification dispatch did not complete.";
        }

        if (!result.outgoingOfficerNotified()) {
            return "Investigating Officer assigned to " + result.newOfficerName()
                    + ". Incoming officer notified and case state moved to CASE_REASSIGNED.";
        }

        return "Investigating Officer reassigned from " + result.previousOfficerName() + " to "
                + result.newOfficerName()
                + ". Outgoing and incoming officers notified, and case state moved to CASE_REASSIGNED.";
    }

    private String buildDeletionStatus(CaseDeletionResult result) {
        StringBuilder status = new StringBuilder("Case ")
                .append(result.caseId())
                .append(" was deleted from ")
                .append(formatEnumName(result.previousState().name()))
                .append('.');

        if (result.relatedUserName() == null) {
            status.append(" No related user was available for notification.");
        } else if (result.relatedUserNotified()) {
            status.append(' ').append(result.relatedUserName()).append(" was notified.");
        } else {
            status.append(" Notification to ").append(result.relatedUserName()).append(" could not be confirmed.");
        }

        if (result.retainedEvidenceFileCount() > 0) {
            status.append(' ')
                    .append(result.deletedEvidenceFileCount())
                    .append(" stored evidence file(s) were removed, but ")
                    .append(result.retainedEvidenceFileCount())
                    .append(" file(s) require manual secure-storage cleanup.");
        } else if (result.deletedEvidenceFileCount() > 0) {
            status.append(' ')
                    .append(result.deletedEvidenceFileCount())
                    .append(" stored evidence file(s) were removed from secure storage.");
        }

        return status.toString();
    }

    private User requireSelectedUser(ComboBox<User> comboBox, String fieldName) {
        User selectedUser = comboBox.getValue();
        if (selectedUser == null) {
            throw new IllegalArgumentException(fieldName + " selection is required.");
        }

        return selectedUser;
    }

    private void clearRegistrationFields() {
        titleField.clear();
        descriptionArea.clear();
        relatedInfoArea.clear();
        severityBox.getSelectionModel().clearSelection();
    }

    private int parseRequiredInteger(String value, String fieldName) {
        Integer parsedValue = parseOptionalInteger(value, fieldName);
        if (parsedValue == null) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }

        return parsedValue;
    }

    private Integer parseOptionalInteger(String value, String fieldName) {
        String cleaned = clean(value);
        if (cleaned == null) {
            return null;
        }

        try {
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a number.");
        }
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
