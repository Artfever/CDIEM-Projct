package com.project.controller;

import com.project.model.User;
import com.project.service.AuthService;
import com.project.util.AppNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {
    private static final String STATUS_NEUTRAL = "status-neutral";
    private static final String STATUS_SUCCESS = "status-success";
    private static final String STATUS_ERROR = "status-error";
    private static final String DEFAULT_STATUS_MESSAGE = "Enter your username/email and password.";

    @FXML
    private TextField identifierField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label statusLabel;

    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        showNeutralStatus();
    }

    @FXML
    public void login() {
        try {
            User authenticatedUser = authService.authenticate(identifierField.getText(), passwordField.getText());
            setStatus("Authentication successful.", STATUS_SUCCESS);
            AppNavigator.showDashboard(authenticatedUser);
        } catch (Exception e) {
            setStatus(getRootMessage(e), STATUS_ERROR);
        }
    }

    @FXML
    public void showSignUp() {
        try {
            AppNavigator.showSignUp();
        } catch (Exception e) {
            showErrorStatus(getRootMessage(e));
        }
    }

    public void showNeutralStatus() {
        setStatus(DEFAULT_STATUS_MESSAGE, STATUS_NEUTRAL);
    }

    public void showSuccessStatus(String message) {
        setStatus(message, STATUS_SUCCESS);
    }

    public void showErrorStatus(String message) {
        setStatus(message, STATUS_ERROR);
    }

    private void setStatus(String message, String stateClass) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll(STATUS_NEUTRAL, STATUS_SUCCESS, STATUS_ERROR);
        statusLabel.getStyleClass().add(stateClass);
    }

    private String getRootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }

        return current.getMessage() == null ? "Unknown error." : current.getMessage();
    }
}
