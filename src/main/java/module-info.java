module com.project.cdiem {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.project.controller to javafx.fxml;
    exports com.project;
}
