package com.project;

import com.project.util.AppNavigator;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * JavaFX entry point that initializes navigation and opens the login screen.
 */
public class CaseManagementApplication extends Application {
    @Override
    public void start(Stage stage) {
        AppNavigator.initialize(stage);
        AppNavigator.showLogin();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
