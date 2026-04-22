package com.project.controller;

import com.project.model.Case;
import com.project.service.CaseService;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

public class CaseController {
    @FXML
    private TextField titleField;

    @FXML
    private ComboBox<String> severityBox;

    private final CaseService caseService = new CaseService();

    @FXML
    public void initialize() {
        severityBox.getItems().setAll("Low", "Medium", "High", "Critical");
    }

    @FXML
    public void createCase() {
        String title = titleField.getText();
        String severity = severityBox.getValue();

        Case c = new Case(title, severity);
        caseService.createCase(c);

        System.out.println("Case Created");
    }
}
