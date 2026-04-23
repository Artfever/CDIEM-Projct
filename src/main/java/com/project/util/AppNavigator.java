package com.project.util;

import com.project.CaseManagementApplication;
import com.project.controller.CaseController;
import com.project.controller.CaseStateTransitionController;
import com.project.controller.DashboardController;
import com.project.controller.EvidenceController;
import com.project.controller.NotificationController;
import com.project.controller.SubmitReviewController;
import com.project.model.User;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public final class AppNavigator {
    private static Stage primaryStage;

    private AppNavigator() {
    }

    public static void initialize(Stage stage) {
        primaryStage = stage;
    }

    public static void showLogin() {
        Parent root = loadRoot("/view/login.fxml");
        showScene(root, "/view/login.css", "CDIEM | Login", 520, 660, 440, 600);
    }

    public static void showDashboard(User currentUser) {
        FXMLLoader loader = newLoader("/view/dashboard.fxml");
        Parent root = load(loader);
        DashboardController controller = loader.getController();
        controller.setCurrentUser(currentUser);
        showScene(root, "/view/dashboard.css", "CDIEM | Dashboard", 1280, 860, 1080, 760);
    }

    public static void showManageCase(User currentUser) {
        FXMLLoader loader = newLoader("/view/manage_case.fxml");
        Parent root = load(loader);
        CaseController controller = loader.getController();
        controller.setCurrentUser(currentUser);
        showScene(root, "/view/manage_case.css", "CDIEM | Manage Case", 1120, 740, 980, 700);
    }

    public static void showNotifications(User currentUser) {
        FXMLLoader loader = newLoader("/view/notifications.fxml");
        Parent root = load(loader);
        NotificationController controller = loader.getController();
        controller.setCurrentUser(currentUser);
        showScene(root, "/view/notifications.css", "CDIEM | Notifications", 1100, 760, 960, 700);
    }

    public static void showManageEvidence(User currentUser) {
        FXMLLoader loader = newLoader("/view/manage_evidence.fxml");
        Parent root = load(loader);
        EvidenceController controller = loader.getController();
        controller.setCurrentUser(currentUser);
        showScene(root, "/view/manage_evidence.css", "CDIEM | Manage Evidence", 1200, 820, 1020, 740);
    }

    public static void showManageStateTransitions(User currentUser) {
        FXMLLoader loader = newLoader("/view/manage_state_transitions.fxml");
        Parent root = load(loader);
        CaseStateTransitionController controller = loader.getController();
        controller.setCurrentUser(currentUser);
        showScene(root, "/view/manage_state_transitions.css", "CDIEM | Case State Transitions", 1180, 820, 1000, 740);
    }

    public static void showSubmitReview(User currentUser) {
        FXMLLoader loader = newLoader("/view/submit_review.fxml");
        Parent root = load(loader);
        SubmitReviewController controller = loader.getController();
        controller.setCurrentUser(currentUser);
        showScene(root, "/view/manage_state_transitions.css", "CDIEM | Submit Case for Supervisor Review",
                1180, 820, 1000, 740);
    }

    private static Parent loadRoot(String fxmlPath) {
        return load(newLoader(fxmlPath));
    }

    private static FXMLLoader newLoader(String fxmlPath) {
        return new FXMLLoader(CaseManagementApplication.class.getResource(fxmlPath));
    }

    private static Parent load(FXMLLoader loader) {
        try {
            return loader.load();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load UI resource.", e);
        }
    }

    private static void showScene(Parent root, String stylesheetPath, String title,
                                  double width, double height, double minWidth, double minHeight) {
        if (primaryStage == null) {
            throw new IllegalStateException("AppNavigator has not been initialized.");
        }

        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().setAll(
                CaseManagementApplication.class.getResource(stylesheetPath).toExternalForm()
        );

        primaryStage.setTitle(title);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(minWidth);
        primaryStage.setMinHeight(minHeight);
        primaryStage.show();
    }
}
