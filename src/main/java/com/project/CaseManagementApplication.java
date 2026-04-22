package com.project;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class CaseManagementApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(CaseManagementApplication.class.getResource("/view/manage_case.fxml"));
        Scene scene = new Scene(loader.load(), 1120, 740);
        scene.getStylesheets().add(
                CaseManagementApplication.class.getResource("/view/manage_case.css").toExternalForm()
        );

        stage.setTitle("CDIEM | Manage Case");
        stage.setScene(scene);
        stage.setMinWidth(980);
        stage.setMinHeight(700);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
