package com.project.controller;

import com.project.model.Case;
import com.project.model.SeverityLevel;
import com.project.model.User;
import com.project.service.CaseService;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

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
    private TextArea relatedInfoArea;

    @FXML
    private ComboBox<User> assignedOfficerBox;

    @FXML
    private Label statusLabel;

    private final CaseService caseService = new CaseService();

    @FXML
    public void initialize() {
        for (SeverityLevel severityLevel : SeverityLevel.values()) {
            severityBox.getItems().add(severityLevel.name());
        }
        setStatus("Case operations ready.", STATUS_NEUTRAL);
        loadUsers();
    }

    @FXML
    public void registerCase() {
        try {
            Case c = new Case(
                    clean(titleField.getText()),
                    clean(descriptionArea.getText()),
                    parseSeverity(),
                    clean(relatedInfoArea.getText())
            );

            int caseId = caseService.registerCase(c, requireSelectedUser(currentUserBox, "Acting User").getUserId());
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
            caseService.updateSeverityLevel(
                    parseRequiredInteger(caseIdField.getText(), "Case ID"),
                    parseSeverity(),
                    requireSelectedUser(currentUserBox, "Acting User").getUserId()
            );
            setStatus("Case severity updated successfully.", STATUS_SUCCESS);
        } catch (Exception e) {
            setStatus(getRootMessage(e), STATUS_ERROR);
        }
    }

    @FXML
    public void reassignOfficer() {
        try {
            caseService.reassignOfficer(
                    parseRequiredInteger(caseIdField.getText(), "Case ID"),
                    requireSelectedUser(assignedOfficerBox, "Assigned Investigating Officer").getUserId(),
                    requireSelectedUser(currentUserBox, "Acting User").getUserId()
            );
            setStatus("Investigating Officer reassigned successfully.", STATUS_SUCCESS);
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
        } catch (Exception e) {
            setStatus(getRootMessage(e), STATUS_ERROR);
        }
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
