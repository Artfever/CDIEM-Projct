package com.project.controller;

import com.project.model.User;
import com.project.model.UserRole;
import com.project.util.AppNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class DashboardController {
    private static final String STATUS_NEUTRAL = "status-neutral";
    private static final String STATUS_ERROR = "status-error";

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label roleLabel;

    @FXML
    private Label identityLabel;

    @FXML
    private Label accessibleModulesLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private VBox manageCaseCard;

    @FXML
    private VBox manageEvidenceCard;

    @FXML
    private VBox notificationsCard;

    @FXML
    private VBox verifyIntegrityCard;

    @FXML
    private VBox stateTransitionsCard;

    @FXML
    private VBox submitReviewCard;

    @FXML
    private VBox closureCard;

    @FXML
    private VBox chainLogCard;

    @FXML
    private VBox summaryReportCard;

    @FXML
    private VBox escalatedReviewCard;

    private User currentUser;

    @FXML
    public void initialize() {
        setStatus("Select a module from the options available to your role.", STATUS_NEUTRAL);
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
        applyCurrentUserContext();
    }

    @FXML
    public void openManageCase() {
        try {
            AppNavigator.showManageCase(requireCurrentUser());
        } catch (Exception e) {
            setStatus(getRootMessage(e), STATUS_ERROR);
        }
    }

    @FXML
    public void openManageEvidence() {
        try {
            AppNavigator.showManageEvidence(requireCurrentUser());
        } catch (Exception e) {
            setStatus(getRootMessage(e), STATUS_ERROR);
        }
    }

    @FXML
    public void openManageStateTransitions() {
        try {
            AppNavigator.showManageStateTransitions(requireCurrentUser());
        } catch (Exception e) {
            setStatus(getRootMessage(e), STATUS_ERROR);
        }
    }

    @FXML
    public void openSubmitReview() {
        try {
            AppNavigator.showSubmitReview(requireCurrentUser());
        } catch (Exception e) {
            setStatus(getRootMessage(e), STATUS_ERROR);
        }
    }

    @FXML
    public void openManageCaseClosure() {
        try {
            AppNavigator.showManageCaseClosure(requireCurrentUser());
        } catch (Exception e) {
            setStatus(getRootMessage(e), STATUS_ERROR);
        }
    }

    @FXML
    public void openChainOfCustodyLog() {
        try {
            AppNavigator.showChainOfCustodyLog(requireCurrentUser());
        } catch (Exception e) {
            setStatus(getRootMessage(e), STATUS_ERROR);
        }
    }

    @FXML
    public void openEscalatedReview() {
        try {
            AppNavigator.showEscalatedReview(requireCurrentUser());
        } catch (Exception e) {
            setStatus(getRootMessage(e), STATUS_ERROR);
        }
    }

    @FXML
    public void openNotifications() {
        try {
            AppNavigator.showNotifications(requireCurrentUser());
        } catch (Exception e) {
            setStatus(getRootMessage(e), STATUS_ERROR);
        }
    }

    @FXML
    public void showComingSoon() {
        setStatus("This module shell is visible for your role, but the workflow is not implemented yet.", STATUS_NEUTRAL);
    }

    @FXML
    public void logout() {
        AppNavigator.showLogin();
    }

    private void applyCurrentUserContext() {
        if (currentUser == null || welcomeLabel == null) {
            return;
        }

        welcomeLabel.setText("Welcome, " + currentUser.getName());
        roleLabel.setText(currentUser.getRole().getDisplayName());
        identityLabel.setText(buildIdentityLine(currentUser));
        accessibleModulesLabel.setText(buildAccessibleModulesText(currentUser.getRole()));

        configureCard(manageCaseCard, hasAccess(currentUser.getRole(), "manageCase"));
        configureCard(manageEvidenceCard, hasAccess(currentUser.getRole(), "manageEvidence"));
        configureCard(notificationsCard, hasAccess(currentUser.getRole(), "notifications"));
        configureCard(verifyIntegrityCard, hasAccess(currentUser.getRole(), "verifyIntegrity"));
        configureCard(stateTransitionsCard, hasAccess(currentUser.getRole(), "stateTransitions"));
        configureCard(submitReviewCard, hasAccess(currentUser.getRole(), "submitReview"));
        configureCard(closureCard, hasAccess(currentUser.getRole(), "closure"));
        configureCard(chainLogCard, hasAccess(currentUser.getRole(), "chainLog"));
        configureCard(summaryReportCard, hasAccess(currentUser.getRole(), "summaryReport"));
        configureCard(escalatedReviewCard, hasAccess(currentUser.getRole(), "escalatedReview"));
    }

    private String buildIdentityLine(User user) {
        String username = user.getUsername() == null ? "n/a" : "@" + user.getUsername();
        String email = user.getEmail() == null ? "n/a" : user.getEmail();
        return username + " | " + email + " | User ID " + user.getUserId();
    }

    private String buildAccessibleModulesText(UserRole role) {
        return switch (role) {
            case OFFICER -> "Accessible modules: Manage Case, Notifications, Manage Evidence, Submit Case for Supervisor Review.";
            case ANALYST -> "Accessible modules: Notifications, Manage Evidence, Verify Evidence Integrity, Manage Case State Transitions.";
            case SUPERVISOR -> "Accessible modules: Manage Case, Notifications, Manage Case State Transitions, Manage Case Closure, View Chain-of-Custody Log, Generate Summary Report, Review Escalated Case.";
        };
    }

    private boolean hasAccess(UserRole role, String moduleKey) {
        return switch (moduleKey) {
            case "manageCase" -> role == UserRole.OFFICER || role == UserRole.SUPERVISOR;
            case "notifications" -> true;
            case "manageEvidence" -> role == UserRole.OFFICER || role == UserRole.ANALYST;
            case "verifyIntegrity" -> role == UserRole.ANALYST;
            case "stateTransitions" -> role == UserRole.ANALYST || role == UserRole.SUPERVISOR;
            case "submitReview" -> role == UserRole.OFFICER;
            case "closure", "chainLog", "summaryReport", "escalatedReview" -> role == UserRole.SUPERVISOR;
            default -> false;
        };
    }

    private void configureCard(VBox card, boolean accessible) {
        if (card == null) {
            return;
        }

        card.setVisible(accessible);
        card.setManaged(accessible);
    }

    private User requireCurrentUser() {
        if (currentUser == null) {
            throw new IllegalStateException("No authenticated user is available.");
        }

        return currentUser;
    }

    private void setStatus(String message, String stateClass) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll(STATUS_NEUTRAL, STATUS_ERROR);
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
