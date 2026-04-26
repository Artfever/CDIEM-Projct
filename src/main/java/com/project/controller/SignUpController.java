package com.project.controller;

import com.project.model.UserRole;
import com.project.service.SignUpException;
import com.project.service.SignUpService;
import com.project.util.AppNavigator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Self-service account creation screen.
 * Collects user details, creates the account, then returns the user to login.
 */
public class SignUpController {
    private static final String STATUS_NEUTRAL = "status-neutral";
    private static final String STATUS_SUCCESS = "status-success";
    private static final String STATUS_ERROR = "status-error";
    private static final String SUCCESS_MESSAGE = "Account created successfully. Please log in.";

    @FXML
    private TextField fullNameField;

    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private ComboBox<UserRole> roleBox;

    @FXML
    private Label statusLabel;

    private final SignUpService signUpService = new SignUpService();

    @FXML
    public void initialize() {
        roleBox.getItems().setAll(UserRole.values());
        roleBox.setVisibleRowCount(UserRole.values().length);
        setStatus("Fill in all fields to create your account.", STATUS_NEUTRAL);
    }

    @FXML
    public void register() {
        try {
            // Registration stays on this screen until every required field is valid and the account is stored.
            signUpService.register(
                    fullNameField.getText(),
                    usernameField.getText(),
                    emailField.getText(),
                    passwordField.getText(),
                    confirmPasswordField.getText(),
                    roleBox.getValue()
            );
            setStatus(SUCCESS_MESSAGE, STATUS_SUCCESS);
            AppNavigator.showLoginSuccess(SUCCESS_MESSAGE);
        } catch (SignUpException e) {
            setStatus(e.getMessage(), STATUS_ERROR);
            handleFailure(e);
        } catch (Exception e) {
            setStatus(getRootMessage(e), STATUS_ERROR);
        }
    }

    @FXML
    public void backToLogin() {
        try {
            AppNavigator.showLogin();
        } catch (Exception e) {
            setStatus(getRootMessage(e), STATUS_ERROR);
        }
    }

    private void handleFailure(SignUpException exception) {
        switch (exception.getReason()) {
            case REQUIRED_FIELDS -> focusFirstBlankField();
            case PASSWORD_MISMATCH, PASSWORD_TOO_SHORT -> clearPasswordsAndFocus();
            case INVALID_EMAIL -> focus(emailField);
            case USERNAME_IN_USE -> {
                usernameField.clear();
                focus(usernameField);
            }
            case EMAIL_REGISTERED -> {
                emailField.clear();
                focus(emailField);
            }
            default -> {
            }
        }
    }

    private void focusFirstBlankField() {
        if (clean(fullNameField.getText()) == null) {
            focus(fullNameField);
            return;
        }

        if (clean(usernameField.getText()) == null) {
            focus(usernameField);
            return;
        }

        if (clean(emailField.getText()) == null) {
            focus(emailField);
            return;
        }

        if (clean(passwordField.getText()) == null) {
            focus(passwordField);
            return;
        }

        if (clean(confirmPasswordField.getText()) == null) {
            focus(confirmPasswordField);
            return;
        }

        if (roleBox.getValue() == null) {
            focus(roleBox);
        }
    }

    private void clearPasswordsAndFocus() {
        passwordField.clear();
        confirmPasswordField.clear();
        focus(passwordField);
    }

    private void focus(Control control) {
        if (control == null) {
            return;
        }

        Platform.runLater(control::requestFocus);
    }

    private void setStatus(String message, String stateClass) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll(STATUS_NEUTRAL, STATUS_SUCCESS, STATUS_ERROR);
        statusLabel.getStyleClass().add(stateClass);
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
