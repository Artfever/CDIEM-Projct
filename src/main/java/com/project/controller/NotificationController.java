package com.project.controller;

import com.project.model.NotificationRecord;
import com.project.model.User;
import com.project.service.NotificationService;
import com.project.util.AppNavigator;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Read-only inbox screen for system notifications.
 * It shows what the current user has been told about cases and workflow events.
 */
public class NotificationController {
    private static final String STATUS_NEUTRAL = "status-neutral";
    private static final String STATUS_ERROR = "status-error";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy | hh:mm a");

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label subtitleLabel;

    @FXML
    private Label notificationCountLabel;

    @FXML
    private VBox notificationsContainer;

    @FXML
    private Label emptyStateLabel;

    @FXML
    private Label statusLabel;

    private final NotificationService notificationService = new NotificationService();
    private User currentUser;

    @FXML
    public void initialize() {
        setStatus("Notifications are loaded from the system inbox.", STATUS_NEUTRAL);
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
        loadNotifications();
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
    public void refreshNotifications() {
        loadNotifications();
    }

    private void loadNotifications() {
        if (currentUser == null || notificationsContainer == null) {
            return;
        }

        try {
            // Notifications are always loaded fresh so the inbox reflects the latest workflow events.
            List<NotificationRecord> notifications = notificationService.getNotificationsForUser(currentUser.getUserId());
            welcomeLabel.setText("Notifications for " + currentUser.getName());
            subtitleLabel.setText("System-delivered alerts tied to your user account.");
            notificationCountLabel.setText(formatCount(notifications.size()));
            renderNotifications(notifications);
            setStatus("Inbox refreshed.", STATUS_NEUTRAL);
        } catch (Exception e) {
            notificationsContainer.getChildren().clear();
            notificationCountLabel.setText("Unavailable");
            emptyStateLabel.setVisible(true);
            emptyStateLabel.setManaged(true);
            emptyStateLabel.setText("Notifications could not be loaded.");
            setStatus(getRootMessage(e), STATUS_ERROR);
        }
    }

    private void renderNotifications(List<NotificationRecord> notifications) {
        notificationsContainer.getChildren().clear();

        boolean empty = notifications.isEmpty();
        emptyStateLabel.setVisible(empty);
        emptyStateLabel.setManaged(empty);

        if (empty) {
            emptyStateLabel.setText("No notifications have been delivered to this user yet.");
            return;
        }

        for (NotificationRecord notification : notifications) {
            notificationsContainer.getChildren().add(buildNotificationCard(notification));
        }
    }

    private VBox buildNotificationCard(NotificationRecord notification) {
        VBox card = new VBox(10.0);
        card.getStyleClass().add("notification-card");
        card.setPadding(new Insets(18.0));

        HBox headerRow = new HBox(10.0);
        Label senderLabel = new Label("From: " + notification.getSenderName());
        senderLabel.getStyleClass().add("notification-sender");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label channelChip = new Label(notification.getChannel());
        channelChip.getStyleClass().add("notification-channel");
        headerRow.getChildren().addAll(senderLabel, spacer, channelChip);

        Label messageLabel = new Label(notification.getMessage());
        messageLabel.getStyleClass().add("notification-message");
        messageLabel.setWrapText(true);

        String caseReference = notification.getCaseId() == null ? "General alert" : "Case ID " + notification.getCaseId();
        String timestamp = notification.getCreatedAt() == null
                ? "Time unavailable"
                : notification.getCreatedAt().format(TIMESTAMP_FORMATTER);
        Label metaLabel = new Label(caseReference + " | " + timestamp);
        metaLabel.getStyleClass().add("notification-meta");

        card.getChildren().addAll(headerRow, messageLabel, metaLabel);
        return card;
    }

    private String formatCount(int notificationCount) {
        return notificationCount == 1 ? "1 notification" : notificationCount + " notifications";
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
